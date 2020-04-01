/*******************************************************************************
 * Copyright (C) 2020 Eclipse Foundation
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipsefoundation.git.eca.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Represents a project in the Eclipse API, along with the users and repos that
 * exist within the context of the project.
 * 
 * @author Martin Lowe
 *
 */
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Project {
	private String projectId;
	private String name;
	private List<User> committers;
	private List<Repo> repos;
	private List<Repo> gitlabRepos;
	private List<Repo> githubRepos;
	private String specWorkingGroup;
	
	public Project() {
		this.committers = new ArrayList<>();
		this.repos = new ArrayList<>();
		this.gitlabRepos = new ArrayList<>();
		this.githubRepos = new ArrayList<>();
	}

	/**
	 * @return the projectId
	 */
	public String getProjectId() {
		return projectId;
	}

	/**
	 * @param projectId the projectId to set
	 */
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the committers
	 */
	public List<User> getCommitters() {
		return committers;
	}

	/**
	 * @param committers the committers to set
	 */
	public void setCommitters(List<User> committers) {
		this.committers = committers;
	}

	/**
	 * @return the repos
	 */
	public List<Repo> getRepos() {
		return new ArrayList<>(repos);
	}

	/**
	 * @param repos the repos to set
	 */
	public void setRepos(List<Repo> repos) {
		this.repos = new ArrayList<>(repos);
	}

	/**
	 * @return the gitlabRepos
	 */
	public List<Repo> getGitlabRepos() {
		return new ArrayList<>(gitlabRepos);
	}

	/**
	 * @param githubRepos the githubRepos to set
	 */
	public void setGithubRepos(List<Repo> githubRepos) {
		this.githubRepos = new ArrayList<>(githubRepos);
	}

	/**
	 * @return the gitlabRepos
	 */
	public List<Repo> getGithubRepos() {
		return new ArrayList<>(githubRepos);
	}

	/**
	 * @param gitlabRepos the gitlabRepos to set
	 */
	public void setGitlabRepos(List<Repo> gitlabRepos) {
		this.gitlabRepos = new ArrayList<>(gitlabRepos);
	}

	/**
	 * @return the specWorkingGroup
	 */
	public String getSpecWorkingGroup() {
		return specWorkingGroup;
	}

	/**
	 * @param specWorkingGroup the specWorkingGroup to set
	 */
	public void setSpecWorkingGroup(String specWorkingGroup) {
		this.specWorkingGroup = specWorkingGroup;
	}

	/**
	 * @param specProjectWorkingGroup the value for the spec_project_working_group.
	 *                                When empty from API, represented by an empty
	 *                                array, and a map/object when set.
	 */
	@SuppressWarnings("unchecked")
	@JsonProperty("spec_project_working_group")
	private void unpackSpecProject(Object specProjectWorkingGroup) {
		if (specProjectWorkingGroup instanceof Map) {
			Object raw = ((Map<String, Object>) specProjectWorkingGroup).get("id");
			if (raw instanceof String) {
				this.specWorkingGroup = (String) raw;
			}
		}
	}

	public static class Repo {
		private String url;

		/**
		 * @return the url
		 */
		public String getUrl() {
			return url;
		}

		/**
		 * @param url the url to set
		 */
		public void setUrl(String url) {
			this.url = url;
		}
	}

	public static class User {
		private String username;
		private String url;

		/**
		 * @return the username
		 */
		public String getUsername() {
			return username;
		}

		/**
		 * @param username the username to set
		 */
		public void setUsername(String username) {
			this.username = username;
		}

		/**
		 * @return the url
		 */
		public String getUrl() {
			return url;
		}

		/**
		 * @param url the url to set
		 */
		public void setUrl(String url) {
			this.url = url;
		}
	}
}
