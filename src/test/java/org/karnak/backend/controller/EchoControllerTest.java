/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.controller;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.karnak.backend.model.echo.DestinationEcho;
import org.karnak.backend.service.EchoService;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Unit test for the MVC controllers Tests for EchoController
 */
@SpringJUnitWebConfig(classes = EchoService.class)
class EchoControllerTest {

	private MockMvc mockMvc;

	@Mock
	private EchoService echoServiceMock;

	/**
	 * Init mock mvc
	 * @param wac WebApplicationContext
	 */
	@BeforeEach
	public void setUp(WebApplicationContext wac) {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
	}

	/**
	 * Test Get Status destinations: case data found
	 *
	 * <p>
	 * Expected: - mocked DestinationEcho have been created and returned from the service
	 * retrieveStatusConfiguredDestinations - status is OK - response body has the correct
	 * values and format
	 * @throws Exception thrown
	 */
	// @Test
	void when_data_found_should_retrieve_status_destinations() throws Exception {
		// Init data
		List<DestinationEcho> destinationEchos = new ArrayList<>();
		DestinationEcho destinationEchoDicom = new DestinationEcho();
		destinationEchoDicom.setAet("aet");
		destinationEchoDicom.setStatus(111);
		destinationEchoDicom.setUrl(null);
		DestinationEcho destinationEchoStow = new DestinationEcho();
		destinationEchoStow.setAet(null);
		destinationEchoStow.setStatus(222);
		destinationEchoStow.setUrl("http://test.com");
		destinationEchos.add(destinationEchoDicom);
		destinationEchos.add(destinationEchoStow);

		// Mock service to return targets
		Mockito.when(echoServiceMock.retrieveStatusConfiguredDestinations(Mockito.anyString()))
			.thenReturn(destinationEchos);

		// Call service and test results
		this.mockMvc.perform(MockMvcRequestBuilders.get("/api/echo/destinations").param("srcAet", "aet"))
			.andExpect(MockMvcResultMatchers.status().isOk())
			.andExpect(MockMvcResultMatchers.content()
				.string("<destinations><destination><aet>aet</aet><status>111</status></destination><destination><url>http://test.com</url><status>222</status></destination></destinations>"));
	}

	/**
	 * Test Get Status destinations: case no data found
	 *
	 * <p>
	 * Expected: - the service retrieveStatusConfiguredDestinations return empty list -
	 * status is no content
	 * @throws Exception thrown
	 */
	// @Test
	void when_no_data_found_should_respond_no_content() throws Exception {
		// Mock service to return targets
		Mockito.when(echoServiceMock.retrieveStatusConfiguredDestinations(Mockito.anyString()))
			.thenReturn(new ArrayList<>());

		// Call service and test results
		this.mockMvc.perform(MockMvcRequestBuilders.get("/api/echo/destinations").param("srcAet", "aet"))
			.andExpect(MockMvcResultMatchers.status().isNoContent());
	}

}
