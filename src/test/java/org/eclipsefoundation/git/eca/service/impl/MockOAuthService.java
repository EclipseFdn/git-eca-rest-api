package org.eclipsefoundation.git.eca.service.impl;

import org.eclipsefoundation.git.eca.service.OAuthService;

import io.quarkus.test.Mock;

/**
 * Disable the OAuth service while in testing via a mock service. This will
 * never authenticate, but since all external data is mocked, this does not
 * impact testing.
 * 
 * @author Martin Lowe
 *
 */
@Mock
public class MockOAuthService implements OAuthService {

	@Override
	public String getToken() {
		// return an empty (invalid) token every time
		return "";
	}

}
