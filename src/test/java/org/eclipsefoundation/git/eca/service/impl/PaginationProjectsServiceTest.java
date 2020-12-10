/**
 * Copyright (C) 2020 Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipsefoundation.git.eca.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipsefoundation.git.eca.model.Project;
import org.eclipsefoundation.git.eca.model.Project.Repo;
import org.eclipsefoundation.git.eca.service.ProjectsService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class PaginationProjectsServiceTest {
  // get the projects service
  @Inject ProjectsService ps;

  @Test
  void validateGerritUrlScrubbed() {
    // get all projects
    List<Project> projectsAll = ps.getProjects();
    // get projects that have gerrit repos
    List<Project> projs =
        projectsAll
            .stream()
            .filter(p -> !p.getGerritRepos().isEmpty())
            .collect(Collectors.toList());
    // for all repos, check that none end with .git (this doesn't account for .git.git, but I assume
    // that would be an entry error)
    for (Project p : projs) {
      for (Repo r : p.getGerritRepos()) {
        Assertions.assertFalse(r.getUrl().endsWith(".git"), "Expected no URLs to end with '.git'");
      }
    }
  }

  @Test
  void validateGithubUrlNotScrubbed() {
    // get all projects
    List<Project> projectsAll = ps.getProjects();
    // get projects that have github repos
    List<Project> projs =
        projectsAll
            .stream()
            .filter(p -> !p.getGithubRepos().isEmpty())
            .collect(Collectors.toList());
    // for all repos, check that at least one ends with .git
    boolean foundGitSuffix = false;
    for (Project p : projs) {
      for (Repo r : p.getGithubRepos()) {
        if (r.getUrl().endsWith(".git")) {
          foundGitSuffix = true;
        }
      }
    }
    Assertions.assertTrue(foundGitSuffix, "Expected a URL to end with '.git'");
  }

  @Test
  void validateGitlabUrlNotScrubbed() {
    // get all projects
    List<Project> projectsAll = ps.getProjects();
    // get projects that have gitlab repos
    List<Project> projs =
        projectsAll
            .stream()
            .filter(p -> !p.getGitlabRepos().isEmpty())
            .collect(Collectors.toList());
    // for all repos, check that at least one ends with .git
    boolean foundGitSuffix = false;
    for (Project p : projs) {
      for (Repo r : p.getGitlabRepos()) {
        if (r.getUrl().endsWith(".git")) {
          foundGitSuffix = true;
        }
      }
    }
    Assertions.assertTrue(foundGitSuffix, "Expected a URL to end with '.git'");
  }
}
