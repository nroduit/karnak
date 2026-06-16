/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.dicom.WadoNode;
import org.weasis.dicom.param.DicomNode;

class UtilTest {

	@Test
	void when_network_response_format_xml_host_not_reachable_should_add_correct_tags() {

		// Init data
		DicomNode dicomNode = new DicomNode("fwdAeTitle", 1111);
		StringBuilder result = new StringBuilder();

		// Call method
		Util.getNetworkResponse(result, dicomNode.getAet(), dicomNode.getHostname(), dicomNode.getPort(), true, "XML");

		// Test results
		assertNotNull(result);
		assertTrue(result.toString().contains("<DcmNetworkStatus>"));
		assertTrue(result.toString().contains("</DcmNetworkStatus>"));
	}

	// @Test
	// void when_echo_response_format_xml_host_not_reachable_should_add_correct_tags() {
	//
	// // Init data
	// DicomNode dicomNode = new DicomNode("fwdAeTitle", 1111);
	// StringBuilder result = new StringBuilder();
	//
	// DicomState dicomState = new DicomState();
	// dicomState.setStatus(444);
	//
	// try (MockedStatic<Echo> echoMock = Mockito.mockStatic(Echo.class)) {
	// echoMock
	// .when(
	// () ->
	// Echo.process(
	// Mockito.any(AdvancedParams.class),
	// Mockito.any(DicomNode.class),
	// Mockito.any(DicomNode.class)))
	// .thenReturn(dicomState);
	//
	// // Call method
	// Util.getEchoResponse(result, dicomNode.getAet(), dicomNode, true, "XML", 0);
	//
	// // Test results
	// Assert.assertNotNull(result);
	// String resultString = result.toString();
	// Assert.assertTrue(resultString.contains("<DcmStatus>"));
	// Assert.assertTrue(resultString.contains("</DcmStatus>"));
	// Assert.assertTrue(resultString.contains("Error"));
	// }
	// }

	// @Test
	// void when_echo_response_format_xml_host_reachable_should_add_correct_tags() {
	//
	// // Init data
	// DicomNode dicomNode = new DicomNode("fwdAeTitle", 1111);
	// StringBuilder result = new StringBuilder();
	// DicomState dicomState = new DicomState();
	// dicomState.setStatus(0);
	//
	// try (MockedStatic<Echo> echoMock = Mockito.mockStatic(Echo.class)) {
	// // Mock
	// echoMock
	// .when(
	// () ->
	// Echo.process(
	// Mockito.any(AdvancedParams.class),
	// Mockito.any(DicomNode.class),
	// Mockito.any(DicomNode.class)))
	// .thenReturn(dicomState);
	//
	// // Call method
	// Util.getEchoResponse(result, dicomNode.getAet(), dicomNode, true, "XML", 0);
	//
	// // Test results
	// Assert.assertNotNull(result);
	// String resultString = result.toString();
	// Assert.assertTrue(resultString.contains("<DcmStatus>"));
	// Assert.assertTrue(resultString.contains("</DcmStatus>"));
	// Assert.assertTrue(resultString.contains("Success"));
	// }
	// }

	@Test
	void when_wado_response_format_xml_host_not_reachable_should_add_correct_tags() throws MalformedURLException {

		// Init data
		WadoNode wadoNode = new WadoNode("fwdAeTitle", new URL("http://test.com"));
		StringBuilder result = new StringBuilder();

		// Call method
		Util.getWadoResponse(result, wadoNode, true, "XML");

		// Test results
		assertNotNull(result);
		assertTrue(result.toString().contains("<WadoStatus"));
		assertTrue(result.toString().contains("</WadoStatus>"));
	}

}
