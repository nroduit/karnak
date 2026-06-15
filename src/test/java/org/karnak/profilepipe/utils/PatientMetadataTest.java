/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.profilepipe.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.stream.Stream;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.karnak.backend.cache.Patient;
import org.karnak.backend.model.profilepipe.PatientMetadata;

class PatientMetadataTest {

	static PatientMetadata patientMetadata;
	static PatientMetadata patientMetadataDicomEmptyWithIssuer;
	static PatientMetadata patientMetadataWithNotValidPatientSex;

	@BeforeAll
	protected static void setUpBeforeClass() {
		Attributes dataset = new Attributes();
		dataset.setString(Tag.PatientID, VR.LO, "");
		dataset.setString(Tag.PatientName, VR.PN, "");
		dataset.setString(Tag.PatientBirthDate, VR.DA, "");
		dataset.setString(Tag.PatientSex, VR.CS, "");

		Attributes datasetWithNotValidPatientSex = new Attributes();
		datasetWithNotValidPatientSex.setString(Tag.PatientID, VR.LO, "EREN");
		datasetWithNotValidPatientSex.setString(Tag.PatientName, VR.PN, "Patient^Name");
		datasetWithNotValidPatientSex.setString(Tag.PatientBirthDate, VR.DA, "19930216");
		datasetWithNotValidPatientSex.setString(Tag.PatientSex, VR.CS, "X");
		datasetWithNotValidPatientSex.setString(Tag.IssuerOfPatientID, VR.LO, "PDA");

		patientMetadata = new PatientMetadata(dataset, "");
		patientMetadataDicomEmptyWithIssuer = new PatientMetadata(new Attributes(), "PDA");
		patientMetadataWithNotValidPatientSex = new PatientMetadata(datasetWithNotValidPatientSex, "");
	}

	private static Stream<Arguments> providerGetPatientID() {
		return Stream.of(Arguments.of(patientMetadata, ""), Arguments.of(patientMetadataDicomEmptyWithIssuer, ""),
				Arguments.of(patientMetadataWithNotValidPatientSex, "EREN"));
	}

	private static Stream<Arguments> providerGetPatientName() {
		return Stream.of(Arguments.of(patientMetadata, ""), Arguments.of(patientMetadataDicomEmptyWithIssuer, ""),
				Arguments.of(patientMetadataWithNotValidPatientSex, "Patient^Name"));
	}

	private static Stream<Arguments> providerGetPatientLastName() {
		return Stream.of(Arguments.of(patientMetadata, ""), Arguments.of(patientMetadataDicomEmptyWithIssuer, ""),
				Arguments.of(patientMetadataWithNotValidPatientSex, "Patient"));
	}

	private static Stream<Arguments> providerGetPatientFirstName() {
		return Stream.of(Arguments.of(patientMetadata, ""), Arguments.of(patientMetadataDicomEmptyWithIssuer, ""),
				Arguments.of(patientMetadataWithNotValidPatientSex, "Name"));
	}

	private static Stream<Arguments> providerGetPatientBirthDate() {
		return Stream.of(Arguments.of(patientMetadata, ""), Arguments.of(patientMetadataDicomEmptyWithIssuer, ""),
				Arguments.of(patientMetadataWithNotValidPatientSex, "19930216"));
	}

	private static Stream<Arguments> providerGetLocalDatePatientBirthDate() {
		return Stream.of(Arguments.of(patientMetadata, null), Arguments.of(patientMetadataDicomEmptyWithIssuer, null),
				Arguments.of(patientMetadataWithNotValidPatientSex, LocalDate.of(1993, 2, 16)));
	}

	private static Stream<Arguments> providerGetIssuerOfPatientID() {
		return Stream.of(Arguments.of(patientMetadata, ""), Arguments.of(patientMetadataDicomEmptyWithIssuer, "PDA"),
				Arguments.of(patientMetadataWithNotValidPatientSex, "PDA"));
	}

	private static Stream<Arguments> providerGetPatientSex() {
		return Stream.of(Arguments.of(patientMetadata, "O"), Arguments.of(patientMetadataDicomEmptyWithIssuer, "O"),
				Arguments.of(patientMetadataWithNotValidPatientSex, "O"));
	}

	private static Stream<Arguments> providerCompareCachedPatient() {
		return Stream.of(Arguments.of(patientMetadata, new Patient("TEST", "", "", "", "", null)),
				Arguments.of(patientMetadata, new Patient("TEST", "", "", "", null, "O", "")),
				Arguments.of(patientMetadataDicomEmptyWithIssuer, new Patient("TEST", "", "", "", "PDA", null)),
				Arguments.of(patientMetadataDicomEmptyWithIssuer, new Patient("TEST", "", "", "", null, "O", "PDA")),
				Arguments.of(patientMetadataWithNotValidPatientSex,
						new Patient("TEST", "EREN", "Name", "Patient", "PDA", null)),
				Arguments.of(patientMetadataWithNotValidPatientSex,
						new Patient("TEST", "EREN", "Name", "Patient", LocalDate.of(1993, 2, 16), "O", "PDA")));
	}

