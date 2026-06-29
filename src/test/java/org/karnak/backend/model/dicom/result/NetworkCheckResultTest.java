/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class NetworkCheckResultTest {

	@Test
	void reachable_host_is_successful_and_reports_open_port() {
		NetworkCheckResult result = NetworkCheckResult.builder()
			.hostname("pacs")
			.hostAddress("10.0.0.1")
			.port(104)
			.hostnameReachable(true)
			.portOpen(true)
			.build();

		assertTrue(result.isSuccessful());
		assertEquals("pacs/10.0.0.1 machine is turned on and can be pinged", result.getCheckHostnameMessage());
		assertEquals("pacs is listening on port 104", result.getCheckPortMessage());
	}

	@Test
	void unresolved_host_reports_resolution_failure() {
		NetworkCheckResult result = NetworkCheckResult.builder()
			.hostname("pacs")
			.hostAddress("10.0.0.1")
			.port(104)
			.unresolvedHostname(true)
			.build();

		assertFalse(result.isSuccessful());
		assertEquals("10.0.0.1/pacs host address and host name are equal, meaning the host name could not be resolved",
				result.getCheckHostnameMessage());
		assertEquals("pacs is not listening on port 104", result.getCheckPortMessage());
	}

	@Test
	void unreachable_host_reports_dns_known_but_not_pingable() {
		NetworkCheckResult result = NetworkCheckResult.builder()
			.hostname("pacs")
			.hostAddress("10.0.0.1")
			.port(104)
			.build();

		assertEquals("pacs/10.0.0.1 machine is known in a DNS lookup but cannot be pinged",
				result.getCheckHostnameMessage());
	}

	@Test
	void unexpected_error_is_reported_in_hostname_message() {
		NetworkCheckResult result = NetworkCheckResult.builder()
			.hostname("pacs")
			.port(104)
			.unexpectedError(true)
			.unexpectedErrorMessage("Unknown Host")
			.build();

		assertEquals("Unexpected error: Unknown Host", result.getCheckHostnameMessage());
	}

	@Test
	void connection_quality_message_reports_latency_and_loss() {
		NetworkCheckResult result = NetworkCheckResult.builder()
			.hostname("pacs")
			.port(104)
			.portOpen(true)
			.connectionLatency(new ConnectionLatency(4, 3, 2, 5, 9))
			.build();

		assertEquals("Connection time min/avg/max 2/5/9 ms over 4 attempt(s), 25% loss",
				result.getCheckQualityMessage());
	}

	@Test
	void connection_quality_message_is_null_without_a_sample() {
		NetworkCheckResult result = NetworkCheckResult.builder().hostname("pacs").port(104).build();

		assertNull(result.getCheckQualityMessage());
	}

}