/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.dicom.DicomForwardDestination;
import org.karnak.backend.dicom.ForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.karnak.backend.dicom.WebForwardDestination;
import org.karnak.backend.model.echo.DestinationEcho;
import org.karnak.backend.service.gateway.GatewaySetUpService;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.weasis.dicom.op.Echo;
import org.weasis.dicom.param.AdvancedParams;
import org.weasis.dicom.param.DicomNode;
import org.weasis.dicom.param.DicomState;

class EchoServiceTest {

	// Service
	private EchoService echoService;

	private final GatewaySetUpService gatewaySetUpServiceMock = Mockito.mock(GatewaySetUpService.class);

	@BeforeEach
	public void setUp() {
		// Build mocked service
		echoService = new EchoService(gatewaySetUpServiceMock);
	}

	@Test
	void should_retrieve_status_destinations() throws IOException {
		// Init data
		ForwardDicomNode forwardDicomNode = new ForwardDicomNode("fwdAeTitle");
		Optional<ForwardDicomNode> forwardDicomNodeOpt = Optional.of(forwardDicomNode);
		DicomNode dicomNode = new DicomNode("fwdAeTitle", 1111);
		ForwardDestination forwardDestination = new DicomForwardDestination(forwardDicomNode, dicomNode);
		WebForwardDestination webForwardDestination = new WebForwardDestination(forwardDicomNode, "http://test.com");
		List<ForwardDestination> forwardDestinations = Arrays.asList(forwardDestination, webForwardDestination);
		DicomState dicomState = new DicomState();
		dicomState.setStatus(444);

		// Mock
		Mockito.when(gatewaySetUpServiceMock.getDestinationNode(Mockito.anyString())).thenReturn(forwardDicomNodeOpt);
		Mockito.when(gatewaySetUpServiceMock.getDestinations(Mockito.any(ForwardDicomNode.class)))
			.thenReturn(forwardDestinations);
		MockedStatic<Echo> echoMock = Mockito.mockStatic(Echo.class);
		echoMock.when(() -> Echo.process(Mockito.any(AdvancedParams.class), Mockito.any(ForwardDicomNode.class),
				Mockito.any(DicomNode.class)))
			.thenReturn(dicomState);

		// Call service
		List<DestinationEcho> destinationEchos = echoService.retrieveStatusConfiguredDestinations("fwdAeTitle");

		// Test results
		Mockito.verify(gatewaySetUpServiceMock, Mockito.times(1)).getDestinationNode(Mockito.anyString());
		assertEquals(2, destinationEchos.size());
		assertEquals("fwdAeTitle", destinationEchos.get(0).getAet());
		assertEquals(444, destinationEchos.get(0).getStatus());
		assertEquals("http://test.com/studies", destinationEchos.get(1).getUrl());
		assertEquals(0, destinationEchos.get(1).getStatus());
	}

}