	private static Stream<Arguments> providerCompareCachedPatientFalse() {
		return Stream.of(Arguments.of(patientMetadata, new Patient("TEST", "1", "", "", "", null)),
				Arguments.of(patientMetadata, new Patient("TEST", "", "", "", "1", null)),
				Arguments.of(patientMetadata, new Patient("TEST", "1", "", "", null, "O", "")),
				Arguments.of(patientMetadata, new Patient("TEST", "", "", "", null, "O", "1")),
				Arguments.of(patientMetadataDicomEmptyWithIssuer, new Patient("TEST", "1", "", "", "PDA", null)),
				Arguments.of(patientMetadataDicomEmptyWithIssuer, new Patient("TEST", "", "", "", "", null)),
				Arguments.of(patientMetadataDicomEmptyWithIssuer, new Patient("TEST", "1", "", "", null, "O", "PDA")),
				Arguments.of(patientMetadataDicomEmptyWithIssuer, new Patient("TEST", "", "", "", null, "O", "")),
				Arguments.of(patientMetadataWithNotValidPatientSex,
						new Patient("TEST", "", "Name", "Patient", "PDA", null)),
				Arguments.of(patientMetadataWithNotValidPatientSex, new Patient("TEST", "", "^Name", "", "PDA", null)),
				Arguments.of(patientMetadataWithNotValidPatientSex,
						new Patient("TEST", "EREN", "Patient^Name", "", "", null)),
				Arguments.of(patientMetadataWithNotValidPatientSex,
						new Patient("TEST", "", "Name", "Patient", LocalDate.of(1993, 2, 16), "O", "PDA")),
				Arguments.of(patientMetadataWithNotValidPatientSex,
						new Patient("TEST", "EREN", "Name", "Patient", LocalDate.of(1993, 2, 16), "O", "")));
	}

	private static Stream<Arguments> providerPatientBirthdateInvalid() {
		Attributes dataset = new Attributes();
		dataset.setString(Tag.PatientID, VR.LO, "");
		dataset.setString(Tag.PatientName, VR.PN, "");
		dataset.setString(Tag.PatientBirthDate, VR.DA, "NULL");
		dataset.setString(Tag.PatientSex, VR.CS, "");
		return Stream.of(Arguments.of(new PatientMetadata(dataset, "")));
	}

	@ParameterizedTest
	@MethodSource("providerGetPatientID")
	void getPatientID(PatientMetadata patientMetadata, String output) {
		assertEquals(patientMetadata.getPatientID(), output);
	}

	@ParameterizedTest
	@MethodSource("providerGetPatientName")
	void getPatientName(PatientMetadata patientMetadata, String output) {
		assertEquals(patientMetadata.getPatientName(), output);
	}

	@ParameterizedTest
	@MethodSource("providerGetPatientLastName")
	void getPatientLastName(PatientMetadata patientMetadata, String output) {
		assertEquals(patientMetadata.getPatientLastName(), output);
	}

	@ParameterizedTest
	@MethodSource("providerGetPatientFirstName")
	void getPatientFirstName(PatientMetadata patientMetadata, String output) {
		assertEquals(patientMetadata.getPatientFirstName(), output);
	}

	@ParameterizedTest
	@MethodSource("providerGetPatientBirthDate")
	void getPatientBirthDate(PatientMetadata patientMetadata, String output) {
		assertEquals(patientMetadata.getPatientBirthDate(), output);
	}

	@ParameterizedTest
	@MethodSource("providerGetLocalDatePatientBirthDate")
	void getLocalDatePatientBirthDate(PatientMetadata patientMetadata, LocalDate output) {
		if (output == null) {
			assertEquals(patientMetadata.getLocalDatePatientBirthDate(), output);
		}
		else {
			assertEquals(patientMetadata.getLocalDatePatientBirthDate().getDayOfMonth(), output.getDayOfMonth());
			assertEquals(patientMetadata.getLocalDatePatientBirthDate().getMonth(), output.getMonth());
			assertEquals(patientMetadata.getLocalDatePatientBirthDate().getYear(), output.getYear());
		}
	}

	@ParameterizedTest
	@MethodSource("providerGetIssuerOfPatientID")
	void getIssuerOfPatientID(PatientMetadata patientMetadata, String output) {
		assertEquals(patientMetadata.getIssuerOfPatientID(), output);
	}

	@ParameterizedTest
	@MethodSource("providerGetPatientSex")
	void getPatientSex(PatientMetadata patientMetadata, String output) {
		assertEquals(patientMetadata.getPatientSex(), output);
	}

	@ParameterizedTest
	@MethodSource("providerCompareCachedPatient")
	void compareCachedPatient(PatientMetadata patientMetadata, Patient patient) {
		assertTrue(patientMetadata.compareCachedPatient(patient));
	}

	@ParameterizedTest
	@MethodSource("providerCompareCachedPatientFalse")
	void compareCachedPatientFalse(PatientMetadata patientMetadata, Patient patient) {
		assertFalse(patientMetadata.compareCachedPatient(patient));
	}

	@ParameterizedTest
	@MethodSource("providerPatientBirthdateInvalid")
	void patientBirthdateInvalid(PatientMetadata patientMetadata) {
		assertEquals("", patientMetadata.getPatientBirthDate());
	}

}
