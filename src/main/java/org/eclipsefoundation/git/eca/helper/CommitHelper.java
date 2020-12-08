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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipsefoundation.git.eca.model.Commit;

/**
 * Contains helpers for processing commits.
 *
 * @author Martin Lowe
 *
 */
public class CommitHelper {
	private static final Pattern SIGNED_OFF_BY_FOOTER = Pattern.compile("Signed-off-by:(.*)<(.*@.*)>");

	/**
	 * Validate the commits fields.
	 *
	 * @param c commit to validate
	 * @return true if valid, otherwise false
	 */
	public static boolean validateCommit(Commit c) {
		if (c == null) {
			return false;
		}

		boolean valid = true;
		// check current commit data
		if (c.getHash() == null) {
			valid = false;
		}
		// check author
		if (c.getAuthor() == null || c.getAuthor().getMail() == null) {
			valid = false;
		}
		// check committer
		if (c.getCommitter() == null || c.getCommitter().getMail() == null) {
			valid = false;
		}

		return valid;
	}

	/**
	 * Retrieves the email address associated with a commit message. This is done by
	 * processing the body and parsing out the given footer knowing its format.
	 *
	 * @param c the commit to retrieve the signed off by footer for
	 * @return the email address in the Signed-off-by footer, or null if none could
	 *         be found.
	 */
	public static String getSignedOffByEmail(Commit c) {
		if (c == null) {
			return null;
		}
		Matcher m = SIGNED_OFF_BY_FOOTER.matcher(c.getBody());
		if (m.find()) {
			return m.group(2);
		}
		return null;
	}

	private CommitHelper() {
	}
}
