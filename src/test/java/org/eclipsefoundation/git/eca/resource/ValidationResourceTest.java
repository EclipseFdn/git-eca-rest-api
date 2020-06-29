/*******************************************************************************
 * Copyright (C) 2020 Eclipse Foundation
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipsefoundation.git.eca.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipsefoundation.git.eca.model.Commit;
import org.eclipsefoundation.git.eca.model.GitUser;
import org.eclipsefoundation.git.eca.model.ValidationRequest;
import org.eclipsefoundation.git.eca.namespace.APIStatusCode;
import org.eclipsefoundation.git.eca.namespace.ProviderType;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

/**
 * Tests for verifying end to end validation via the endpoint. Uses restassured
 * to create pseudo requests, and Mock API endpoints to ensure that all data is
 * kept internal for test checks.
 * 
 * @author Martin Lowe
 *
 */
@QuarkusTest
public class ValidationResourceTest {

	@Test
	void validate() throws MalformedURLException {
		// set up test users
		GitUser g1 = new GitUser();
		g1.setName("The Wizard");
		g1.setMail("code.wiz@important.co");

		List<Commit> commits = new ArrayList<>();
		// create sample commits
		Commit c1 = new Commit();
		c1.setAuthor(g1);
		c1.setCommitter(g1);
		c1.setBody("Signed-off-by: The Wizard <code.wiz@important.co>");
		c1.setHash("123456789abcdefghijklmnop");
		c1.setSubject("All of the things");
		c1.setParents(Collections.emptyList());
		commits.add(c1);
		
		ValidationRequest vr = new ValidationRequest();
		vr.setProvider(ProviderType.GITHUB);
		vr.setRepoUrl(new URL("http://www.github.com/eclipsefdn/sample"));
		vr.setCommits(commits);
		
		// test output w/ assertions
		given()
			.body(vr)
			.contentType(ContentType.JSON)
				.when().post("/eca")
				.then()
					.statusCode(200)
					.body("passed", is(true),
							"errorCount", is(0));
	}

