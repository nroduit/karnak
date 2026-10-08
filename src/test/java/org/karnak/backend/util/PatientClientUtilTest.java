/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.karnak.backend.cache.InMemoryExternalIDCache;
import org.karnak.backend.model.patient.PatientModel;
import org.karnak.backend.cache.PseudonymCache;
import org.karnak.backend.model.profilepipe.PatientMetadata;
import java.util.UUID;

@DisplayNameGeneration(ReplaceUnderscores.class)
class PatientClientUtilTest {

	private static PatientModel patient() {
		return new PatientModel("PSEUDO", "PID-1", "John", "Doe", null, "M", "ISSUER-1");
	}

	private static PatientMetadata metadata() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.PatientID, VR.LO, "PID-1");
		dcm.setString(Tag.IssuerOfPatientID, VR.LO, "ISSUER-1");
		return new PatientMetadata(dcm, "");
	}

	@Nested
	class KeyGeneration {

		@Test
		void concatenates_patient_id_and_issuer() {
			assertEquals("PID-1ISSUER-1", PatientClientUtil.generateKey("PID-1", "ISSUER-1"));
		}

		@Test
		void omits_a_null_issuer() {
			assertEquals("PID-1", PatientClientUtil.generateKey("PID-1", null));
		}

		@Test
		void from_a_patient_matches_from_metadata() {
			assertEquals(PatientClientUtil.generateKey(metadata()), PatientClientUtil.generateKey(patient()));
		}

		@Test
		void appends_the_project_id_from_metadata() {
			assertEquals("PID-1ISSUER-199", PatientClientUtil.generateKey(metadata(), 99L));
		}

		@Test
		void appends_the_project_id_from_a_patient() {
			assertEquals("PID-1ISSUER-199", PatientClientUtil.generateKey(patient(), 99L));
		}

		@Test
		void omits_a_null_project_id() {
			assertEquals("PID-1ISSUER-1", PatientClientUtil.generateKey(metadata(), null));
		}

	}

	@Nested
	class PseudonymLookup {

		@Test
		void returns_null_when_the_cache_is_null() {
			assertNull(PatientClientUtil.getPseudonym(metadata(), null));
		}

		@Test
		void returns_the_pseudonym_of_a_matching_cached_patient() {
			PseudonymCache cache = new InMemoryExternalIDCache();
			cache.put(PatientClientUtil.generateKey(patient()), patient());

			assertEquals("PSEUDO", PatientClientUtil.getPseudonym(metadata(), cache));
		}

		@Test
		void returns_null_when_the_patient_is_not_cached() {
			PseudonymCache cache = new InMemoryExternalIDCache();

			assertNull(PatientClientUtil.getPseudonym(metadata(), cache));
		}

		@Test
		void returns_the_pseudonym_for_a_matching_patient_and_project() {
			PseudonymCache cache = new InMemoryExternalIDCache();
			cache.put(PatientClientUtil.generateKey(patient(), 5L), patient());

			assertEquals("PSEUDO", PatientClientUtil.getPseudonym(metadata(), cache, 5L));
		}

	}

	@Nested
	class SkipIssuerLookup {

		private static PatientModel patientWithProject() {
			return new PatientModel("PSEUDO", "PID-1", "John", "Doe", "ISSUER-1", 5L, UUID.randomUUID());
		}

		private static PatientMetadata metadataWithoutIssuer() {
			Attributes dcm = new Attributes();
			dcm.setString(Tag.PatientID, VR.LO, "PID-1");
			return new PatientMetadata(dcm);
		}

		@Test
		void recovers_a_pseudonym_stored_with_an_issuer_when_skipping_the_issuer() {
			PseudonymCache cache = new InMemoryExternalIDCache();
			// The populate side embeds the issuer in the key.
			cache.put(PatientClientUtil.generateKey(patientWithProject(), 5L), patientWithProject());

			// Without skipping, the issuer-embedded key cannot be matched.
			assertNull(PatientClientUtil.getPseudonym(metadataWithoutIssuer(), cache, 5L, false));
			// Skipping the issuer falls back to an issuer-agnostic scan and finds it.
			assertEquals("PSEUDO", PatientClientUtil.getPseudonym(metadataWithoutIssuer(), cache, 5L, true));
		}

		@Test
		void does_not_match_across_project_boundaries() {
			PseudonymCache cache = new InMemoryExternalIDCache();
			cache.put(PatientClientUtil.generateKey(patientWithProject(), 5L), patientWithProject());

			assertNull(PatientClientUtil.getPseudonym(metadataWithoutIssuer(), cache, 9L, true));
		}

		@Test
		void does_not_match_a_different_patient_id() {
			PseudonymCache cache = new InMemoryExternalIDCache();
			cache.put(PatientClientUtil.generateKey(patientWithProject(), 5L), patientWithProject());

			Attributes dcm = new Attributes();
			dcm.setString(Tag.PatientID, VR.LO, "OTHER");
			assertNull(PatientClientUtil.getPseudonym(new PatientMetadata(dcm), cache, 5L, true));
		}

	}

}