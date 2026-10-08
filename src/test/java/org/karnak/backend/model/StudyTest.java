/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class StudyTest {

	@Test
	void rejects_a_null_uid() {
		assertThrows(NullPointerException.class, () -> new Study(null, "patient"));
	}

	@Test
	void defaults_a_null_patient_id_to_empty_string() {
		Study study = new Study("1.2.3", null);

		assertEquals("", study.getPatientID());
		assertEquals("", study.getStudyDescription());
	}

	@Test
	void adds_gets_and_removes_series() {
		Study study = new Study("1.2.3", "patient");
		Series series = new Series("4.5.6");

		study.addSeries(series);

		assertSame(series, study.getSeries("4.5.6"));
		assertEquals(1, study.getSeries().size());
		assertEquals(1, study.getEntrySet().size());

		assertSame(series, study.removeSeries("4.5.6"));
		assertNull(study.getSeries("4.5.6"));
	}

	@Test
	void is_empty_when_it_has_no_series() {
		Study study = new Study("1.2.3", "patient");

		assertTrue(study.isEmpty());
	}

	@Test
	void is_empty_when_all_its_series_are_empty() {
		Study study = new Study("1.2.3", "patient");
		study.addSeries(new Series("4.5.6"));

		assertTrue(study.isEmpty());
	}

	@Test
	void is_not_empty_when_a_series_holds_an_instance() {
		Study study = new Study("1.2.3", "patient");
		Series series = new Series("4.5.6");
		series.addSopInstance(new SopInstance("7.8.9"));
		study.addSeries(series);

		assertFalse(study.isEmpty());
	}

	@Test
	void stores_descriptive_attributes() {
		Study study = new Study("1.2.3", "patient");
		LocalDateTime date = LocalDateTime.of(2026, 6, 11, 10, 0);
		study.setStudyDescription("brain");
		study.setStudyDate(date);
		study.setAccessionNumber("ACC-1");
		study.setTimeStamp(1234L);
		study.setOtherPatientIDs(new String[] { "A", "B" });
		study.setPatientID("patient-2");

		assertEquals("brain", study.getStudyDescription());
		assertEquals(date, study.getStudyDate());
		assertEquals("ACC-1", study.getAccessionNumber());
		assertEquals(1234L, study.getTimeStamp());
		assertArrayEquals(new String[] { "A", "B" }, study.getOtherPatientIDs());
		assertEquals("patient-2", study.getPatientID());
	}

	@Test
	void considers_studies_equal_when_uid_matches() {
		Study a = new Study("1.2.3", "patient");
		Study b = new Study("1.2.3", "other");
		Study c = new Study("9.9.9", "patient");

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, c);
	}

}