	@Test
	void validateMultipleCommits() throws MalformedURLException {
		// set up test users
		GitUser g1 = new GitUser();
		g1.setName("The Wizard");
		g1.setMail("code.wiz@important.co");

		GitUser g2 = new GitUser();
		g2.setName("Grunts McGee");
		g2.setMail("grunt@important.co");

		List<Commit> commits = new ArrayList<>();
		// create sample commits
		Commit c1 = new Commit();
		c1.setAuthor(g1);
		c1.setCommitter(g1);
		c1.setBody("Signed-off-by: The Wizard <code.wiz@important.co>");
		c1.setHash("123456789abcdefghijklmnop");
		c1.setSubject("All of the things");
		c1.setParents(Collections.emptyList());
		commits.add(c1);

		Commit c2 = new Commit();
		c2.setAuthor(g2);
		c2.setCommitter(g2);
		c2.setBody("Signed-off-by: Grunts McGee<grunt@important.co>");
		c2.setHash("c044dca1847c94e709601651339f88a5c82e3cc7");
		c2.setSubject("Add in feature");
		c2.setParents(
				Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
		commits.add(c2);
		
		ValidationRequest vr = new ValidationRequest();
		vr.setProvider(ProviderType.GITHUB);
		vr.setRepoUrl(new URL("http://www.github.com/eclipsefdn/sample"));
		vr.setCommits(commits);
		
		// test output w/ assertions
		given()
			.body(vr)
			.contentType(ContentType.JSON)
				.when().post("/eca")
				.then()
					.statusCode(200)
					.body("passed", is(true),
							"errorCount", is(0));
	}
	
	@Test
	void validateMergeCommit() throws MalformedURLException {
		// set up test users
		GitUser g1 = new GitUser();
		g1.setName("Rando Calressian");
		g1.setMail("rando@nowhere.co");

		List<Commit> commits = new ArrayList<>();
		// create sample commits
		Commit c1 = new Commit();
		c1.setAuthor(g1);
		c1.setCommitter(g1);
		c1.setBody(String.format("Signed-off-by: %s <%s>", g1.getName(), g1.getMail()));
		c1.setHash("123456789abcdefghijklmnop");
		c1.setSubject("All of the things");
		c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10", "46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c11"));
		commits.add(c1);
		
		ValidationRequest vr = new ValidationRequest();
		vr.setProvider(ProviderType.GITHUB);
		vr.setRepoUrl(new URL("http://www.github.com/eclipsefdn/sample"));
		vr.setCommits(commits);
		// test output w/ assertions
		// No errors expected, should pass as only commit is a valid merge commit
		given()
			.body(vr)
			.contentType(ContentType.JSON)
				.when().post("/eca")
				.then()
					.statusCode(200)
					.body("passed", is(true),
							"errorCount", is(0));
	}
	
	@Test
	void validateCommitNoSignOffCommitter() throws MalformedURLException {
		// set up test users
		GitUser g1 = new GitUser();
		g1.setName("Grunts McGee");
		g1.setMail("grunt@important.co");

		List<Commit> commits = new ArrayList<>();
		// create sample commits
		Commit c1 = new Commit();
		c1.setAuthor(g1);
		c1.setCommitter(g1);
		c1.setBody("");
		c1.setHash("123456789abcdefghijklmnop");
		c1.setSubject("All of the things");
		c1.setParents(Collections.emptyList());
		commits.add(c1);
		
		ValidationRequest vr = new ValidationRequest();
		vr.setProvider(ProviderType.GITHUB);
		vr.setRepoUrl(new URL("http://www.github.com/eclipsefdn/prototype"));
		vr.setCommits(commits);
		
		// test output w/ assertions
		// Should be valid as Grunt is a committer on the prototype project
		given()
			.body(vr)
			.contentType(ContentType.JSON)
				.when().post("/eca")
				.then()
					.statusCode(200)
					.body("passed", is(true),
							"errorCount", is(0));
	}
	
	@Test
	void validateCommitNoSignOffNonCommitter() throws MalformedURLException {
		// set up test users
		GitUser g1 = new GitUser();
		g1.setName("The Wizard");
		g1.setMail("code.wiz@important.co");

		List<Commit> commits = new ArrayList<>();
		// create sample commits
		Commit c1 = new Commit();
		c1.setAuthor(g1);
		c1.setCommitter(g1);
		c1.setBody("");
		c1.setHash("123456789abcdefghijklmnop");
		c1.setSubject("All of the things");
		c1.setParents(Collections.emptyList());
		commits.add(c1);
		
		ValidationRequest vr = new ValidationRequest();
		vr.setProvider(ProviderType.GITHUB);
		vr.setRepoUrl(new URL("http://www.github.com/eclipsefdn/prototype"));
		vr.setCommits(commits);
		
		// test output w/ assertions
		// Should be invalid as Wizard is not a committer on the prototype project
		given()
			.body(vr)
			.contentType(ContentType.JSON)
				.when().post("/eca")
				.then()
					.statusCode(403)
					.body("passed", is(false),
							"errorCount", is(1),
							"commits.123456789abcdefghijklmnop.errors[0].code", 
							is(APIStatusCode.ERROR_SIGN_OFF.getValue()));
	}
	
	@Test
	void validateCommitInvalidSignOff() throws MalformedURLException {
		// set up test users
		GitUser g1 = new GitUser();
		g1.setName("Barshall Blathers");
		g1.setMail("slom@eclipse-foundation.org");

		List<Commit> commits = new ArrayList<>();
		// create sample commits
		Commit c1 = new Commit();
		c1.setAuthor(g1);
		c1.setCommitter(g1);
		c1.setBody(String.format("Signed-off-by: %s <%s>", g1.getName(), "barshallb@personal.co"));
		c1.setHash("123456789abcdefghijklmnop");
		c1.setSubject("All of the things");
		c1.setParents(Collections.emptyList());
		commits.add(c1);
		
		ValidationRequest vr = new ValidationRequest();
		vr.setProvider(ProviderType.GITHUB);
		vr.setRepoUrl(new URL("http://www.github.com/eclipsefdn/prototype"));
		vr.setCommits(commits);
		
		// test output w/ assertions
		// Should be invalid as a different email was associated with the footer
		given()
			.body(vr)
			.contentType(ContentType.JSON)
				.when().post("/eca")
				.then()
					.statusCode(403)
					.body("passed", is(false),
							"errorCount", is(1),
							"commits.123456789abcdefghijklmnop.errors[0].code", 
							is(APIStatusCode.ERROR_SIGN_OFF.getValue()));
	}

	@Test
	void validateWorkingGroupSpecAccess() throws MalformedURLException {
		// set up test users
		GitUser g1 = new GitUser();
		g1.setName("The Wizard");
		g1.setMail("code.wiz@important.co");

		GitUser g2 = new GitUser();
		g2.setName("Grunts McGee");
		g2.setMail("grunt@important.co");

		// CASE 1: WG Spec project write access valid
		List<Commit> commits = new ArrayList<>();
		// create sample commits
		Commit c1 = new Commit();
		c1.setAuthor(g1);
		c1.setCommitter(g1);
		c1.setBody("Signed-off-by: The Wizard <code.wiz@important.co>");
		c1.setHash("123456789abcdefghijklmnop");
		c1.setSubject("All of the things");
		c1.setParents(Collections.emptyList());
		commits.add(c1);
		
		ValidationRequest vr = new ValidationRequest();
		vr.setProvider(ProviderType.GITHUB);
		vr.setRepoUrl(new URL("http://www.github.com/eclipsefdn/tck-proto"));
		vr.setCommits(commits);
		
		// test output w/ assertions
		// Should be valid as Wizard has spec project write access + is committer
		given()
			.body(vr)
			.contentType(ContentType.JSON)
				.when().post("/eca")
				.then()
					.statusCode(200)
					.body("passed", is(true),
							"errorCount", is(0));
		
		// CASE 2: No WG Spec proj write access
		commits = new ArrayList<>();
		// create sample commits
		c1 = new Commit();
		c1.setAuthor(g2);
		c1.setCommitter(g2);
		c1.setBody(String.format("Signed-off-by: %s <%s>", g2.getName(), g2.getMail()));
		c1.setHash("123456789abcdefghijklmnop");
		c1.setSubject("All of the things");
		c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
		commits.add(c1);
		
		vr = new ValidationRequest();
		vr.setProvider(ProviderType.GITHUB);
		vr.setRepoUrl(new URL("http://www.github.com/eclipsefdn/tck-proto"));
		vr.setCommits(commits);

		// test output w/ assertions
		// Should be invalid as Grunt does not have spec project write access
		given()
			.body(vr)
			.contentType(ContentType.JSON)
				.when().post("/eca")
				.then()
					.statusCode(403)
					.body("passed", is(false),
							"errorCount", is(1),
							"commits.123456789abcdefghijklmnop.errors[0].code", 
							is(APIStatusCode.ERROR_SPEC_PROJECT.getValue()));
	}
	
	@Test
	void validateProxyCommitPush() throws MalformedURLException {
		// set up test users
		GitUser g1 = new GitUser();
		g1.setName("The Wizard");
		g1.setMail("code.wiz@important.co");

		GitUser g2 = new GitUser();
		g2.setName("Barshall Blathers");
		g2.setMail("slom@eclipse-foundation.org");

		// CASE 1: Committer pushing for non-committer author
		List<Commit> commits = new ArrayList<>();
		// create sample commits
		Commit c1 = new Commit();
		c1.setAuthor(g2);
		c1.setCommitter(g1);
		c1.setBody(String.format("Signed-off-by: %s <%s>", g2.getName(), g2.getMail()));
		c1.setHash("123456789abcdefghijklmnop");
		c1.setSubject("All of the things");
		c1.setParents(Collections.emptyList());
		commits.add(c1);
		
		ValidationRequest vr = new ValidationRequest();
		vr.setProvider(ProviderType.GITHUB);
		vr.setRepoUrl(new URL("http://www.github.com/eclipsefdn/tck-proto"));
		vr.setCommits(commits);
		
		// test output w/ assertions
		// Should be valid as Wizard is a committer on proj
		given()
			.body(vr)
			.contentType(ContentType.JSON)
				.when().post("/eca")
				.then()
					.statusCode(200)
					.body("passed", is(true),
							"errorCount", is(0));
		
		// CASE 2: Non-committer pushing for non-committer author
		commits = new ArrayList<>();
		// create sample commits
		c1 = new Commit();
		c1.setAuthor(g2);
		c1.setCommitter(g1);
		c1.setBody(String.format("Signed-off-by: %s <%s>", g2.getName(), g2.getMail()));
		c1.setHash("123456789abcdefghijklmnop");
		c1.setSubject("All of the things");
		c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
		commits.add(c1);
		
		vr = new ValidationRequest();
		vr.setProvider(ProviderType.GITHUB);
		vr.setRepoUrl(new URL("http://www.github.com/eclipsefdn/prototype"));
		vr.setCommits(commits);

		// test output w/ assertions
		// Should be invalid as Wizard is not a committer on proj
		given()
			.body(vr)
			.contentType(ContentType.JSON)
				.when().post("/eca")
				.then()
					.statusCode(403)
					.body("passed", is(false),
							"errorCount", is(1));
	}

	@Test
	void validateNoECA() throws MalformedURLException {
		// set up test users
		GitUser g1 = new GitUser();
		g1.setName("Newbie Anon");
		g1.setMail("newbie@important.co");

		List<Commit> commits = new ArrayList<>();
		// create sample commits
		Commit c1 = new Commit();
		c1.setAuthor(g1);
		c1.setCommitter(g1);
		c1.setBody(String.format("Signed-off-by: %s <%s>", g1.getName(), g1.getMail()));
		c1.setHash("123456789abcdefghijklmnop");
		c1.setSubject("All of the things");
		c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
		commits.add(c1);
		
		ValidationRequest vr = new ValidationRequest();
		vr.setProvider(ProviderType.GITHUB);
		vr.setRepoUrl(new URL("http://www.github.com/eclipsefdn/sample"));
		vr.setCommits(commits);
		// test output w/ assertions
		// Error should be singular + that there's no ECA on file
		// Status 403 (forbidden) is the standard return for invalid requests
		given()
			.body(vr)
			.contentType(ContentType.JSON)
				.when().post("/eca")
				.then()
					.statusCode(403)
					.body("passed", is(false),
							"errorCount", is(1));
	}
	
	@Test
	void validateAuthorNoEclipseAccount() throws MalformedURLException {
		// set up test users
		GitUser g1 = new GitUser();
		g1.setName("Rando Calressian");
		g1.setMail("rando@nowhere.co");
		
		GitUser g2 = new GitUser();
		g2.setName("Grunts McGee");
		g2.setMail("grunt@important.co");

		List<Commit> commits = new ArrayList<>();
		// create sample commits
		Commit c1 = new Commit();
		c1.setAuthor(g1);
		c1.setCommitter(g2);
		c1.setBody(String.format("Signed-off-by: %s <%s>", g1.getName(), g1.getMail()));
		c1.setHash("123456789abcdefghijklmnop");
		c1.setSubject("All of the things");
		c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
		commits.add(c1);
		
		ValidationRequest vr = new ValidationRequest();
		vr.setProvider(ProviderType.GITHUB);
		vr.setRepoUrl(new URL("http://www.github.com/eclipsefdn/sample"));
		vr.setCommits(commits);
		// test output w/ assertions
		// Error should be singular + that there's no Eclipse Account on file for author
		// Status 403 (forbidden) is the standard return for invalid requests
		given()
			.body(vr)
			.contentType(ContentType.JSON)
				.when().post("/eca")
				.then()
					.statusCode(403)
					.body("passed", is(false),
							"errorCount", is(1));
	}
	
	@Test
	void validateCommitterNoEclipseAccount() throws MalformedURLException {
		// set up test users
		GitUser g1 = new GitUser();
		g1.setName("Rando Calressian");
		g1.setMail("rando@nowhere.co");
		
		GitUser g2 = new GitUser();
		g2.setName("Grunts McGee");
		g2.setMail("grunt@important.co");

		List<Commit> commits = new ArrayList<>();
		// create sample commits
		Commit c1 = new Commit();
		c1.setAuthor(g2);
		c1.setCommitter(g1);
		c1.setBody(String.format("Signed-off-by: %s <%s>", g2.getName(), g2.getMail()));
		c1.setHash("123456789abcdefghijklmnop");
		c1.setSubject("All of the things");
		c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
		commits.add(c1);
		
		ValidationRequest vr = new ValidationRequest();
		vr.setProvider(ProviderType.GITHUB);
		vr.setRepoUrl(new URL("http://www.github.com/eclipsefdn/sample"));
		vr.setCommits(commits);
		// test output w/ assertions
		// Error should be singular + that there's no Eclipse Account on file for committer
		// Status 403 (forbidden) is the standard return for invalid requests
		given()
			.body(vr)
			.contentType(ContentType.JSON)
				.when().post("/eca")
				.then()
					.statusCode(403)
					.body("passed", is(false),
							"errorCount", is(1));
	}
}
