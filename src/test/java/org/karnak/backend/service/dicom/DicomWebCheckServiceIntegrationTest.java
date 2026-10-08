/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.dicom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.enums.DicomWebServiceType;
import org.karnak.backend.model.dicom.WebDestinationNode;
import org.karnak.backend.model.dicom.result.AuthCheckResult;
import org.karnak.backend.model.dicom.result.WebDestinationCheckResult;
import org.karnak.backend.model.dicom.result.WebNodeCheckResult;
import org.karnak.backend.model.dicom.result.WebServiceProbe;

/**
 * Drives {@link DicomWebCheckService} against a real in-process HTTP server to exercise
 * the TCP reach and HTTP-status probe of the DICOMweb reachability check.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class DicomWebCheckServiceIntegrationTest {

	private HttpServer server;

	private int port;

	@BeforeEach
	void start_server() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		port = server.getAddress().getPort();
		server.createContext("/dicom-web", (exchange) -> {
			// Mimic a DICOMweb endpoint that requires authentication.
			exchange.sendResponseHeaders(401, -1);
			exchange.close();
		});
		server.createContext("/stow-ok", (exchange) -> {
			// Mimic a STOW-RS /studies resource advertising its methods on OPTIONS.
			exchange.getResponseHeaders().add("Allow", "GET,POST,OPTIONS");
			exchange.getResponseHeaders().add("Accept-Post", "application/dicom, multipart/related");
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
		server.createContext("/caps/capabilities", (exchange) -> {
			// Mimic a server publishing a Capabilities-RS document.
			byte[] body = "{\"studies\":{},\"series\":{},\"metadata\":{}}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
	}

	@AfterEach
	void stop_server() {
		if (server != null) {
			server.stop(0);
		}
	}

	private static int freePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	@Test
	void reachable_http_endpoint_reports_status_and_no_tls() {
		DicomWebCheckService service = new DicomWebCheckService(3000);

		WebDestinationCheckResult result = service.check("http://127.0.0.1:" + port + "/dicom-web");

		assertTrue(result.isTcpReachable());
		assertTrue(result.isHttpResponded());
		assertEquals(401, result.getHttpStatus());
		assertFalse(result.isSecure());
		assertNull(result.getTls());
		// An auth-required endpoint is still reachable.
		assertTrue(result.isSuccessful());
	}

	@Test
	void unreachable_endpoint_is_reported() throws IOException {
		int closedPort = freePort();
		DicomWebCheckService service = new DicomWebCheckService(1000);

		WebDestinationCheckResult result = service.check("http://127.0.0.1:" + closedPort + "/dicom-web");

		assertFalse(result.isTcpReachable());
		assertFalse(result.isHttpResponded());
		assertFalse(result.isSuccessful());
	}

	@Test
	void invalid_url_is_reported_as_error() {
		DicomWebCheckService service = new DicomWebCheckService(1000);

		WebDestinationCheckResult result = service.check("http://");

		assertTrue(result.isUnexpectedError());
		assertFalse(result.isSuccessful());
	}

	@Test
	void multiple_destinations_are_checked_in_order() throws IOException {
		int closedPort = freePort();
		DicomWebCheckService service = new DicomWebCheckService(1000);

		WebDestinationNode reachable = new WebDestinationNode("PACS web", "http://127.0.0.1:" + port + "/dicom-web");
		WebDestinationNode unreachable = new WebDestinationNode("Dead web",
				"http://127.0.0.1:" + closedPort + "/dicom-web");

		List<WebNodeCheckResult> results = service.check(List.of(reachable, unreachable));

		assertEquals(2, results.size());
		assertSame(reachable, results.get(0).node());
		assertTrue(results.get(0).result().isHttpResponded());
		assertSame(unreachable, results.get(1).node());
		assertFalse(results.get(1).result().isTcpReachable());
	}

	@Test
	void empty_destination_list_returns_empty() {
		assertTrue(new DicomWebCheckService(1000).check(List.of()).isEmpty());
	}

	@Test
	void auth_token_result_is_attached_for_destinations_with_auth_config() {
		WebTokenService tokenService = mock(WebTokenService.class);
		when(tokenService.authorize("idp"))
			.thenReturn(new WebTokenService.TokenResult(new AuthCheckResult("idp", true, null), "token-123"));
		DicomWebCheckService service = new DicomWebCheckService(1000, tokenService);

		WebDestinationNode node = new WebDestinationNode("PACS web", "http://127.0.0.1:" + port + "/dicom-web", "idp");
		WebDestinationCheckResult result = service.check(node);

		assertTrue(result.isHttpResponded());
		assertNotNull(result.getAuth());
		assertTrue(result.getAuth().acquired());
	}

	@Test
	void stow_service_probe_reports_supported_from_options() {
		DicomWebCheckService service = new DicomWebCheckService(1000);

		WebDestinationNode node = new WebDestinationNode("PACS web", "http://127.0.0.1:" + port + "/stow-ok");
		WebDestinationCheckResult result = service.check(node, EnumSet.of(DicomWebServiceType.STOW_RS));

		assertEquals(1, result.getServiceProbes().size());
		WebServiceProbe stow = result.getServiceProbes().get(0);
		assertEquals(DicomWebServiceType.STOW_RS, stow.type());
		assertEquals(WebServiceProbe.Support.SUPPORTED, stow.support());
		assertTrue(stow.detail().contains("POST"));
	}

	@Test
	void capabilities_probe_reports_published_document_and_declared_resources() {
		DicomWebCheckService service = new DicomWebCheckService(1000);

		WebDestinationNode node = new WebDestinationNode("PACS web", "http://127.0.0.1:" + port + "/caps");
		WebDestinationCheckResult result = service.check(node, EnumSet.of(DicomWebServiceType.CAPABILITIES));

		assertEquals(1, result.getServiceProbes().size());
		WebServiceProbe capabilities = result.getServiceProbes().get(0);
		assertEquals(DicomWebServiceType.CAPABILITIES, capabilities.type());
		assertEquals(WebServiceProbe.Support.SUPPORTED, capabilities.support());
		assertTrue(capabilities.detail().contains("studies"));
	}

	@Test
	void capabilities_probe_reports_unsupported_when_absent() {
		DicomWebCheckService service = new DicomWebCheckService(1000);

		WebDestinationNode node = new WebDestinationNode("PACS web", "http://127.0.0.1:" + port + "/missing");
		WebDestinationCheckResult result = service.check(node, EnumSet.of(DicomWebServiceType.CAPABILITIES));

		WebServiceProbe capabilities = result.getServiceProbes().get(0);
		assertEquals(WebServiceProbe.Support.UNSUPPORTED, capabilities.support());
	}

	@Test
	void ups_rs_service_probe_reports_supported_from_query() {
		DicomWebCheckService service = new DicomWebCheckService(1000);

		WebDestinationNode node = new WebDestinationNode("PACS web", "http://127.0.0.1:" + port + "/stow-ok");
		WebDestinationCheckResult result = service.check(node, EnumSet.of(DicomWebServiceType.UPS_RS));

		assertEquals(1, result.getServiceProbes().size());
		WebServiceProbe ups = result.getServiceProbes().get(0);
		assertEquals(DicomWebServiceType.UPS_RS, ups.type());
		assertEquals(WebServiceProbe.Support.SUPPORTED, ups.support());
	}

	@Test
	void default_check_probes_all_base_url_services() {
		DicomWebCheckService service = new DicomWebCheckService(1000);

		WebDestinationNode node = new WebDestinationNode("PACS web", "http://127.0.0.1:" + port + "/stow-ok");
		WebDestinationCheckResult result = service.check(node);

		Set<DicomWebServiceType> probed = result.getServiceProbes()
			.stream()
			.map(WebServiceProbe::type)
			.collect(Collectors.toSet());
		assertEquals(EnumSet.of(DicomWebServiceType.STOW_RS, DicomWebServiceType.QIDO_RS, DicomWebServiceType.WADO_RS,
				DicomWebServiceType.UPS_RS, DicomWebServiceType.CAPABILITIES), probed);
	}

	@Test
	void no_auth_check_when_destination_has_no_auth_config() {
		WebTokenService tokenService = mock(WebTokenService.class);
		DicomWebCheckService service = new DicomWebCheckService(1000, tokenService);

		WebDestinationNode node = new WebDestinationNode("PACS web", "http://127.0.0.1:" + port + "/dicom-web");
		WebDestinationCheckResult result = service.check(node);

		assertNull(result.getAuth());
		verifyNoInteractions(tokenService);
	}

}
