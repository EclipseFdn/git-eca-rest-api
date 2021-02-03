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

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipsefoundation.git.eca.model.BotUser;

/**
 * Interface for interacting with the Eclipse Foundation Bots API.
 * 
 * @author Martin Lowe
 *
 */
@Path("/bots")
@RegisterRestClient
public interface BotsAPI {
	
	/**
	 * Retrieves all bot users from the endpoint.
	 * 
	 * @return list of active bot users
	 */
	@GET
	@Produces("application/json")
	List<BotUser> getBots();
}
