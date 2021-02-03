/**
 * Copyright (C) 2020 Eclipse Foundation
 *
 * <p>This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 */
package org.eclipsefoundation.git.eca.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a bot user in the Eclipse API.
 *
 * @author Martin Lowe
 */
public class BotUser {
  private String id;
  private String projectId;
  private String username;
  private String email;

  @JsonProperty("github.com")
  private SiteSpecificBot githubBot;

  @JsonProperty("gitlab.eclipse.org")
  private SiteSpecificBot gitlabBot;

  /** @return the id */
  public String getId() {
    return id;
  }

  /** @param id the id to set */
  public void setId(String id) {
    this.id = id;
  }

  /** @return the projectId */
  public String getProjectId() {
    return projectId;
  }

  /** @param projectId the projectId to set */
  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  /** @return the username */
  public String getUsername() {
    return username;
  }

  /** @param username the username to set */
  public void setUsername(String username) {
    this.username = username;
  }

  /** @return the email */
  public String getEmail() {
    return email;
  }

  /** @return the githubBot */
  public SiteSpecificBot getGithubBot() {
    return githubBot;
  }

  /** @param githubBot the githubBot to set */
  public void setGithubBot(SiteSpecificBot githubBot) {
    this.githubBot = githubBot;
  }

  /** @return the gitlabBot */
  public SiteSpecificBot getGitlabBot() {
    return gitlabBot;
  }

  /** @param gitlabBot the gitlabBot to set */
  public void setGitlabBot(SiteSpecificBot gitlabBot) {
    this.gitlabBot = gitlabBot;
  }

  /** @param email the email to set */
  public void setEmail(String email) {
    this.email = email;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("BotUser [id=");
    builder.append(id);
    builder.append(", projectId=");
    builder.append(projectId);
    builder.append(", username=");
    builder.append(username);
    builder.append(", email=");
    builder.append(email);
    builder.append(", githubBot=");
    builder.append(githubBot);
    builder.append(", gitlabBot=");
    builder.append(gitlabBot);
    builder.append("]");
    return builder.toString();
  }

  public static class SiteSpecificBot {
    private String username;
    private String email;

    /** @return the username */
    public String getUsername() {
      return username;
    }

    /** @param username the username to set */
    public void setUsername(String username) {
      this.username = username;
    }

    /** @return the email */
    public String getEmail() {
      return email;
    }

    /** @param email the email to set */
    public void setEmail(String email) {
      this.email = email;
    }
  }
}
