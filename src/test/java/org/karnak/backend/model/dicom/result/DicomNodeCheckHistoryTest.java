/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom.result;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DicomNodeCheckHistoryTest {

	@Test
	void builder_populates_the_fields_and_derives_the_network_details() {
		Instant checkedAt = Instant.parse("2026-01-02T03:04:05Z");

		DicomNodeCheckHistory history = DicomNodeCheckHistory.builder()
			.checkedAt(checkedAt)
			.callingAeTitle("KARNAK")
			.calledDescription("PACS")
			.calledAeTitle("DCM4CHEE")
			.calledHostname("pacs.example.org")
			.calledPort(11112)
			.echoSuccessful(true)
			.echoStatusHex("0000")
			.connectionMs(12L)
			.executionMs(34L)
			.networkReachable(true)
			.portOpen(true)
			.build();

		assertEquals(checkedAt, history.getCheckedAt());
		assertEquals("KARNAK", history.getCallingAeTitle());
		assertEquals("DCM4CHEE", history.getCalledAeTitle());
		assertEquals(11112, history.getCalledPort());
		assertTrue(history.isEchoSuccessful());
		assertTrue(history.isNetworkReachable());
		assertTrue(history.isPortOpen());
		assertEquals(12L, history.getConnectionMs());
		assertEquals(34L, history.getExecutionMs());
		assertEquals("DCM4CHEE pacs.example.org 11112", history.getCalledNetworkDetails());
	}

}