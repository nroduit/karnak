/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DicomNodeCheckHistoryEntityTest {

	@Test
	void builder_populates_the_flattened_check_outcome() {
		Instant checkedAt = Instant.parse("2026-03-04T05:06:07Z");

		DicomNodeCheckHistoryEntity entity = DicomNodeCheckHistoryEntity.builder()
			.id(3L)
			.checkedAt(checkedAt)
			.callingAeTitle("KARNAK")
			.calledDescription("PACS")
			.calledAeTitle("DCM4CHEE")
			.calledHostname("pacs.example.org")
			.calledPort(11112)
			.echoSuccessful(true)
			.echoStatusHex("0000")
			.connectionMs(5L)
			.executionMs(20L)
			.networkReachable(true)
			.portOpen(true)
			.build();

		assertEquals(3L, entity.getId());
		assertEquals(checkedAt, entity.getCheckedAt());
		assertEquals("KARNAK", entity.getCallingAeTitle());
		assertEquals("DCM4CHEE", entity.getCalledAeTitle());
		assertEquals("pacs.example.org", entity.getCalledHostname());
		assertEquals(11112, entity.getCalledPort());
		assertTrue(entity.isEchoSuccessful());
		assertEquals("0000", entity.getEchoStatusHex());
		assertEquals(5L, entity.getConnectionMs());
		assertEquals(20L, entity.getExecutionMs());
		assertTrue(entity.isNetworkReachable());
		assertTrue(entity.isPortOpen());
	}

	@Test
	void no_args_constructor_leaves_a_blank_row() {
		DicomNodeCheckHistoryEntity entity = new DicomNodeCheckHistoryEntity();

		assertNull(entity.getId());
		assertNull(entity.getCheckedAt());
		assertFalse(entity.isEchoSuccessful());
		assertFalse(entity.isNetworkReachable());
	}

	@Test
	void setters_update_the_failure_details() {
		DicomNodeCheckHistoryEntity entity = new DicomNodeCheckHistoryEntity();
		entity.setEchoSuccessful(false);
		entity.setEchoErrorMessage("timeout");
		entity.setEchoRejectionReason("rejected");
		entity.setNetworkReachable(false);
		entity.setPortOpen(false);
		entity.setNetworkPortMessage("port closed");

		assertFalse(entity.isEchoSuccessful());
		assertEquals("timeout", entity.getEchoErrorMessage());
		assertEquals("rejected", entity.getEchoRejectionReason());
		assertEquals("port closed", entity.getNetworkPortMessage());
	}

}