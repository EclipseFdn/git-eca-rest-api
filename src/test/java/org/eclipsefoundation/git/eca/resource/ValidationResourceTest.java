/**
 * Copyright (C) 2020 Eclipse Foundation
 *
 * <p>This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 */
package org.eclipsefoundation.git.eca.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.eclipsefoundation.git.eca.model.Commit;
import org.eclipsefoundation.git.eca.model.GitUser;
import org.eclipsefoundation.git.eca.model.ValidationRequest;
import org.eclipsefoundation.git.eca.namespace.APIStatusCode;
import org.eclipsefoundation.git.eca.namespace.ProviderType;
import org.eclipsefoundation.git.eca.service.CachingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

/**
 * Tests for verifying end to end validation via the endpoint. Uses restassured to create pseudo
 * requests, and Mock API endpoints to ensure that all data is kept internal for test checks.
 *
 * @author Martin Lowe
 */
@QuarkusTest
class ValidationResourceTest {

    @Inject
    CachingService cs;
    
    
    @BeforeEach
    void cacheClear() {
        // if dev servers are run on the same machine, some values may live in the cache
        cs.removeAll();
    }
    
  @Test
  void validate() throws URISyntaxException {
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
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/sample"));
    vr.setCommits(commits);

    // test output w/ assertions
    given()
        .body(vr)
        .contentType(ContentType.JSON)
        .when()
        .post("/eca")
        .then()
        .statusCode(200)
        .body("passed", is(true), "errorCount", is(0));
  }

  @Test
  void validateMultipleCommits() throws URISyntaxException {
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
    c2.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
    commits.add(c2);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GITHUB);
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/sample"));
    vr.setCommits(commits);

