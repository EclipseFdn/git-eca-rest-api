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
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.eclipsefoundation.git.eca.model.BotUser;
import org.eclipsefoundation.git.eca.model.BotUser.SiteSpecificBot;

import io.quarkus.test.Mock;

@Mock
@RestClient
@ApplicationScoped
public class MockBotsAPI implements BotsAPI {

	private List<BotUser> src;
	
	@PostConstruct
	public void build() {
		this.src = new ArrayList<>();
		BotUser b1 = new BotUser();
		b1.setId("1");
		b1.setEmail("1.bot@eclipse.org");
		b1.setProjectId("sample.proj");
		b1.setUsername("projbot");
		src.add(b1);
		
		BotUser b2 = new BotUser();
		b2.setId("10");
		b2.setEmail("2.bot@eclipse.org");
		b2.setProjectId("sample.proto");
		b2.setUsername("protobot");
		SiteSpecificBot ssbGH = new SiteSpecificBot();
		ssbGH.setEmail("2.bot-github@eclipse.org");
		ssbGH.setUsername("protobot-gh");
		b2.setGithubBot(ssbGH);
		src.add(b2);
		
		BotUser b3 = new BotUser();
		b3.setId("11");
		b3.setEmail("3.bot@eclipse.org");
		b3.setProjectId("spec.proj");
		b3.setUsername("specbot");
		SiteSpecificBot ssbGL = new SiteSpecificBot();
		ssbGL.setEmail("3.bot-gitlab@eclipse.org");
		ssbGL.setUsername("protobot-gl");
		b3.setGitlabBot(ssbGL);
		src.add(b3);
	}

	@Override
	public List<BotUser> getBots() {
		return new ArrayList<>(src);
	}
}
