/**
 * Copyright (C) 2020 Eclipse Foundation
 *
 * <p>This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 */
package org.eclipsefoundation.git.eca.resource;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.eclipsefoundation.git.eca.api.AccountsAPI;
import org.eclipsefoundation.git.eca.api.BotsAPI;
import org.eclipsefoundation.git.eca.helper.CommitHelper;
import org.eclipsefoundation.git.eca.model.Commit;
import org.eclipsefoundation.git.eca.model.EclipseUser;
import org.eclipsefoundation.git.eca.model.GitUser;
import org.eclipsefoundation.git.eca.model.Project;
import org.eclipsefoundation.git.eca.model.ValidationRequest;
import org.eclipsefoundation.git.eca.model.ValidationResponse;
import org.eclipsefoundation.git.eca.namespace.APIStatusCode;
import org.eclipsefoundation.git.eca.namespace.ProviderType;
import org.eclipsefoundation.git.eca.service.CachingService;
import org.eclipsefoundation.git.eca.service.OAuthService;
import org.eclipsefoundation.git.eca.service.ProjectsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * ECA validation endpoint for Git commits. Will use information from the bots, projects, and
 * accounts API to validate commits passed to this endpoint. Should be as system agnostic as
 * possible to allow for any service to request validation with less reliance on services external
 * to the Eclipse foundation.
 *
 * @author Martin Lowe
 */
