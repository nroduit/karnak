/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import java.util.Objects;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.cache.Patient;
import org.karnak.backend.cache.PatientClient;
import org.karnak.backend.model.profilepipe.PatientMetadata;

@NullUnmarked
public class PatientClientUtil {

	private PatientClientUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static String getPseudonym(PatientMetadata patientMetadata, PatientClient cache) {
		if (cache != null) {
			final String key = generateKey(patientMetadata);
			return getCachedKey(key, patientMetadata, cache);
		}
		return null;
	}

	public static String getPseudonym(PatientMetadata patientMetadata, PatientClient cache, Long projectID) {
		if (cache != null) {
			final String key = generateKey(patientMetadata, projectID);
			return getCachedKey(key, patientMetadata, cache);
		}
		return null;
	}

	public static String getPseudonym(PatientMetadata patientMetadata, PatientClient cache, Long projectID,
			boolean skipIssuerOfPatientId) {
		if (cache != null) {
			final String key = generateKey(patientMetadata, projectID, skipIssuerOfPatientId);
			final String pseudonym = getCachedKey(key, patientMetadata, cache, skipIssuerOfPatientId);
			if (pseudonym == null && skipIssuerOfPatientId) {
				// The populated side (manual entry / CSV import) always stores patients
				// under a key embedding the issuer. When the issuer-skipping keyed lookup
				// misses, fall back to an issuer-agnostic scan, so the pseudonym is still
				// found regardless of the issuer used at insertion time.
				return findPseudonymIgnoringIssuer(patientMetadata, cache, projectID);
			}
			return pseudonym;
		}
		return null;
	}

	private static String findPseudonymIgnoringIssuer(PatientMetadata patientMetadata, PatientClient cache,
			Long projectID) {
		return cache.getAll()
			.stream()
			.filter(patient -> Objects.equals(patient.getProjectID(), projectID))
			.filter(patient -> patientMetadata.compareCachedPatient(patient, true))
			.map(Patient::getPseudonym)
			.findFirst()
			.orElse(null);
	}

	private static String getCachedKey(String key, PatientMetadata patientMetadata, PatientClient cache) {
		final Patient patient = cache.get(key);
		if (patient != null && patientMetadata.compareCachedPatient(patient)) {
			return patient.getPseudonym();
		}
		return null;
	}

	private static String getCachedKey(String key, PatientMetadata patientMetadata, PatientClient cache,
			boolean skipIssuerOfPatientId) {
		final Patient patient = cache.get(key);
		if (patient != null && patientMetadata.compareCachedPatient(patient, skipIssuerOfPatientId)) {
			return patient.getPseudonym();
		}
		return null;
	}

	public static String generateKey(String patientID, String issuerOfPatientID) {
		return patientID.concat(issuerOfPatientID == null ? "" : issuerOfPatientID);
	}

	public static String generateKey(String patientID, String issuerOfPatientID, boolean skipIssuerOfPatientId) {
		if (skipIssuerOfPatientId) {
			return patientID;
		}
		return generateKey(patientID, issuerOfPatientID);
	}

	public static String generateKey(Patient patient) {
		String patientID = patient.getPatientId();
		String issuerOfPatientID = patient.getIssuerOfPatientId();
		return generateKey(patientID, issuerOfPatientID);
	}

	public static String generateKey(PatientMetadata patientMetadata) {
		String patientID = patientMetadata.getPatientID();
		String issuerOfPatientID = patientMetadata.getIssuerOfPatientID();
		return generateKey(patientID, issuerOfPatientID);
	}

	public static String generateKey(PatientMetadata patientMetadata, boolean skipIssuerOfPatientId) {
		String patientID = patientMetadata.getPatientID();
		String issuerOfPatientID = patientMetadata.getIssuerOfPatientID();
		return generateKey(patientID, issuerOfPatientID, skipIssuerOfPatientId);
	}

	public static String generateKey(PatientMetadata patientMetadata, Long projectID) {
		final String key = generateKey(patientMetadata);
		return key.concat(projectID == null ? "" : projectID.toString());
	}

	public static String generateKey(PatientMetadata patientMetadata, Long projectID, boolean skipIssuerOfPatientId) {
		final String key = generateKey(patientMetadata, skipIssuerOfPatientId);
		return key.concat(projectID == null ? "" : projectID.toString());
	}

	public static String generateKey(Patient patient, Long projectID) {
		final String key = generateKey(patient);
		return key.concat(projectID == null ? "" : projectID.toString());
	}

}