    // test output w/ assertions
    given()
        .body(vr)
        .contentType(ContentType.JSON)
        .when()
        .post("/eca")
        .then()
        .statusCode(200)
        .body("passed", is(true), "errorCount", is(0));
  }

  @Test
  void validateMergeCommit() throws URISyntaxException {
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
    c1.setParents(
        Arrays.asList(
            "46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10",
            "46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c11"));
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GITHUB);
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/sample"));
    vr.setCommits(commits);
    // test output w/ assertions
    // No errors expected, should pass as only commit is a valid merge commit
    given()
        .body(vr)
        .contentType(ContentType.JSON)
        .when()
        .post("/eca")
        .then()
        .statusCode(200)
        .body("passed", is(true), "errorCount", is(0));
  }

  @Test
  void validateCommitNoSignOffCommitter() throws URISyntaxException {
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
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/prototype"));
    vr.setCommits(commits);

    // test output w/ assertions
    // Should be valid as Grunt is a committer on the prototype project
    given()
        .body(vr)
        .contentType(ContentType.JSON)
        .when()
        .post("/eca")
        .then()
        .statusCode(200)
        .body("passed", is(true), "errorCount", is(0));
  }

  @Test
  void validateCommitNoSignOffNonCommitter() throws URISyntaxException {
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
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/prototype.git"));
    vr.setCommits(commits);

    // test output w/ assertions
    // Should be valid as wizard has signed ECA
    given()
        .body(vr)
        .contentType(ContentType.JSON)
        .when()
        .post("/eca")
        .then()
        .statusCode(200)
        .body("passed", is(true), "errorCount", is(0));
  }

  @Test
  void validateCommitInvalidSignOff() throws URISyntaxException {
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
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/prototype.git"));
    vr.setCommits(commits);

    // test output w/ assertions
    // Should be valid as signed off by footer is no longer checked
    given()
        .body(vr)
        .contentType(ContentType.JSON)
        .when()
        .post("/eca")
        .then()
        .statusCode(200)
        .body("passed", is(true), "errorCount", is(0));
  }

  @Test
  void validateCommitSignOffMultipleFooterLines_Last() throws URISyntaxException {
    // set up test users
    GitUser g1 = new GitUser();
    g1.setName("Barshall Blathers");
    g1.setMail("slom@eclipse-foundation.org");

    List<Commit> commits = new ArrayList<>();
    // create sample commits
    Commit c1 = new Commit();
    c1.setAuthor(g1);
    c1.setCommitter(g1);
    c1.setBody(
        String.format(
            "Change-Id: 0000000000000001\nSigned-off-by: %s <%s>", g1.getName(), g1.getMail()));
    c1.setHash("123456789abcdefghijklmnop");
    c1.setSubject("All of the things");
    c1.setParents(Collections.emptyList());
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GITHUB);
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/prototype"));
    vr.setCommits(commits);

    // test output w/ assertions
    given()
        .body(vr)
        .contentType(ContentType.JSON)
        .when()
        .post("/eca")
        .then()
        .statusCode(200)
        .body("passed", is(true), "errorCount", is(0));
  }

  @Test
  void validateCommitSignOffMultipleFooterLines_First() throws URISyntaxException {
    // set up test users
    GitUser g1 = new GitUser();
    g1.setName("Barshall Blathers");
    g1.setMail("slom@eclipse-foundation.org");

    List<Commit> commits = new ArrayList<>();
    // create sample commits
    Commit c1 = new Commit();
    c1.setAuthor(g1);
    c1.setCommitter(g1);
    c1.setBody(
        String.format(
            "Signed-off-by: %s <%s>\nChange-Id: 0000000000000001", g1.getName(), g1.getMail()));
    c1.setHash("123456789abcdefghijklmnop");
    c1.setSubject("All of the things");
    c1.setParents(Collections.emptyList());
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GITHUB);
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/prototype"));
    vr.setCommits(commits);

    // test output w/ assertions
    given()
        .body(vr)
        .contentType(ContentType.JSON)
        .when()
        .post("/eca")
        .then()
        .statusCode(200)
        .body("passed", is(true), "errorCount", is(0));
  }

  @Test
  void validateCommitSignOffMultipleFooterLines_Multiple() throws URISyntaxException {
    // set up test users
    GitUser g1 = new GitUser();
    g1.setName("Barshall Blathers");
    g1.setMail("slom@eclipse-foundation.org");

    List<Commit> commits = new ArrayList<>();
    // create sample commits
    Commit c1 = new Commit();
    c1.setAuthor(g1);
    c1.setCommitter(g1);
    c1.setBody(
        String.format(
            "Change-Id: 0000000000000001\\nSigned-off-by: %s <%s>\nSigned-off-by: %s <%s>",
            g1.getName(), g1.getMail(), g1.getName(), "barshallb@personal.co"));
    c1.setHash("123456789abcdefghijklmnop");
    c1.setSubject("All of the things");
    c1.setParents(Collections.emptyList());
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GITHUB);
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/prototype"));
    vr.setCommits(commits);

    // test output w/ assertions
    given()
        .body(vr)
        .contentType(ContentType.JSON)
        .when()
        .post("/eca")
        .then()
        .statusCode(200)
        .body("passed", is(true), "errorCount", is(0));
  }

  @Test
  void validateWorkingGroupSpecAccess() throws URISyntaxException {
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
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/tck-proto"));
    vr.setCommits(commits);

    // test output w/ assertions
    // Should be valid as Wizard has spec project write access + is committer
    given()
        .body(vr)
        .contentType(ContentType.JSON)
        .when()
        .post("/eca")
        .then()
        .statusCode(200)
        .body("passed", is(true), "errorCount", is(0));

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
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/tck-proto"));
    vr.setCommits(commits);

    // test output w/ assertions
    // Should be invalid as Grunt does not have spec project write access
    // Should have 2 errors, as both users get validated
    given()
        .body(vr)
        .contentType(ContentType.JSON)
        .when()
        .post("/eca")
        .then()
        .statusCode(403)
        .body(
            "passed",
            is(false),
            "errorCount",
            is(2),
            "commits.123456789abcdefghijklmnop.errors[0].code",
            is(APIStatusCode.ERROR_SPEC_PROJECT.getValue()));
  }

  @Test
  void validateNoECA_author() throws URISyntaxException {
    // set up test users
    GitUser g1 = new GitUser();
    g1.setName("Newbie Anon");
    g1.setMail("newbie@important.co");

    GitUser g2 = new GitUser();
    g2.setName("The Wizard");
    g2.setMail("code.wiz@important.co");

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
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/sample"));
    vr.setCommits(commits);
    // test output w/ assertions
    // Error should be singular + that there's no ECA on file
    // Status 403 (forbidden) is the standard return for invalid requests
    given()
        .body(vr)
        .contentType(ContentType.JSON)
        .when()
        .post("/eca")
        .then()
        .statusCode(403)
        .body("passed", is(false), "errorCount", is(1));
  }

  @Test
  void validateNoECA_committer() throws URISyntaxException {
    // set up test users
    GitUser g1 = new GitUser();
    g1.setName("Newbie Anon");
    g1.setMail("newbie@important.co");

    GitUser g2 = new GitUser();
    g2.setName("The Wizard");
    g2.setMail("code.wiz@important.co");

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
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/sample"));
    vr.setCommits(commits);
    // test output w/ assertions
    // Error count should be 1 for just the committer access
    // Status 403 (forbidden) is the standard return for invalid requests
    given()
        .body(vr)
        .contentType(ContentType.JSON)
        .when()
        .post("/eca")
        .then()
        .statusCode(403)
        .body("passed", is(false), "errorCount", is(1));
  }
  @Test
  void validateNoECA_both() throws URISyntaxException {
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
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/sample"));
    vr.setCommits(commits);
    // test output w/ assertions
    // Should have 2 errors, 1 for author entry and 1 for committer entry
    // Status 403 (forbidden) is the standard return for invalid requests
    given()
        .body(vr)
        .contentType(ContentType.JSON)
        .when()
        .post("/eca")
        .then()
        .statusCode(403)
        .body("passed", is(false), "errorCount", is(2));
  }

  @Test
  void validateAuthorNoEclipseAccount() throws URISyntaxException {
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
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/sample"));
    vr.setCommits(commits);
    // test output w/ assertions
    // Error should be singular + that there's no Eclipse Account on file for author
    // Status 403 (forbidden) is the standard return for invalid requests
    given()
        .body(vr)
        .contentType(ContentType.JSON)
        .when()
        .post("/eca")
        .then()
        .statusCode(403)
        .body("passed", is(false), "errorCount", is(1));
  }

  @Test
  void validateCommitterNoEclipseAccount() throws URISyntaxException {
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
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/sample"));
    vr.setCommits(commits);
    // test output w/ assertions
    // Error should be singular + that there's no Eclipse Account on file for committer
    // Status 403 (forbidden) is the standard return for invalid requests
    given()
        .body(vr)
        .contentType(ContentType.JSON)
        .when()
        .post("/eca")
        .then()
        .statusCode(403)
        .body("passed", is(false), "errorCount", is(1));
  }

  @Test
  void validateProxyCommitUntrackedProject() throws URISyntaxException {
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
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/sample-not-tracked"));
    vr.setCommits(commits);
    // test output w/ assertions
    // Should be valid as project is not tracked
    given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(200);
  }

  @Test
  void validateBotCommiterAccessGithub() throws URISyntaxException {
    // set up test users
    GitUser g1 = new GitUser();
    g1.setName("projbot");
    g1.setMail("1.bot@eclipse.org");

    List<Commit> commits = new ArrayList<>();
    // create sample commits
    Commit c1 = new Commit();
    c1.setAuthor(g1);
    c1.setCommitter(g1);
    c1.setHash("123456789abcdefghijklmnop");
    c1.setSubject("All of the things");
    c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GITHUB);
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/sample"));
    vr.setCommits(commits);
    // test output w/ assertions
    // Should be valid as bots should only commit on their own projects (including aliases)
    given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(200);
  }

  @Test
  void validateBotCommiterAccessGithub_untracked() throws URISyntaxException {
    // set up test users
    GitUser g1 = new GitUser();
    g1.setName("projbot");
    g1.setMail("1.bot@eclipse.org");

    List<Commit> commits = new ArrayList<>();
    // create sample commits
    Commit c1 = new Commit();
    c1.setAuthor(g1);
    c1.setCommitter(g1);
    c1.setHash("123456789abcdefghijklmnop");
    c1.setSubject("All of the things");
    c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GITHUB);
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/sample-untracked"));
    vr.setCommits(commits);
    // test output w/ assertions
    // Should be valid as bots can commit on any untracked project (legacy support)
    given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(200);
  }

  @Test
  void validateBotCommiterAccessGithub_invalidBot() throws URISyntaxException {
    // set up test users
    GitUser g1 = new GitUser();
    g1.setName("protobot-gh");
    g1.setMail("2.bot-github@eclipse.org");

    List<Commit> commits = new ArrayList<>();
    // create sample commits
    Commit c1 = new Commit();
    c1.setAuthor(g1);
    c1.setCommitter(g1);
    c1.setHash("123456789abcdefghijklmnop");
    c1.setSubject("All of the things");
    c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GITHUB);
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/sample"));
    vr.setCommits(commits);
    // test output w/ assertions
    // Should be invalid as bots should only commit on their own projects (including aliases)
    given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(403);
  }

  @Test
  void validateBotCommiterAccessGithub_wrongEmail() throws URISyntaxException {
    // set up test users - uses Gerrit/LDAP email (wrong for case)
    GitUser g1 = new GitUser();
    g1.setName("protobot");
    g1.setMail("2.bot@eclipse.org");

    List<Commit> commits = new ArrayList<>();
    // create sample commits
    Commit c1 = new Commit();
    c1.setAuthor(g1);
    c1.setCommitter(g1);
    c1.setHash("123456789abcdefghijklmnop");
    c1.setSubject("All of the things");
    c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GITHUB);
    vr.setRepoUrl(new URI("http://www.github.com/eclipsefdn/sample"));
    vr.setCommits(commits);
    // test output w/ assertions
    // Should be invalid as wrong email was used for bot (uses Gerrit bot email)
    given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(403);
  }

  @Test
  void validateBotCommiterAccessGitlab() throws URISyntaxException {
      // set up test users
      GitUser g1 = new GitUser();
      g1.setName("protobot-gh");
      g1.setMail("2.bot-github@eclipse.org");

      List<Commit> commits = new ArrayList<>();
      // create sample commits
      Commit c1 = new Commit();
      c1.setAuthor(g1);
      c1.setCommitter(g1);
      c1.setHash("123456789abcdefghijklmnop");
      c1.setSubject("All of the things");
      c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
      commits.add(c1);

      ValidationRequest vr = new ValidationRequest();
      vr.setProvider(ProviderType.GITLAB);
      vr.setRepoUrl(new URI("https://gitlab.eclipse.org/eclipse/dash/dash.handbook.test"));
      vr.setCommits(commits);
      // test output w/ assertions
      // Should be valid as bots should only commit on their own projects (including aliases)
      given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(200);
  }

  @Test
  void validateBotCommiterAccessGitlab_untracked() throws URISyntaxException {
      // set up test users
      GitUser g1 = new GitUser();
      g1.setName("protobot-gh");
      g1.setMail("2.bot-github@eclipse.org");

      List<Commit> commits = new ArrayList<>();
      // create sample commits
      Commit c1 = new Commit();
      c1.setAuthor(g1);
      c1.setCommitter(g1);
      c1.setHash("123456789abcdefghijklmnop");
      c1.setSubject("All of the things");
      c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
      commits.add(c1);

      ValidationRequest vr = new ValidationRequest();
      vr.setProvider(ProviderType.GITLAB);
      vr.setRepoUrl(new URI("https://gitlab.eclipse.org/eclipse/dash/dash.handbook.untracked"));
      vr.setCommits(commits);
      // test output w/ assertions
      // Should be valid as bots can commit on any untracked project (legacy support)
      given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(200);
  }
  
  @Test
  void validateBotCommiterAccessGitlab_invalidBot() throws URISyntaxException {
    // set up test users (wrong bot for project)
    GitUser g1 = new GitUser();
    g1.setName("specbot");
    g1.setMail("3.bot@eclipse.org");

    List<Commit> commits = new ArrayList<>();
    // create sample commits
    Commit c1 = new Commit();
    c1.setAuthor(g1);
    c1.setCommitter(g1);
    c1.setHash("123456789abcdefghijklmnop");
    c1.setSubject("All of the things");
    c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GITLAB);
    vr.setRepoUrl(new URI("https://gitlab.eclipse.org/eclipse/dash/dash.handbook.test"));
    vr.setCommits(commits);
    // test output w/ assertions
    // Should be invalid as bots should only commit on their own projects
    given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(403);
  }

  @Test
  void validateBotCommiterAccessGitlab_wrongEmail() throws URISyntaxException {
    // set up test users - uses Gerrit/LDAP email (expects Gitlab email)
    GitUser g1 = new GitUser();
    g1.setName("specbot");
    g1.setMail("3.bot@eclipse.org");

    List<Commit> commits = new ArrayList<>();
    // create sample commits
    Commit c1 = new Commit();
    c1.setAuthor(g1);
    c1.setCommitter(g1);
    c1.setHash("123456789abcdefghijklmnop");
    c1.setSubject("All of the things");
    c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GITLAB);
    vr.setRepoUrl(new URI("https://gitlab.eclipse.org/eclipse/dash/dash.git"));
    vr.setCommits(commits);
    // test output w/ assertions
    // Should be valid as wrong email was used, but is still bot email alias (uses Gerrit bot email)
    given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(200);
  }

  @Test
  void validateBotCommiterAccessGerrit() throws URISyntaxException {
    // set up test users
    GitUser g1 = new GitUser();
    g1.setName("protobot");
    g1.setMail("2.bot@eclipse.org");

    List<Commit> commits = new ArrayList<>();
    // create sample commits
    Commit c1 = new Commit();
    c1.setAuthor(g1);
    c1.setCommitter(g1);
    c1.setHash("123456789abcdefghijklmnop");
    c1.setSubject("All of the things");
    c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GERRIT);
    vr.setRepoUrl(new URI("/gitroot/sample/gerrit.other-project"));
    vr.setCommits(commits);
    vr.setStrictMode(true);
    // test output w/ assertions
    // Should be valid as bots should only commit on their own projects (including aliases)
    given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(200);
  }

  @Test
  void validateBotCommiterAccessGerrit_untracked() throws URISyntaxException {
    // set up test users
    GitUser g1 = new GitUser();
    g1.setName("protobot");
    g1.setMail("2.bot@eclipse.org");

    List<Commit> commits = new ArrayList<>();
    // create sample commits
    Commit c1 = new Commit();
    c1.setAuthor(g1);
    c1.setCommitter(g1);
    c1.setHash("123456789abcdefghijklmnop");
    c1.setSubject("All of the things");
    c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GERRIT);
    vr.setRepoUrl(new URI("/gitroot/sample/untracked.project"));
    vr.setCommits(commits);
    vr.setStrictMode(true);
    // test output w/ assertions
    // Should be valid as bots can commit on any untracked project (legacy support)
    given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(200);
  }

  @Test
  void validateBotCommiterAccessGerrit_invalidBot() throws URISyntaxException {
      // set up test users -  (wrong bot for project)
      GitUser g1 = new GitUser();
      g1.setName("specbot");
      g1.setMail("3.bot@eclipse.org");

      List<Commit> commits = new ArrayList<>();
      // create sample commits
      Commit c1 = new Commit();
      c1.setAuthor(g1);
      c1.setCommitter(g1);
      c1.setHash("123456789abcdefghijklmnop");
      c1.setSubject("All of the things");
      c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
      commits.add(c1);

      ValidationRequest vr = new ValidationRequest();
      vr.setProvider(ProviderType.GERRIT);
      vr.setRepoUrl(new URI("/gitroot/sample/gerrit.other-project"));
      vr.setCommits(commits);
      vr.setStrictMode(true);
      // test output w/ assertions
      // Should be invalid as bots should only commit on their own projects (wrong project)
      given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(403);
      
  }

  @Test
  void validateBotCommiterAccessGerrit_aliasEmail() throws URISyntaxException {
    // set up test users - uses GH (instead of expected Gerrit/LDAP email)
    GitUser g1 = new GitUser();
    g1.setName("protobot-gh");
    g1.setMail("2.bot-github@eclipse.org");

    List<Commit> commits = new ArrayList<>();
    // create sample commits
    Commit c1 = new Commit();
    c1.setAuthor(g1);
    c1.setCommitter(g1);
    c1.setHash("123456789abcdefghijklmnop");
    c1.setSubject("All of the things");
    c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GERRIT);
    vr.setRepoUrl(new URI("/gitroot/sample/gerrit.other-project"));
    vr.setCommits(commits);
    vr.setStrictMode(true);
    // test output w/ assertions
    // Should be valid as wrong email was used, but is still bot email alias 
    given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(200);
  }

  @Test
  void validateNullEmailCheck() throws URISyntaxException {
      // set up test users - uses GH (instead of expected Gerrit/LDAP email)
      GitUser g1 = new GitUser();
      g1.setName("protobot-gh");
      g1.setMail("2.bot-github@eclipse.org");
      GitUser g2 = new GitUser();
      g2.setName("protobot-gh");

      List<Commit> commits = new ArrayList<>();
      // create sample commits
      Commit c1 = new Commit();
      c1.setAuthor(g1);
      c1.setCommitter(g2);
      c1.setHash("123456789abcdefghijklmnop");
      c1.setSubject("All of the things");
      c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
      commits.add(c1);

      ValidationRequest vr = new ValidationRequest();
      vr.setProvider(ProviderType.GERRIT);
      vr.setRepoUrl(new URI("/gitroot/sample/gerrit.other-project"));
      vr.setCommits(commits);
      vr.setStrictMode(true);
      // test output w/ assertions
      // Should be invalid as there is no email (refuse commit, not server error)
      given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(403);
  }

  @Test
  void validateGithubNoReply_legacy() throws URISyntaxException {
    GitUser g1 = new GitUser();
    g1.setName("grunter");
    g1.setMail("grunter@users.noreply.github.com");

    List<Commit> commits = new ArrayList<>();
    // create sample commits
    Commit c1 = new Commit();
    c1.setAuthor(g1);
    c1.setCommitter(g1);
    c1.setHash("123456789abcdefghijklmnop");
    c1.setSubject("All of the things");
    c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GERRIT);
    vr.setRepoUrl(new URI("/gitroot/sample/gerrit.other-project"));
    vr.setCommits(commits);
    vr.setStrictMode(true);
    // test output w/ assertions
    // Should be valid as grunter used a no-reply Github account and has a matching GH handle
    given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(200);
  }

  @Test
  void validateGithubNoReply_success() throws URISyntaxException {
    // sometimes the user ID and user name are reversed
    GitUser g1 = new GitUser();
    g1.setName("grunter");
    g1.setMail("123456789+grunter@users.noreply.github.com");

    List<Commit> commits = new ArrayList<>();
    // create sample commits
    Commit c1 = new Commit();
    c1.setAuthor(g1);
    c1.setCommitter(g1);
    c1.setHash("123456789abcdefghijklmnop");
    c1.setSubject("All of the things");
    c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GERRIT);
    vr.setRepoUrl(new URI("/gitroot/sample/gerrit.other-project"));
    vr.setCommits(commits);
    vr.setStrictMode(true);
    // test output w/ assertions
    // Should be valid as grunter used a no-reply Github account and has a matching GH handle
    given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(200);
  }

  @Test
  void validateGithubNoReply_nomatch() throws URISyntaxException {
    GitUser g1 = new GitUser();
    g1.setName("some_guy");
    g1.setMail("123456789+some_guy@users.noreply.github.com");

    List<Commit> commits = new ArrayList<>();
    // create sample commits
    Commit c1 = new Commit();
    c1.setAuthor(g1);
    c1.setCommitter(g1);
    c1.setHash("123456789abcdefghijklmnop");
    c1.setSubject("All of the things");
    c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GERRIT);
    vr.setRepoUrl(new URI("/gitroot/sample/gerrit.other-project"));
    vr.setCommits(commits);
    vr.setStrictMode(true);
    // test output w/ assertions
    // Should be invalid as no user exists with "Github" handle that matches some_guy
    given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(403);
  }

  @Test
  void validateGithubNoReply_nomatch_legacy() throws URISyntaxException {
    GitUser g1 = new GitUser();
    g1.setName("some_guy");
    g1.setMail("some_guy@users.noreply.github.com");

    List<Commit> commits = new ArrayList<>();
    // create sample commits
    Commit c1 = new Commit();
    c1.setAuthor(g1);
    c1.setCommitter(g1);
    c1.setHash("123456789abcdefghijklmnop");
    c1.setSubject("All of the things");
    c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GERRIT);
    vr.setRepoUrl(new URI("/gitroot/sample/gerrit.other-project"));
    vr.setCommits(commits);
    vr.setStrictMode(true);
    // test output w/ assertions
    // Should be invalid as no user exists with "Github" handle that matches some_guy
    given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(403);
  }

  @Test
  void validateAllowListAuthor_success() throws URISyntaxException {
    GitUser g1 = new GitUser();
    g1.setName("grunter");
    g1.setMail("grunter@users.noreply.github.com");
    GitUser g2 = new GitUser();
    g2.setName("grunter");
    g2.setMail("noreply@github.com");

    List<Commit> commits = new ArrayList<>();
    // create sample commits
    Commit c1 = new Commit();
    c1.setAuthor(g2);
    c1.setCommitter(g1);
    c1.setHash("123456789abcdefghijklmnop");
    c1.setSubject("All of the things");
    c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GERRIT);
    vr.setRepoUrl(new URI("/gitroot/sample/gerrit.other-project"));
    vr.setCommits(commits);
    vr.setStrictMode(true);
    // test output w/ assertions
    // Should be valid as grunter used a no-reply Github account and has a matching GH handle
    given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(200);
  }

  @Test
  void validateAllowListCommitter_success() throws URISyntaxException {
    GitUser g1 = new GitUser();
    g1.setName("grunter");
    g1.setMail("grunter@users.noreply.github.com");
    GitUser g2 = new GitUser();
    g2.setName("grunter");
    g2.setMail("noreply@github.com");

    List<Commit> commits = new ArrayList<>();
    // create sample commits
    Commit c1 = new Commit();
    c1.setAuthor(g1);
    c1.setCommitter(g2);
    c1.setHash("123456789abcdefghijklmnop");
    c1.setSubject("All of the things");
    c1.setParents(Arrays.asList("46bb69bf6aa4ed26b2bf8c322ae05bef0bcc5c10"));
    commits.add(c1);

    ValidationRequest vr = new ValidationRequest();
    vr.setProvider(ProviderType.GERRIT);
    vr.setRepoUrl(new URI("/gitroot/sample/gerrit.other-project"));
    vr.setCommits(commits);
    vr.setStrictMode(true);
    // test output w/ assertions
    // Should be valid as grunter used a no-reply Github account and has a matching GH handle
    given().body(vr).contentType(ContentType.JSON).when().post("/eca").then().statusCode(200);
  }
}
