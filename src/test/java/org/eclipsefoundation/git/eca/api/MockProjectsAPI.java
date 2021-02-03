/*******************************************************************************
 * Copyright (C) 2020 Eclipse Foundation
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipsefoundation.git.eca.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.eclipsefoundation.git.eca.model.Project;
import org.eclipsefoundation.git.eca.model.Project.Repo;
import org.eclipsefoundation.git.eca.model.Project.User;

import io.quarkus.test.Mock;

@Mock
@RestClient
@ApplicationScoped
public class MockProjectsAPI implements ProjectsAPI {

	private List<Project> src;

	@PostConstruct
	public void build() {
		this.src = new ArrayList<>();

		// sample repos
		Repo r1 = new Repo();
		r1.setUrl("http://www.github.com/eclipsefdn/sample");
		Repo r2 = new Repo();
		r2.setUrl("http://www.github.com/eclipsefdn/test");
		Repo r3 = new Repo();
		r3.setUrl("http://www.github.com/eclipsefdn/prototype.git");
		Repo r4 = new Repo();
		r4.setUrl("http://www.github.com/eclipsefdn/tck-proto");
		Repo r5 = new Repo();
		r5.setUrl("/gitroot/sample/gerrit.project.git");
		Repo r6 = new Repo();
		r6.setUrl("/gitroot/sample/gerrit.other-project");
		Repo r7 = new Repo();
		r7.setUrl("https://gitlab.eclipse.org/eclipse/dash/dash.git");
		Repo r8 = new Repo();
		r8.setUrl("https://gitlab.eclipse.org/eclipse/dash/dash.handbook.test");

		// sample users, correlates to users in Mock projects API
		User u1 = new User();
		u1.setUrl("");
		u1.setUsername("da_wizz");

		User u2 = new User();
		u2.setUrl("");
		u2.setUsername("grunter");

		// projects
		Project p1 = new Project();
		p1.setName("Sample project");
		p1.setProjectId("sample.proj");
		p1.setSpecWorkingGroup(null);
		p1.setGithubRepos(Arrays.asList(r1, r2));
		p1.setGerritRepos(Arrays.asList(r5));
		p1.setCommitters(Arrays.asList(u1, u2));
		src.add(p1);

		Project p2 = new Project();
		p2.setName("Prototype thing");
		p2.setProjectId("sample.proto");
		p2.setSpecWorkingGroup(null);
		p2.setGithubRepos(Arrays.asList(r3));
		p1.setGerritRepos(Arrays.asList(r6));
		p1.setGitlabRepos(Arrays.asList(r8));
		p2.setCommitters(Arrays.asList(u2));
		src.add(p2);

		Project p3 = new Project();
		p3.setName("Spec project");
		p3.setProjectId("spec.proj");
		p3.setSpecWorkingGroup("proj1");
		p3.setGithubRepos(Arrays.asList(r4));
		p1.setGitlabRepos(Arrays.asList(r7));
		p3.setCommitters(Arrays.asList(u1, u2));
		src.add(p3);
	}

	@Override
	public List<Project> getProject(int page, int pageSize) {
		return page == 1 ? new ArrayList<>(src) : Collections.emptyList();
	}
}
