/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profilepipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.patient.PatientModel;

@DisplayNameGeneration(ReplaceUnderscores.class)
class PatientMetadataTest {

	private static Attributes attributes(String id, String name, String birthDate, String sex) {
		Attributes dcm = new Attributes();
		if (id != null) {
			dcm.setString(Tag.PatientID, VR.LO, id);
		}
		if (name != null) {
			dcm.setString(Tag.PatientName, VR.PN, name);
		}
		if (birthDate != null) {
			dcm.setString(Tag.PatientBirthDate, VR.DA, birthDate);
		}
		if (sex != null) {
			dcm.setString(Tag.PatientSex, VR.CS, sex);
		}
		return dcm;
	}

	@Nested
	class NameSplitting {

		@Test
		void splits_dicom_name_into_last_and_first() {
			PatientMetadata metadata = new PatientMetadata(attributes("1", "Doe^John", null, null));

			assertEquals("Doe", metadata.getPatientLastName());
			assertEquals("John", metadata.getPatientFirstName());
		}

		@Test
		void returns_empty_first_name_when_only_last_name_present() {
			PatientMetadata metadata = new PatientMetadata(attributes("1", "Doe", null, null));

			assertEquals("Doe", metadata.getPatientLastName());
			assertEquals("", metadata.getPatientFirstName());
		}

		@Test
		void defaults_missing_name_to_empty_string() {
			PatientMetadata metadata = new PatientMetadata(attributes("1", null, null, null));

			assertEquals("", metadata.getPatientName());
			assertEquals("", metadata.getPatientLastName());
			assertEquals("", metadata.getPatientFirstName());
		}

	}

	@Nested
	class Sex {

		@Test
		void keeps_male_and_female() {
			assertEquals("M", new PatientMetadata(attributes("1", null, null, "M")).getPatientSex());
			assertEquals("F", new PatientMetadata(attributes("1", null, null, "F")).getPatientSex());
		}

		@Test
		void normalizes_unknown_value_to_other() {
			assertEquals("O", new PatientMetadata(attributes("1", null, null, "X")).getPatientSex());
		}

		@Test
		void defaults_to_other_when_absent() {
			assertEquals("O", new PatientMetadata(attributes("1", null, null, null)).getPatientSex());
		}

	}

	@Nested
	class BirthDate {

		@Test
		void formats_a_valid_birth_date() {
			PatientMetadata metadata = new PatientMetadata(attributes("1", null, "19800101", null));

			assertEquals("19800101", metadata.getPatientBirthDate());
			assertEquals(LocalDate.of(1980, 1, 1), metadata.getLocalDatePatientBirthDate());
		}

		@Test
		void returns_empty_string_and_null_date_when_absent() {
			PatientMetadata metadata = new PatientMetadata(attributes("1", null, null, null));

			assertEquals("", metadata.getPatientBirthDate());
			assertNull(metadata.getLocalDatePatientBirthDate());
		}

	}

	@Nested
	class IssuerOfPatientId {

		@Test
		void defaults_to_empty_with_the_single_argument_constructor() {
			PatientMetadata metadata = new PatientMetadata(attributes("1", null, null, null));

			assertEquals("", metadata.getIssuerOfPatientID());
		}

		@Test
		void falls_back_to_provided_default_when_tag_absent() {
			PatientMetadata metadata = new PatientMetadata(attributes("1", null, null, null), "PDA");

			assertEquals("PDA", metadata.getIssuerOfPatientID());
		}

		@Test
		void uses_the_value_from_the_dicom_object_when_present() {
			Attributes dcm = attributes("1", null, null, null);
			dcm.setString(Tag.IssuerOfPatientID, VR.LO, "HOSPITAL");

			PatientMetadata metadata = new PatientMetadata(dcm, "PDA");

			assertEquals("HOSPITAL", metadata.getIssuerOfPatientID());
		}

	}

	@Nested
	class CompareCachedPatient {

		private PatientModel patient(String id, String issuer) {
			return new PatientModel("pseudo", id, "John", "Doe", null, "O", issuer);
		}

		@Test
		void matches_when_id_and_issuer_are_equal() {
			PatientMetadata metadata = new PatientMetadata(attributes("EREN", null, null, null), "PDA");

			assertTrue(metadata.compareCachedPatient(patient("EREN", "PDA")));
		}

		@Test
		void matches_when_cached_issuer_is_null() {
			PatientMetadata metadata = new PatientMetadata(attributes("EREN", null, null, null), "PDA");

			assertTrue(metadata.compareCachedPatient(patient("EREN", null)));
		}

		@Test
		void does_not_match_when_id_differs() {
			PatientMetadata metadata = new PatientMetadata(attributes("EREN", null, null, null), "PDA");

			assertFalse(metadata.compareCachedPatient(patient("ARMIN", "PDA")));
		}

		@Test
		void does_not_match_when_issuer_differs() {
			PatientMetadata metadata = new PatientMetadata(attributes("EREN", null, null, null), "PDA");

			assertFalse(metadata.compareCachedPatient(patient("EREN", "OTHER")));
		}

		@Test
		void does_not_match_null_patient() {
			PatientMetadata metadata = new PatientMetadata(attributes("EREN", null, null, null), "PDA");

			assertFalse(metadata.compareCachedPatient(null));
		}

	}

}