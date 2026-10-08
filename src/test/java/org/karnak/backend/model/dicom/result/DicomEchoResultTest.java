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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import org.dcm4che3.net.Status;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.weasis.dicom.param.DicomState;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DicomEchoResultTest {

	@Test
	void successful_state_exposes_hex_status_and_durations() {
		DicomState state = mock(DicomState.class);
		lenient().when(state.getStatus()).thenReturn(Status.Success);
		lenient().when(state.getMessage()).thenReturn("OK");

		DicomEchoResult result = DicomEchoResult.builder()
			.dicomState(state)
			.connectionDuration(Duration.ofMillis(150))
			.executionDuration(Duration.ofMillis(40))
			.remoteImplementationVersionName("DCM4CHE_5")
			.build();

		assertTrue(result.isSuccessful());
		assertFalse(result.isRejected());
		assertEquals("0000", result.getDicomStatusInHex());
		assertEquals("OK", result.getDicomStatusMessage());
		assertEquals(150L, result.getConnectionDurationInMs());
		assertEquals(40L, result.getExecutionDurationInMs());
		assertEquals("DCM4CHE_5", result.getRemoteImplementationVersionName());
	}

	@Test
	void missing_state_is_not_successful_and_returns_null_status() {
		DicomEchoResult result = DicomEchoResult.builder().unexpectedError(true).unexpectedErrorMessage("boom").build();

		assertFalse(result.isSuccessful());
		assertNull(result.getDicomStatusInHex());
		assertNull(result.getDicomStatusMessage());
		assertNull(result.getConnectionDurationInMs());
		assertNull(result.getExecutionDurationInMs());
		assertTrue(result.isUnexpectedError());
		assertEquals("boom", result.getUnexpectedErrorMessage());
	}

	@Test
	void rejected_association_is_flagged_and_not_successful() {
		DicomEchoResult result = DicomEchoResult.builder().rejectionReason("Called AE Title not recognized").build();

		assertTrue(result.isRejected());
		assertFalse(result.isSuccessful());
		assertEquals("Called AE Title not recognized", result.getRejectionReason());
	}

}