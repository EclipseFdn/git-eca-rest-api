# git-eca-rest-api

---
**NOTE**

This project was migrated to [Eclipse Gitlab](https://gitlab.eclipse.org/eclipsefdn/it/api/git-eca-rest-api) on October 14, 2021.

---

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

<!-- TOC -->
- [What is a valid commit?](#what-is-a-valid-commit)
- [Submitting applications for existing bots](#submitting-applications-for-existing-bots)
- [Running the application in dev mode](#running-the-application-in-dev-mode)
- [Packaging and running the application](#packaging-and-running-the-application)
- [Enabling commit hook in GitLab](#enabling-commit-hook-in-gitlab)
<!-- /TOC -->


## What is a valid commit?
To be considered a valid commit, the following set of rules are checked against all commits that are submitted to this service. If any fail and the commit is related to a project, the service returns a message indicating the commit is not suitable for submission along with messages as hints.

1. All users that commit to a project within the Eclipse space must have a signed [ECA](https://accounts.eclipse.org/user/eca), and therefore, Eclipse accounts. 
    - The one exception to this rule is registered bot users, as they cannot sign an [ECA](https://accounts.eclipse.org/user/eca).
    - Users covered by an MCCA are also covered, as it is considered an equivalent document in the eyes of contribution access.
2. Contributing users must sign off on their commits using the `Signed-off-by` footer (example below).
example) Signed-off-by: Martin Lowe <martin.lowe@eclipse-foundation.org>  
    - Bot users and committers for a project are exempt from this rule, as they are covered by other agreements. If a bot is required for a project, it can be registered through the [bot API](https://github.com/EclipseFdn/projects-bots-api).
    - When committing to a project, a bot must be registered to the particular project or the commit is rejected ([#45](https://github.com/EclipseFdn/git-eca-rest-api/issues/45)).
    - Additionally, if a bot user makes a commit to a repository from an email address tracked for a different service, that commit is still considered valid ([#45](https://github.com/EclipseFdn/git-eca-rest-api/issues/45)).
3. The committing user must be the same as the authoring user.  
    - The one exception to this is when the committing user is a project committer, who may commit on behalf of other users  
4. Requests made to non-project repositories by default will be allowed to pass with warnings to enable [ECA](https://accounts.eclipse.org/user/eca) validation to be run across a system with projects or repositories not directly managed by the Eclipse Foundation, such as forks or supporting projects ([#26](https://github.com/EclipseFdn/git-eca-rest-api/issues/26)).
    - Within the API, a request can be made under 'strict mode' to enforce contribution guidelines within repositories not covered by an active project ([#43](https://github.com/EclipseFdn/git-eca-rest-api/pull/43)). 

While these rules apply to all project repositories, any non-project repositories will also be checked. The messages indicate the failures as warnings to the consuming service (like Gitlab or Gerrit) unless 'strict mode' is enabled for the service. Whether or not a repository is tracked (or if it is a project repository) is determined by its presence as a repository directly linked to an active project within the [PMI](https://projects.eclipse.org/), as reported by the [Projects API](https://api.eclipse.org/#tag/Projects). 

## Submitting applications for existing bots

To submit requests for existing bots to be registered within our API (such as dependabot), please see the [Project Bots API repository](https://github.com/EclipseFdn/projects-bots-api) and create an issue.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```
./mvnw quarkus:dev
```

## Packaging and running the application

The application is packageable using `./mvnw package`.
It produces the executable `git-eca-rest-api-0.0.1-runner.jar` file in `/target` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/lib` directory.

The application is now runnable using `java -jar target/git-eca-rest-api-0.0.1-runner.jar`.

## Enabling commit hook in GitLab

To enable the Git hook that makes use of this service, a running GitLab instance will be needed with shell access. This instruction set assumes that the running GitLab instance runs using the Omnibus set up rather than the source. For the differences in process, please see the [GitLab custom hook administration instructions](https://docs.gitlab.com/ee/administration/custom_hooks.html). Once obtained, the following steps can be used to start or update the hook.

1. Access the GitLab server shell, and create a folder at `/opt/gitlab/embedded/service/gitlab-shell/hooks/pre-receive.d/` if it doesn't already exist. This folder will contain all of the servers global Git hooks for pre-receive events. These hooks trigger when a user attempts to push information to the server.  
1. In the host machine, copy the ECA script to the newly created folder. If using a docker container, this can be done with a call similar to the following:  
`docker cp src/main/rb/eca.rb gitlab.eca_web_1:/opt/gitlab/embedded/service/gitlab-shell/hooks/pre-receive.d/`

1. In the GitLab shell once again, ensure that the newly copied script matches the folders ownership, and that the file permissions are `755`. This allows the server to properly run the hook when needed.
