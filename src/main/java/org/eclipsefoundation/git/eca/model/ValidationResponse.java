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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipsefoundation.git.eca.namespace.APIStatusCode;

/**
 * Represents an internal response for a call to this API.
 * 
 * @author Martin Lowe
 *
 */
public class ValidationResponse {
	private boolean passed;
	private int errorCount;
	private Date time;
	private Map<String, CommitStatus> commits;
	private boolean trackedProject;

	public ValidationResponse() {
		this.commits = new HashMap<>();
		this.time = new Date();
	}

	/**
	 * @return the passed
	 */
	public boolean isPassed() {
		return passed;
	}

	/**
	 * @param passed the passed to set
	 */
	public void setPassed(boolean passed) {
		this.passed = passed;
	}

	/**
	 * @return the errorCount
	 */
	public int getErrorCount() {
		this.errorCount = commits.values().stream().mapToInt(s -> s.getErrors().size()).sum();
		return errorCount;
	}

	/**
	 * @param errorCount the errorCount to set
	 */
	public void setErrorCount(int errorCount) {
		this.errorCount = errorCount;
	}

	/**
	 * @return the time
	 */
	public Date getTime() {
		return time;
	}

	/**
	 * @param time the time to set
	 */
	public void setTime(Date time) {
		this.time = time;
	}

	/**
	 * @return the commits
	 */
	public Map<String, CommitStatus> getCommits() {
		return commits;
	}

	/**
	 * @param commits the commits to set
	 */
	public void setCommits(Map<String, CommitStatus> commits) {
		this.commits = commits;
	}

	/**
	 * @param message message to add to the API response
	 */
	public void addMessage(String hash, String message, APIStatusCode code) {
		commits.computeIfAbsent(getHashKey(hash), k -> new CommitStatus()).addMessage(message, code);
	}

	/**
	 * @param warning message to add to the API response
	 */
	public void addWarning(String hash, String warning, APIStatusCode code) {
		commits.computeIfAbsent(getHashKey(hash), k -> new CommitStatus()).addWarning(warning, code);
	}

	/**
	 * @param error message to add to the API response
	 */
	public void addError(String hash, String error, APIStatusCode code) {
		commits.computeIfAbsent(getHashKey(hash), k -> new CommitStatus()).addError(error, code);
	}

	/**
	 * @return the trackedProject
	 */
	public boolean isTrackedProject() {
		return trackedProject;
	}

	/**
	 * @param trackedProject the trackedProject to set
	 */
	public void setTrackedProject(boolean trackedProject) {
		this.trackedProject = trackedProject;
	}

	private String getHashKey(String hash) {
		return hash == null ? "_nil" : hash;
	}

	/**
	 * Converts the APIResponse to a web response with appropriate status.
	 * 
	 * @return a web response with status {@link Status.OK} if the commits pass
	 *         validation, {@link Status.FORBIDDEN} otherwise.
	 */
	public Response toResponse() {
		// update error count before returning
		if (passed) {
			return Response.ok(this).build();
		} else {
			return Response.status(Status.FORBIDDEN).entity(this).build();
		}
	}
}
