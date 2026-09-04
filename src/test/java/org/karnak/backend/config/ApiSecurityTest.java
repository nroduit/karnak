/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.config;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.Filter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.constant.EndPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Guards the security contract of the REST API filter chain: it authenticates every
 * request on its own, keeps no session, and therefore runs without CSRF tokens. Runs
 * against the in-memory identity provider, which is the one active in the tests.
 */
@SpringBootTest
@DisplayNameGeneration(ReplaceUnderscores.class)
class ApiSecurityTest {

	private static final String ECHO_DESTINATIONS = EndPoint.ECHO_PATH + EndPoint.DESTINATIONS_PATH;

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	@Qualifier("springSecurityFilterChain")
	private Filter springSecurityFilterChain;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
			.addFilters(springSecurityFilterChain)
			.build();
	}

	/**
	 * The API answers a 401 challenge instead of the redirect to the Vaadin login page
	 * that the UI chain would produce, which is what tells an API client to send
	 * credentials.
	 */
	@Test
	void unauthenticated_api_request_is_challenged() throws Exception {
		mockMvc.perform(get(EndPoint.FORWARD_NODES_PATH)).andExpect(status().isUnauthorized());
	}

	@Test
	void echo_destinations_stays_open_without_credentials() throws Exception {
		MvcResult result = mockMvc.perform(get(ECHO_DESTINATIONS).param(EndPoint.SRC_AET_PARAM, "TEST-AET"))
			.andReturn();

		assertNotEquals(HttpStatus.UNAUTHORIZED.value(), result.getResponse().getStatus());
	}

	/**
	 * An authenticated write without any CSRF token must reach the controller - a 403
	 * here would mean CSRF protection is back on the API and every non-browser client is
	 * broken. The 400 is the bean validation of the payload, i.e. the request went
	 * through the whole security chain.
	 */
	@Test
	void authenticated_write_needs_no_csrf_token() throws Exception {
		mockMvc
			.perform(post(EndPoint.FORWARD_NODES_PATH).header(HttpHeaders.AUTHORIZATION, basicAdmin())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"fwdAeTitle\":\"BAD\\nAET\"}"))
			.andExpect(status().isBadRequest());
	}

	/**
	 * The AETitle reaches the DICOM gateway and its logs; a control character in it would
	 * let a crafted payload forge log records.
	 */
	@Test
	void aetitle_with_a_control_character_is_rejected() throws Exception {
		mockMvc
			.perform(post(EndPoint.FORWARD_NODES_PATH).header(HttpHeaders.AUTHORIZATION, basicAdmin())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"fwdAeTitle\":\"A\\r\\nINFO forged\"}"))
			.andExpect(status().isBadRequest());
	}

	/**
	 * The update is a partial payload and so cannot be {@code @Valid}; it must still hold
	 * the fields it does carry to the constraints of the creation endpoint. A 404 here
	 * would mean the payload was accepted and only the unknown uuid stopped it.
	 */
	@Test
	void updated_aetitle_with_a_control_character_is_rejected() throws Exception {
		mockMvc
			.perform(put(EndPoint.FORWARD_NODES_PATH + "/" + UUID.randomUUID())
				.header(HttpHeaders.AUTHORIZATION, basicAdmin())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"fwdAeTitle\":\"A\\r\\nINFO forged\"}"))
			.andExpect(status().isBadRequest());
	}

	/** No session is created, so there is no ambient credential to replay cross-site. */
	@Test
	void api_request_creates_no_session() throws Exception {
		MvcResult result = mockMvc
			.perform(get(EndPoint.FORWARD_NODES_PATH).header(HttpHeaders.AUTHORIZATION, basicAdmin()))
			.andReturn();

		assertNull(result.getRequest().getSession(false));
	}

	private static String basicAdmin() {
		String credentials = AppConfig.getInstance().getKarnakAdmin() + ":"
				+ AppConfig.getInstance().getKarnakPassword();
		return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
	}

}
