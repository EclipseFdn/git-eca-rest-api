/*******************************************************************************
 * Copyright (C) 2020 Eclipse Foundation
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/
package org.eclipsefoundation.git.eca.helper;

import java.util.ArrayList;

import org.eclipsefoundation.git.eca.model.Commit;
import org.eclipsefoundation.git.eca.model.GitUser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests related to the {@linkplain CommitHelper} class.
 * 
 * @author Martin Lowe
 *
 */
@QuarkusTest
class CommitHelperTest {

	// represents a known good commit before the start of each test
	GitUser testUser;
	Commit baseCommit;

	@BeforeEach
	void setup() {
		// basic good user
		testUser = new GitUser();
		testUser.setMail("test.user@eclipse-foundation.org");
		testUser.setName("Tester McTesterson");

		// basic known good commit
		baseCommit = new Commit();
		baseCommit.setBody(
				String.format("Sample body content\n\nSigned-off-by: %s <%s>", testUser.getName(), testUser.getMail()));
		baseCommit.setHash("abc123f");
		baseCommit.setHead(false);
		baseCommit.setParents(new ArrayList<>());
		baseCommit.setSubject("Testing CommitHelper class #1337");
		baseCommit.setAuthor(testUser);
		baseCommit.setCommitter(testUser);
	}

	@Test
	void getSignedOffByEmailNullCommit() {
		Assertions.assertFalse(CommitHelper.getSignedOffByEmail(null), "Expected null return for null commit");
	}

	@Test
	void getSignedOffByEmailOnlyFooter() {
		baseCommit.setBody(String.format("Signed-off-by: %s <%s>", testUser.getName(), testUser.getMail()));
		boolean actual = CommitHelper.getSignedOffByEmail(baseCommit);
		Assertions.assertTrue(actual);
	}
	@Test
	void getSignedOffByEmailWithDifferentCasing() {
		baseCommit.setBody(String.format("Signed-off-by: %s <%s>", testUser.getName(), testUser.getMail().toUpperCase()));
		boolean actual = CommitHelper.getSignedOffByEmail(baseCommit);
		Assertions.assertTrue(actual);
	}

	@Test
	void getSignedOffByEmailBodyAndFooter() {
		baseCommit.setBody(
				String.format("Sample body content\n\nSigned-off-by: %s <%s>", testUser.getName(), testUser.getMail()));
		boolean actualMail = CommitHelper.getSignedOffByEmail(baseCommit);
		Assertions.assertTrue(actualMail);
	}

	@Test
	void getSignedOffByEmailNoNameFooter() {
		baseCommit.setBody(
				String.format("Sample body content\n\nSigned-off-by:<%s>", testUser.getMail()));
		boolean actual = CommitHelper.getSignedOffByEmail(baseCommit);
		Assertions.assertTrue(actual);
	}

	@Test
	void getSignedOffByEmailNoBrackets() {
		baseCommit.setBody(
				String.format("Sample body content\n\nSigned-off-by:%s", testUser.getMail()));
		boolean actual = CommitHelper.getSignedOffByEmail(baseCommit);
		Assertions.assertFalse(actual);
	}

	@Test
	void getSignedOffByEmailBadFooterName() {
		baseCommit.setBody(
				String.format("Sample body content\n\nSign-off-by: %s <%s>", testUser.getName(), testUser.getMail()));
		Assertions.assertFalse(CommitHelper.getSignedOffByEmail(baseCommit), "Expected false with typo in footer name");
		
		baseCommit.setBody(
				String.format("Sample body content\n\nSIGNED-OFF-BY: %s <%s>", testUser.getName(), testUser.getMail()));
		Assertions.assertFalse(CommitHelper.getSignedOffByEmail(baseCommit), "Expected false with bad casing");
	}

	@Test
	void getSignedOffByEmailBadAuthorEmail() {
		baseCommit.setBody(
				String.format("Sample body content\n\nSigned-off-by: %s <%s>", testUser.getName(), "known_bad@email.org"));
		Assertions.assertFalse(CommitHelper.getSignedOffByEmail(baseCommit), "Expected false with typo in footer name");
	}

	@Test
	void validateCommitKnownGood() {
		Assertions.assertTrue(CommitHelper.validateCommit(baseCommit), "Expected basic commit to pass validation");
	}
	
	@Test
	void validateCommitNullCommit() {
		Assertions.assertFalse(CommitHelper.validateCommit(null), "Expected null commit to fail validation");
	}

	@Test
	void validateCommitNoAuthor() {
		baseCommit.setAuthor(null);
		Assertions.assertFalse(CommitHelper.validateCommit(baseCommit),
				"Expected basic commit to fail validation w/ no author");
	}

	@Test
	void validateCommitNoAuthorMail() {
		GitUser noMail = new GitUser();
		noMail.setName("Some Name");

		baseCommit.setAuthor(noMail);
		Assertions.assertFalse(CommitHelper.validateCommit(baseCommit),
				"Expected basic commit to fail validation w/ no author mail address");
	}

	@Test
	void validateCommitNoCommitter() {
		baseCommit.setCommitter(null);
		Assertions.assertFalse(CommitHelper.validateCommit(baseCommit),
				"Expected basic commit to fail validation w/ no committer");
	}

	@Test
	void validateCommitNoCommitterMail() {
		GitUser noMail = new GitUser();
		noMail.setName("Some Name");

		baseCommit.setCommitter(noMail);
		Assertions.assertFalse(CommitHelper.validateCommit(baseCommit),
				"Expected basic commit to fail validation w/ no committer mail address");
	}

	@Test
	void validateCommitNoHash() {
		baseCommit.setHash(null);
		Assertions.assertFalse(CommitHelper.validateCommit(baseCommit),
				"Expected basic commit to fail validation w/ no commit hash");
	}

	@Test
	void validateCommitNoBody() {
		baseCommit.setBody(null);
		Assertions.assertTrue(CommitHelper.validateCommit(baseCommit),
				"Expected basic commit to pass validation w/ no body");
	}

	@Test
	void validateCommitNoParents() {
		baseCommit.setParents(new ArrayList<>());
		Assertions.assertTrue(CommitHelper.validateCommit(baseCommit),
				"Expected basic commit to pass validation w/ no parents");
	}

	@Test
	void validateCommitNoSubject() {
		baseCommit.setSubject(null);
		Assertions.assertTrue(CommitHelper.validateCommit(baseCommit),
				"Expected basic commit to pass validation w/ no subject");
	}
}
