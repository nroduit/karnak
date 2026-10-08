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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.result.DicomEchoResult;
import org.karnak.backend.model.dicom.result.DicomNodeCheckResult;
import org.karnak.backend.model.dicom.result.NetworkCheckResult;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.weasis.dicom.param.DicomNode;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
class DicomNodeCheckServiceTest {

	@Mock
	private DicomEchoCheckService dicomEchoCheckService;

	@Mock
	private NetworkCheckService networkCheckService;

	private DicomNodeCheckService service;

	@BeforeEach
	void setUp() {
		service = new DicomNodeCheckService(dicomEchoCheckService, networkCheckService);
	}

	private static ConfigNode node(String aet, String hostname, int port) {
		return new ConfigNode(aet, new DicomNode(aet, hostname, port));
	}

	private static DicomEchoResult echoResult() {
		return DicomEchoResult.builder().build();
	}

	private static NetworkCheckResult networkResult(String hostname, int port) {
		return NetworkCheckResult.builder()
			.hostname(hostname)
			.port(port)
			.hostnameReachable(true)
			.portOpen(true)
			.build();
	}

	@Test
	void single_node_composes_echo_and_network_results() {
		ConfigNode node = node("AET", "host", 104);
		DicomEchoResult echo = echoResult();
		NetworkCheckResult network = networkResult("host", 104);
		when(dicomEchoCheckService.echo("CALL", node)).thenReturn(echo);
		when(networkCheckService.check("host", 104)).thenReturn(network);

		DicomNodeCheckResult result = service.check("CALL", node);

		assertEquals("CALL", result.getCallingAET());
		assertSame(node, result.getCalledNode());
		assertSame(echo, result.getDicomEchoResult());
		assertSame(network, result.getNetworkCheckResult());
	}

	@Test
	void multi_node_checks_every_node_and_preserves_order() {
		ConfigNode n1 = node("AET1", "h1", 104);
		ConfigNode n2 = node("AET2", "h2", 105);
		ConfigNode n3 = node("AET3", "h3", 106);
		when(dicomEchoCheckService.echo(eq("CALL"), any(ConfigNode.class))).thenReturn(echoResult());
		when(networkCheckService.check(anyString(), anyInt())).thenReturn(networkResult("h", 1));

		List<DicomNodeCheckResult> results = service.check("CALL", List.of(n1, n2, n3));

		assertEquals(3, results.size());
		assertSame(n1, results.get(0).getCalledNode());
		assertSame(n2, results.get(1).getCalledNode());
		assertSame(n3, results.get(2).getCalledNode());
	}

	@Test
	void empty_node_list_returns_empty() {
		assertTrue(service.check("CALL", List.of()).isEmpty());
	}

	@Test
	void runs_echo_and_network_concurrently_for_a_single_node() {
		ConfigNode node = node("AET", "host", 104);
		long delayMs = 300L;
		when(dicomEchoCheckService.echo("CALL", node)).thenAnswer((invocation) -> {
			Thread.sleep(delayMs);
			return echoResult();
		});
		when(networkCheckService.check("host", 104)).thenAnswer((invocation) -> {
			Thread.sleep(delayMs);
			return networkResult("host", 104);
		});

		long start = System.nanoTime();
		service.check("CALL", node);
		long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

		// Run sequentially this would take ~2 * delayMs; concurrently it is ~delayMs.
		assertTrue(elapsedMs < 2 * delayMs, "Echo and network check should run concurrently but took " + elapsedMs
				+ "ms (sequential would be ~" + (2 * delayMs) + "ms)");
	}

}