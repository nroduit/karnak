/*
 * Copyright (c) 2025 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.patient.PatientModel;

@DisplayNameGeneration(ReplaceUnderscores.class)
class PatientTest {

	private static final String PSEUDONYM = "pseudo";

	private static final String PATIENT_ID = "EREN";

	private static final String ISSUER = "PDA";

	@Nested
	class Constructors {

		@Test
		void builds_DICOM_patient_name_as_lastName_caret_firstName() {
			var patient = new PatientModel(PSEUDONYM, PATIENT_ID, "John", "Doe", ISSUER, 1L, UUID.randomUUID());

			assertEquals("Doe^John", patient.getPatientName());
			assertEquals("John", patient.getPatientFirstName());
			assertEquals("Doe", patient.getPatientLastName());
		}

		@Test
		void uses_lastName_only_when_firstName_is_empty() {
			var patient = new PatientModel(PSEUDONYM, PATIENT_ID, "", "Doe", ISSUER, 1L, UUID.randomUUID());

			assertEquals("Doe", patient.getPatientName());
			assertEquals("", patient.getPatientFirstName());
			assertEquals("Doe", patient.getPatientLastName());
		}

		@Test
		void stores_empty_strings_when_names_are_null() {
			var patient = new PatientModel(PSEUDONYM, PATIENT_ID, null, null, ISSUER, 1L, UUID.randomUUID());

			assertEquals("", patient.getPatientFirstName());
			assertEquals("", patient.getPatientLastName());
		}

		@Test
		void prefixes_caret_when_only_firstName_is_present() {
			var patient = new PatientModel(PSEUDONYM, PATIENT_ID, "John", null, ISSUER, 1L, UUID.randomUUID());

			assertEquals("^John", patient.getPatientName());
			assertEquals("John", patient.getPatientFirstName());
			assertEquals("", patient.getPatientLastName());
		}

		@Test
		void keeps_explicit_patient_name_with_the_full_constructor() {
			var patient = new PatientModel(PSEUDONYM, PATIENT_ID, "Doe^John", "John", "Doe", LocalDate.of(1993, 2, 16), "M",
					ISSUER, 1L, UUID.randomUUID());

			assertEquals("Doe^John", patient.getPatientName());
			assertEquals(LocalDate.of(1993, 2, 16), patient.getPatientBirthDate());
			assertEquals("M", patient.getPatientSex());
		}

	}

	@Nested
	class Updates {

		@Test
		void update_patient_name_splits_first_and_last_name() {
			var patient = newDoeJohn();

			patient.updatePatientName("Smith^Jane");

			assertEquals("Smith^Jane", patient.getPatientName());
			assertEquals("Jane", patient.getPatientFirstName());
			assertEquals("Smith", patient.getPatientLastName());
		}

		@Test
		void update_patient_name_without_separator_clears_first_name() {
			var patient = newDoeJohn();

			patient.updatePatientName("Solo");

			assertEquals("Solo", patient.getPatientName());
			assertEquals("", patient.getPatientFirstName());
			assertEquals("Solo", patient.getPatientLastName());
		}

		@Test
		void update_first_name_rebuilds_the_patient_name() {
			var patient = newDoeJohn();

			patient.updatePatientFirstName("Jane");

			assertEquals("Jane", patient.getPatientFirstName());
			assertEquals("Doe^Jane", patient.getPatientName());
		}

		@Test
		void update_last_name_rebuilds_the_patient_name() {
			var patient = newDoeJohn();

			patient.updatePatientLastName("Smith");

			assertEquals("Smith", patient.getPatientLastName());
			assertEquals("Smith^John", patient.getPatientName());
		}

		@Test
		void update_last_name_with_null_keeps_first_name_prefixed_by_caret() {
			var patient = newDoeJohn();

			patient.updatePatientLastName(null);

			assertEquals("", patient.getPatientLastName());
			assertEquals("^John", patient.getPatientName());
		}

		private PatientModel newDoeJohn() {
			return new PatientModel(PSEUDONYM, PATIENT_ID, "John", "Doe", ISSUER, 1L, UUID.randomUUID());
		}

	}

}