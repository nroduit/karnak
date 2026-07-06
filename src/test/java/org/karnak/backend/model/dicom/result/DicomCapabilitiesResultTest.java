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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DicomCapabilitiesResultTest {

	@Test
	void builder_populates_the_associated_capabilities() {
		SopClassCapability capability = new SopClassCapability("Storage", "CT Image Storage",
				"1.2.840.10008.5.1.4.1.1.2", List.of("Implicit VR Little Endian"));

		DicomCapabilitiesResult result = DicomCapabilitiesResult.builder()
			.associated(true)
			.maxPduLength(16384)
			.remoteImplementationVersionName("dcm4che-5")
			.remoteImplementationClassUid("1.2.40.0.13.1.3")
			.capability(capability)
			.build();

		assertTrue(result.isAssociated());
		assertEquals(16384, result.getMaxPduLength());
		assertEquals("dcm4che-5", result.getRemoteImplementationVersionName());
		assertEquals(1, result.getCapabilities().size());
		assertEquals(capability, result.getCapabilities().getFirst());
	}

	@Test
	void is_rejected_reflects_a_rejection_reason() {
		DicomCapabilitiesResult rejected = DicomCapabilitiesResult.builder()
			.associated(false)
			.rejectionReason("called AE title not recognized")
			.build();

		assertTrue(rejected.isRejected());
		assertFalse(rejected.isAssociated());
		assertFalse(rejected.isUnexpectedError());
	}

	@Test
	void is_unexpected_error_reflects_an_error_message() {
		DicomCapabilitiesResult errored = DicomCapabilitiesResult.builder()
			.unexpectedErrorMessage("connection refused")
			.build();

		assertTrue(errored.isUnexpectedError());
		assertFalse(errored.isRejected());
	}

	@Test
	void a_plain_result_is_neither_rejected_nor_errored() {
		DicomCapabilitiesResult result = DicomCapabilitiesResult.builder().associated(true).build();

		assertFalse(result.isRejected());
		assertFalse(result.isUnexpectedError());
	}

}