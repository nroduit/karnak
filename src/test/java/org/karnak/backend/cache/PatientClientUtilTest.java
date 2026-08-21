/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.karnak.backend.model.patient.PatientModel;
import org.karnak.backend.model.profilepipe.PatientMetadata;
import org.karnak.backend.util.PatientClientUtil;

@DisplayNameGeneration(ReplaceUnderscores.class)
class PatientClientUtilTest {

	static Attributes dataset;
	static Attributes datasetWithIssuer;

	@BeforeAll
	static void setUpBeforeClass() {
		dataset = new Attributes();
		dataset.setString(Tag.PatientID, VR.LO, "EREN");
		dataset.setString(Tag.PatientName, VR.PN, "Patient^Name");
		dataset.setString(Tag.PatientBirthDate, VR.DA, "19930216");
		dataset.setString(Tag.PatientSex, VR.CS, "M");

		datasetWithIssuer = new Attributes();
		datasetWithIssuer.setString(Tag.PatientID, VR.LO, "EREN");
		datasetWithIssuer.setString(Tag.PatientName, VR.PN, "Patient^Name");
		datasetWithIssuer.setString(Tag.PatientBirthDate, VR.DA, "19930216");
		datasetWithIssuer.setString(Tag.PatientSex, VR.CS, "M");
		datasetWithIssuer.setString(Tag.IssuerOfPatientID, VR.LO, "PDA");
	}

	@Nested
	class GenerateKey {

		private static Stream<Arguments> providerGenerateKey() {
			return Stream.of(Arguments.of("123", "456", "123456"), Arguments.of("123", "", "123"),
					Arguments.of("EREN", "Patient^Name", "ERENPatient^Name"), Arguments.of("EREN", "", "EREN"));
		}

		private static Stream<Arguments> providerGenerateKeyPseudonymPatient() {
			return Stream.of(Arguments.of(new PatientModel("pseudo", "123", "456", "789", "101112", null, UUID.randomUUID()), "123101112"),
					Arguments.of(new PatientModel("pseudo", "123", "456", "789", "", null, UUID.randomUUID()), "123", null),
					Arguments.of(new PatientModel("pseudo", "EREN", "Patient", "Name", "PDA", null, UUID.randomUUID()), "ERENPDA"),
					Arguments.of(new PatientModel("pseudo", "EREN", "Patient", "Name", "", null, UUID.randomUUID()), "EREN"),
					Arguments.of(new PatientModel("pseudo", "123", "", "456", LocalDate.of(1993, 2, 16), "M", ""), "123"),
					Arguments.of(new PatientModel("pseudo", "123", "", "456", LocalDate.of(1993, 2, 16), "M", "789"),
							"123789"),
					Arguments.of(
							new PatientModel("pseudo", "EREN", "Name", "Patient", LocalDate.of(1993, 2, 16), "M", "PDA"),
							"ERENPDA"),
					Arguments.of(new PatientModel("pseudo", "EREN", "Name", "Patient", LocalDate.of(1993, 2, 16), "M", ""),
							"EREN"));
		}

		private static Stream<Arguments> providerGenerateKeyPseudonymPatientAndProjectID() {
			return Stream.of(Arguments.of(new PatientModel("pseudo", "123", "456", "789", "101112", 900L, UUID.randomUUID()), "123101112900"),
					Arguments.of(new PatientModel("pseudo", "123", "456", "789", "", 128L, UUID.randomUUID()), "123128"),
					Arguments.of(new PatientModel("pseudo", "EREN", "Patient", "Name", "PDA", 524L, UUID.randomUUID()), "ERENPDA524"));
		}

		private static Stream<Arguments> providerGenerateKeyPatientMetadata() {
			return Stream.of(Arguments.of(new PatientMetadata(dataset, "PDA"), "ERENPDA"),
					Arguments.of(new PatientMetadata(dataset, ""), "EREN"),
					Arguments.of(new PatientMetadata(datasetWithIssuer, "TEST"), "ERENPDA"),
					Arguments.of(new PatientMetadata(datasetWithIssuer, ""), "ERENPDA"));
		}

		@ParameterizedTest
		@MethodSource("providerGenerateKey")
		void from_patient_id_and_issuer(String patientID, String issuerOfPatientID, String output) {
			assertEquals(output, PatientClientUtil.generateKey(patientID, issuerOfPatientID));
		}

		@ParameterizedTest
		@MethodSource("providerGenerateKeyPseudonymPatient")
		void from_patient(PatientModel patient, String output) {
			assertEquals(output, PatientClientUtil.generateKey(patient));
		}

		@ParameterizedTest
		@MethodSource("providerGenerateKeyPseudonymPatientAndProjectID")
		void from_patient_and_project_id(PatientModel patient, String output) {
			assertEquals(output, PatientClientUtil.generateKey(patient, patient.getProjectID()));
		}

		@ParameterizedTest
		@MethodSource("providerGenerateKeyPatientMetadata")
		void from_patient_metadata(PatientMetadata patientMetadata, String output) {
			assertEquals(output, PatientClientUtil.generateKey(patientMetadata));
		}

	}

	@Nested
	class GetPseudonym {

		private final PseudonymCache cache = new InMemoryExternalIDCache();

		@Test
		void returns_the_cached_pseudonym_when_id_and_issuer_match() {
			var metadata = new PatientMetadata(datasetWithIssuer, "");
			cache.put(PatientClientUtil.generateKey(metadata),
					new PatientModel("the-pseudonym", "EREN", "Name", "Patient", "PDA", null, UUID.randomUUID()));

			assertEquals("the-pseudonym", PatientClientUtil.getPseudonym(metadata, cache));
		}

		@Test
		void returns_null_when_the_key_is_not_cached() {
			var metadata = new PatientMetadata(datasetWithIssuer, "");

			assertNull(PatientClientUtil.getPseudonym(metadata, cache));
		}

		@Test
		void returns_null_when_a_cached_entry_has_a_different_patient_id() {
			var metadata = new PatientMetadata(datasetWithIssuer, "");
			// Same key but the stored patient does not describe the same person.
			cache.put(PatientClientUtil.generateKey(metadata),
					new PatientModel("other-pseudonym", "OTHER", "Name", "Patient", "PDA", null, UUID.randomUUID()));

			assertNull(PatientClientUtil.getPseudonym(metadata, cache));
		}

		@Test
		void returns_null_when_the_cache_is_null() {
			var metadata = new PatientMetadata(datasetWithIssuer, "");

			assertNull(PatientClientUtil.getPseudonym(metadata, null));
		}

		@Test
		void scopes_the_lookup_by_project_id() {
			var metadata = new PatientMetadata(datasetWithIssuer, "");
			cache.put(PatientClientUtil.generateKey(metadata, 42L),
					new PatientModel("scoped-pseudonym", "EREN", "Name", "Patient", "PDA", 42L, UUID.randomUUID()));

			assertEquals("scoped-pseudonym", PatientClientUtil.getPseudonym(metadata, cache, 42L));
			assertNull(PatientClientUtil.getPseudonym(metadata, cache, 99L));
		}

	}

}