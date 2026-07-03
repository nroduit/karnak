/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@code MonitoringEntry.of} factory maps the original / to-send attributes.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class MonitoringEntryTest {

	private static Attributes original() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.PatientID, VR.LO, "REAL-ID");
		dcm.setString(Tag.AccessionNumber, VR.SH, "ACC-1");
		dcm.setString(Tag.StudyDescription, VR.LO, "Chest CT");
		dcm.setString(Tag.StudyInstanceUID, VR.UI, "1.2.3");
		dcm.setString(Tag.SeriesDescription, VR.LO, "Axial");
		dcm.setString(Tag.SeriesInstanceUID, VR.UI, "1.2.3.4");
		dcm.setString(Tag.StudyDate, VR.DA, "20260101");
		dcm.setString(Tag.StudyTime, VR.TM, "101500");
		return dcm;
	}

	private static Attributes toSend() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.PatientID, VR.LO, "PSEUDO-ID");
		dcm.setString(Tag.AccessionNumber, VR.SH, "ACC-PSEUDO");
		dcm.setString(Tag.StudyInstanceUID, VR.UI, "9.8.7");
		dcm.setString(Tag.SeriesInstanceUID, VR.UI, "9.8.7.6");
		return dcm;
	}

	@Test
	void of_copies_the_original_and_to_send_identifiers() {
		MonitoringEntry entry = MonitoringEntry.of(1L, 2L, original(), toSend(), true, false, null, "CT",
				"1.2.840.10008.5.1.4.1.1.2");

		assertEquals(1L, entry.forwardNodeId());
		assertEquals(2L, entry.destinationId());
		assertTrue(entry.sent());
		assertFalse(entry.error());
		assertEquals("REAL-ID", entry.patientIdOriginal());
		assertEquals("PSEUDO-ID", entry.patientIdToSend());
		assertEquals("ACC-1", entry.accessionNumberOriginal());
		assertEquals("ACC-PSEUDO", entry.accessionNumberToSend());
		assertEquals("Chest CT", entry.studyDescriptionOriginal());
		assertEquals("1.2.3", entry.studyUidOriginal());
		assertEquals("9.8.7", entry.studyUidToSend());
		assertEquals("1.2.3.4", entry.serieUidOriginal());
		assertEquals("9.8.7.6", entry.serieUidToSend());
		assertEquals("CT", entry.modality());
		assertEquals("1.2.840.10008.5.1.4.1.1.2", entry.sopClassUid());
	}

	@Test
	void of_parses_the_study_date_and_stamps_a_timestamp() {
		MonitoringEntry entry = MonitoringEntry.of(1L, 2L, original(), toSend(), false, true, "boom", "CT", "1.2.3");

		assertNotNull(entry.timestamp());
		assertNotNull(entry.studyDateOriginal());
		assertEquals(2026, entry.studyDateOriginal().getYear());
		assertEquals("boom", entry.reason());
		assertTrue(entry.error());
	}

}