@Path("/eca")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class ValidationResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(ValidationResource.class);

  @Inject
  @ConfigProperty(name = "eclipse.mail.allowlist")
  List<String> allowListUsers;
  @Inject
  @ConfigProperty(name = "eclipse.noreply.email-patterns")
  List<String> emailPatterns;

  // eclipse API rest client interfaces
  @Inject @RestClient AccountsAPI accounts;
  @Inject @RestClient BotsAPI bots;

  // external API/service harnesses
  @Inject OAuthService oauth;
  @Inject CachingService cache;
  @Inject ProjectsService projects;

  // rendered list of regex values
  List<Pattern> patterns;

  @PostConstruct
  void init() {
    // compile the patterns once per object to save processing time
    this.patterns = emailPatterns.stream().map(Pattern::compile).collect(Collectors.toList());
  }

  /**
   * Consuming a JSON request, this method will validate all passed commits, using the repo URL and
   * the repository provider. These commits will be validated to ensure that all users are covered
   * either by an ECA, or are committers on the project. In the case of ECA-only contributors, an
   * additional sign off footer is required in the body of the commit.
   *
   * @param req the request containing basic data plus the commits to be validated
   * @return a web response indicating success or failure for each commit, along with standard
   *     messages that may be used to give users context on failure.
   * @throws MalformedURLException
   */
  @POST
  public Response validate(ValidationRequest req) {
    ValidationResponse r = new ValidationResponse();
    r.setStrictMode(req.isStrictMode());
    // check that we have commits to validate
    if (req.getCommits() == null || req.getCommits().isEmpty()) {
      addError(r, "A commit is required to validate", null);
    }
    // check that we have a repo set
    if (req.getRepoUrl() == null) {
      addError(r, "A base repo URL needs to be set in order to validate", null);
    }
    // check that we have a type set
    if (req.getProvider() == null) {
      addError(r, "A provider needs to be set to validate a request", null);
    }
    // only process if we have no errors
    if (r.getErrorCount() == 0) {
      LOGGER.debug("Processing: {}", req);
      // filter the projects based on the repo URL. At least one repo in project must
      // match the repo URL to be valid
      List<Project> filteredProjects = retrieveProjectsForRequest(req);
      // set whether this call has tracked projects
      r.setTrackedProject(!filteredProjects.isEmpty());
      for (Commit c : req.getCommits()) {
        // process the request, capturing if we should continue processing
        boolean continueProcessing = processCommit(c, r, filteredProjects, req.getProvider());
        // if there is a reason to stop processing, break the loop
        if (!continueProcessing) {
          break;
        }
      }
    }
    // depending on number of errors found, set response status
    if (r.getErrorCount() == 0) {
      r.setPassed(true);
    }
    return r.toResponse();
  }

  /**
   * Process the current request, validating that the passed commit is valid. The author and
   * committers Eclipse Account is retrieved, which are then used to check if the current commit is
   * valid for the current project.
   *
   * @param c the commit to process
   * @param response the response container
   * @param filteredProjects tracked projects for the current request
   * @return true if we should continue processing, false otherwise.
   */
  private boolean processCommit(
      Commit c,
      ValidationResponse response,
      List<Project> filteredProjects,
      ProviderType provider) {
    // ensure the commit is valid, and has required fields
    if (!CommitHelper.validateCommit(c)) {
      addError(
          response,
          "One or more commits were invalid. Please check the payload and try again",
          c.getHash());
      return false;
    }
    // retrieve the author + committer for the current request
    GitUser author = c.getAuthor();
    GitUser committer = c.getCommitter();

    addMessage(response, String.format("Reviewing commit: %1$s", c.getHash()), c.getHash());
    addMessage(
        response,
        String.format("Authored by: %1$s <%2$s>", author.getName(), author.getMail()),
        c.getHash());

    // skip processing if a merge commit
    if (c.getParents().size() > 1) {
      addMessage(
          response,
          String.format(
              "Commit '%1$s' has multiple parents, merge commit detected, passing", c.getHash()),
          c.getHash());
      return true;
    }

    // retrieve the eclipse account for the author
    EclipseUser eclipseAuthor = getIdentifiedUser(author);
    // if the user is a bot, generate a stubbed user
    if (isAllowedUser(author.getMail()) || userIsABot(author.getMail(), filteredProjects)) {
      addMessage(
          response,
          String.format(
              "Automated user '%1$s' detected for author of commit %2$s",
              author.getMail(), c.getHash()),
          c.getHash());
      eclipseAuthor = EclipseUser.createBotStub(author);
    } else if (eclipseAuthor == null) {
      addMessage(
          response,
          String.format(
              "Could not find an Eclipse user with mail '%1$s' for author of commit %2$s",
              author.getMail(), c.getHash()),
          c.getHash());
      addError(response, "Author must have an Eclipse Account", c.getHash(), APIStatusCode.ERROR_AUTHOR);
      return true;
    }

    // retrieve the eclipse account for the committer
    EclipseUser eclipseCommitter = getIdentifiedUser(committer);
    // check if whitelisted or bot
    if (isAllowedUser(committer.getMail()) || userIsABot(committer.getMail(), filteredProjects)) {
      addMessage(
          response,
          String.format(
              "Automated user '%1$s' detected for committer of commit %2$s",
              committer.getMail(), c.getHash()),
          c.getHash());
      eclipseCommitter = EclipseUser.createBotStub(committer);
    } else if (eclipseCommitter == null) {
      addMessage(
          response,
          String.format(
              "Could not find an Eclipse user with mail '%1$s' for committer of commit %2$s",
              committer.getMail(), c.getHash()),
          c.getHash());
      addError(response, "Committing user must have an Eclipse Account", c.getHash(), APIStatusCode.ERROR_COMMITTER);
      return true;
    }
    // validate author access to the current repo
    validateUserAccess(response, c, eclipseAuthor, filteredProjects, APIStatusCode.ERROR_AUTHOR);

    // check committer general access
    boolean isCommittingUserCommitter = isCommitter(response, eclipseCommitter, c.getHash(), filteredProjects);
    validateUserAccessPartial(response, c, eclipseCommitter, isCommittingUserCommitter, APIStatusCode.ERROR_COMMITTER);
    return true;
  }



  /**
   * Validates author access for the current commit. If there are errors, they are recorded in the
   * response for the current request to be returned once all validation checks are completed.
   *
   * @param r the current response object for the request
   * @param c the commit that is being validated
   * @param eclipseUser the user to validate on a branch
   * @param filteredProjects tracked projects for the current request
   * @param errorCode the error code to display if the user does not have access
   */
  private void validateUserAccess(
      ValidationResponse r,
      Commit c,
      EclipseUser eclipseUser,
      List<Project> filteredProjects, APIStatusCode errorCode) {
    // call isCommitter inline and pass to partial call 
    validateUserAccessPartial(r, c, eclipseUser, isCommitter(r, eclipseUser, c.getHash(), filteredProjects), errorCode);
  }

  /**
   * Allows for isCommitter to be called external to this method. This was extracted to ensure that isCommitter isn't 
   * called twice for the same user when checking committer proxy push rules and committer general access.
   * 
   * @param r the current response object for the request
   * @param c the commit that is being validated
   * @param eclipseUser the user to validate on a branch
   * @param isCommitter the results of the isCommitter call from this class.
   * @param errorCode the error code to display if the user does not have access
   */
  private void validateUserAccessPartial(ValidationResponse r, Commit c, EclipseUser eclipseUser, 
        boolean isCommitter, APIStatusCode errorCode) {
    String userType = "author";
    if (APIStatusCode.ERROR_COMMITTER.equals(errorCode)) {
      userType = "committer";
    }
    if (isCommitter) {
      addMessage(r, String.format("Eclipse user '%s'(%s) is a committer on the project.", eclipseUser.getName(), userType), c.getHash());
    } else {
      addMessage(r, String.format("Eclipse user '%s'(%s) is not a committer on the project.", eclipseUser.getName(), userType), c.getHash());
      // check if the author is signed off if not a committer
      if (eclipseUser.getEca().isSigned()) {
        addMessage(
            r,
            String.format("Eclipse user '%s'(%s) has a current Eclipse Contributor Agreement (ECA) on file.", eclipseUser.getName(), userType),
            c.getHash());
      } else {
        addMessage(
            r,
            String.format("Eclipse user '%s'(%s) does not have a current Eclipse Contributor Agreement (ECA) on file.\n"
                + "If there are multiple commits, please ensure that each author has a ECA.", eclipseUser.getName(), userType),
            c.getHash());
        addError(r, String.format("An Eclipse Contributor Agreement is required for Eclipse user '%s'(%s).", eclipseUser.getName(), userType),
            c.getHash(), errorCode);
      }
    }
  }

  /**
   * Checks whether the given user is a committer on the project. If they are and the project is
   * also a specification for a working group, an additional access check is made against the user.
   *
   * <p>Additionally, a check is made to see if the user is a registered bot user for the given
   * project. If they match for the given project, they are granted committer-like access to the
   * repository.
   *
   * @param r the current response object for the request
   * @param user the user to validate on a branch
   * @param hash the hash of the commit that is being validated
   * @param filteredProjects tracked projects for the current request
   * @return true if user is considered a committer, false otherwise.
   */
  private boolean isCommitter(
      ValidationResponse r,
      EclipseUser user,
      String hash,
      List<Project> filteredProjects) {
    // iterate over filtered projects
    for (Project p : filteredProjects) {
      LOGGER.debug("Checking project '{}' for user '{}'", p.getName(), user.getName());
      // check if any of the committers usernames match the current user
      if (p.getCommitters().stream().anyMatch(u -> u.getUsername().equals(user.getName()))) {
        // check if the current project is a committer project, and if the user can
        // commit to specs
        if (p.getSpecWorkingGroup() != null && !user.getEca().isCanContributeSpecProject()) {
          // set error + update response status
          r.addError(
              hash,
              String.format(
                  "Project is a specification for the working group '%1$s', but user does not have permission to modify a specification project",
                  p.getSpecWorkingGroup()),
              APIStatusCode.ERROR_SPEC_PROJECT);
          return false;
        } else {
          LOGGER.debug(
              "User '{}' was found to be a committer on current project repo '{}'",
              user.getMail(),
              p.getName());
          return true;
        }
      }
    }
    // check if user is a bot, either through early detection or through on-demand check
    if (user.isBot() || userIsABot(user.getMail(), filteredProjects)) {
      LOGGER.debug("User '{} <{}>' was found to be a bot", user.getName(), user.getMail());
      return true;
    }
    return false;
  }

    private boolean userIsABot(String mail, List<Project> filteredProjects) {
        if (mail == null || "".equals(mail.trim())) {
            return false;
        }
        List<JsonNode> botObjs = getBots();
        // if there are no matching projects, then check against all bots, not just project bots
        if (filteredProjects == null || filteredProjects.isEmpty()) {
            return botObjs.stream().anyMatch(bot -> checkFieldsForMatchingMail(bot, mail));
        }
        // for each of the matched projects, check the bot for the matching project ID
        for (Project p : filteredProjects) {
            LOGGER.debug("Checking project {} for matching bots", p.getProjectId());
            for (JsonNode bot : botObjs) {
                // if the project ID match, and one of the email fields matches, then user is bot
                if (p.getProjectId().equalsIgnoreCase(bot.get("projectId").asText())
                        && checkFieldsForMatchingMail(bot, mail)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks JSON node to look for email fields, both at the root, and nested email fields.
     * 
     * @param bot the bots JSON object representation
     * @param mail the email to match against
     * @return true if the bot has a matching email value, otherwise false
     */
    private boolean checkFieldsForMatchingMail(JsonNode bot, String mail) {
        // check the root email for the bot for match
        JsonNode botmail = bot.get("email");
        if (mail != null && botmail != null && mail.equalsIgnoreCase(botmail.asText(""))) {
            LOGGER.debug("Found matching bot at root level for '{}'",mail);
            return true;
        }
        Iterator<Entry<String, JsonNode>> i = bot.fields();
        while (i.hasNext()) {
            Entry<String, JsonNode> e = i.next();
            // check that our field is an object with fields
            JsonNode node = e.getValue();
            if (node.isObject()) {
                LOGGER.debug("Checking {} for bot email", e.getKey());
                // if the mail matches (ignoring case) user is bot
                JsonNode botAliasMail = node.get("email");
                if (mail != null && botAliasMail != null && mail.equalsIgnoreCase(botAliasMail.asText(""))) {
                    LOGGER.debug("Found match for bot email {}", mail);
                    return true;
                }
            }
        }
        return false;
    }
  private boolean isAllowedUser(String mail) {
    return allowListUsers.indexOf(mail) != -1;
  }

  /**
   * Retrieves projects valid for the current request, or an empty list if no data or matching
   * project repos could be found.
   *
   * @param req the current request
   * @return list of matching projects for the current request, or an empty list if none found.
   */
  private List<Project> retrieveProjectsForRequest(ValidationRequest req) {
    String repoUrl = req.getRepoUrl().getPath();
    if (repoUrl == null) {
      LOGGER.warn("Can not match null repo URL to projects");
      return Collections.emptyList();
    }
    // check for all projects that make use of the given repo
    List<Project> availableProjects = projects.getProjects();
    if (availableProjects == null || availableProjects.isEmpty()) {
      LOGGER.warn("Could not find any projects to match against");
      return Collections.emptyList();
    }
    LOGGER.debug("Checking projects for repos that end with: {}", repoUrl);

    // filter the projects based on the repo URL. At least one repo in project must
    // match the repo URL to be valid
    if (ProviderType.GITLAB.equals(req.getProvider())) {
      return availableProjects
          .stream()
          .filter(p -> p.getGitlabRepos().stream().anyMatch(re -> re.getUrl() != null && re.getUrl().endsWith(repoUrl)))
          .collect(Collectors.toList());
    } else if (ProviderType.GITHUB.equals(req.getProvider())) {
      return availableProjects
          .stream()
          .filter(p -> p.getGithubRepos().stream().anyMatch(re -> re.getUrl() != null && re.getUrl().endsWith(repoUrl)))
          .collect(Collectors.toList());
    } else if (ProviderType.GERRIT.equals(req.getProvider())) {
      return availableProjects
          .stream()
          .filter(p -> p.getGerritRepos().stream().anyMatch(re -> re.getUrl() != null && re.getUrl().endsWith(repoUrl)))
          .collect(Collectors.toList());
    } else {
      return availableProjects
          .stream()
          .filter(p -> p.getRepos().stream().anyMatch(re -> re.getUrl().endsWith(repoUrl)))
          .collect(Collectors.toList());
    }
  }

  /**
   * Retrieves an Eclipse Account user object given the Git users email address (at minimum). This
   * is facilitated using the Eclipse Foundation accounts API, along short lived in-memory caching
   * for performance and some protection against duplicate requests.
   *
   * @param user the user to retrieve Eclipse Account information for
   * @return the Eclipse Account user information if found, or null if there was an error or no user
   *     exists.
   */
  private EclipseUser getIdentifiedUser(GitUser user) {
    // get the Eclipse account for the user
    try {
      // use cache to avoid asking for the same user repeatedly on repeated requests
      Optional<EclipseUser> foundUser =
          cache.get("user|" + user.getMail(), () -> retrieveUser(user), EclipseUser.class);
      if (!foundUser.isPresent()) {
        LOGGER.warn("No users found for mail '{}'", user.getMail());
        return null;
      }
      return foundUser.get();
    } catch (WebApplicationException e) {
      Response r = e.getResponse();
      if (r != null && r.getStatus() == 404) {
        LOGGER.error("No users found for mail '{}'", user.getMail());
      } else {
        LOGGER.error("Error while checking for user", e);
      }
    }
    return null;
  }

  /**
   * Checks for standard and noreply email address matches for a Git user and converts to a
   * Eclipse Foundation account object.
   * 
   * @param user the user to attempt account retrieval for.
   * @return the user account if found by mail, or null if none found.
   */
  private EclipseUser retrieveUser(GitUser user) {
    // check for noreply (no reply will never have user account, and fails fast)
    EclipseUser eclipseUser = checkForNoReplyUser(user);
    if (eclipseUser != null) {
      return eclipseUser;
    }
    // standard user check (returns best match)
    LOGGER.debug("Checking user with mail {}", user.getMail());
    try {
      List<EclipseUser> users = accounts.getUsers("Bearer " + oauth.getToken(), null, null, user.getMail());
      if (users != null) {
        return users.get(0);
      }
    } catch(WebApplicationException e) {
      LOGGER.warn("Could not find user account with mail '{}'", user.getMail());
    }
    return null;
  }

  /**
   * Checks git user for no-reply address, and attempts to ratify user through reverse lookup in API service.
   * Currently, this service only recognizes Github no-reply addresses as they have a route to be mapped.
   * 
   * @param user the Git user account to check for no-reply mail address
   * @return the Eclipse user if email address is detected no reply and one can be mapped, otherwise null
   */
  private EclipseUser checkForNoReplyUser(GitUser user) {
    LOGGER.debug("Checking user with mail {} for no-reply", user.getMail());
    boolean isNoReply = patterns.stream().anyMatch(pattern -> pattern.matcher(user.getMail().trim()).find());
    if (isNoReply) {
      // get the username/ID string before the first @ symbol. 
      String noReplyUser = user.getMail().substring(0, user.getMail().indexOf("@", 0));
      // split based on +, if more than one part, use second (contains user), otherwise, use whole string
      String[] nameParts = noReplyUser.split("[\\+]");
      String namePart;
      if (nameParts.length > 1 && nameParts[1] != null) {
        namePart = nameParts[1];
      } else {
        namePart = nameParts[0];
      }
      String uname = namePart.trim();
      LOGGER.debug("User with mail {} detected as noreply account, checking services for username match on '{}'", 
        user.getMail(), uname);

      // check github for no-reply (only allowed noreply currently)
      if (user.getMail().endsWith("noreply.github.com")) {
        try {
          // check for Github no reply, return if set
          EclipseUser eclipseUser = accounts.getUserByGithubUname("Bearer " + oauth.getToken(), uname);
          if (eclipseUser != null) {
            return eclipseUser;
          }
        } catch(WebApplicationException e) {
          LOGGER.warn("No match for '{}' in Github", uname);
        }
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private List<JsonNode> getBots() {
      Optional<List<JsonNode>> allBots = cache.get("allBots", () -> bots.getBots(),
              (Class<List<JsonNode>>) (Object) List.class);
      if (!allBots.isPresent()) {
          return Collections.emptyList();
      }
      return allBots.get();
  }

  private void addMessage(ValidationResponse r, String message, String hash) {
    addMessage(r, message, hash, APIStatusCode.SUCCESS_DEFAULT);
  }

  private void addError(ValidationResponse r, String message, String hash) {
    addError(r, message, hash, APIStatusCode.ERROR_DEFAULT);
  }

  private void addMessage(ValidationResponse r, String message, String hash, APIStatusCode code) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(message);
    }
    r.addMessage(hash, message, code);
  }

  private void addError(ValidationResponse r, String message, String hash, APIStatusCode code) {
    LOGGER.error(message);
    // only add as strict error for tracked projects
    if (r.isTrackedProject() || r.isStrictMode()) {
      r.addError(hash, message, code);
    } else {
      r.addWarning(hash, message, code);
    }
  }
}
