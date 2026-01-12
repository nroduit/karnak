/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
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
import java.util.stream.Stream;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.karnak.backend.model.profilepipe.PatientMetadata;
import org.karnak.backend.util.PatientClientUtil;

class PatientClientUtilTest {

	static Attributes dataset;
	static Attributes datasetWithIssuer;

	@BeforeAll
	protected static void setUpBeforeClass() {
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

	private static Stream<Arguments> providerGenerateKey() {
		return Stream.of(Arguments.of("123", "456", "123456"), Arguments.of("123", "", "123"),
				Arguments.of("EREN", "Patient^Name", "ERENPatient^Name"), Arguments.of("EREN", "", "EREN"));
	}

	private static Stream<Arguments> providerGenerateKeyPseudonymPatient() {
		return Stream.of(Arguments.of(new Patient("pseudo", "123", "456", "789", "101112", null), "123101112"),
				Arguments.of(new Patient("pseudo", "123", "456", "789", "", null), "123", null),
				Arguments.of(new Patient("pseudo", "EREN", "Patient", "Name", "PDA", null), "ERENPDA"),
				Arguments.of(new Patient("pseudo", "EREN", "Patient", "Name", "", null), "EREN"),
				Arguments.of(new Patient("pseudo", "123", "", "456", LocalDate.of(1993, 02, 16), "M", ""), "123"),
				Arguments.of(new Patient("pseudo", "123", "", "456", LocalDate.of(1993, 02, 16), "M", "789"), "123789"),
				Arguments.of(new Patient("pseudo", "EREN", "Name", "Patient", LocalDate.of(1993, 02, 16), "M", "PDA"),
						"ERENPDA"),
				Arguments.of(new Patient("pseudo", "EREN", "Name", "Patient", LocalDate.of(1993, 02, 16), "M", ""),
						"EREN"));
	}

	private static Stream<Arguments> providerGenerateKeyPseudonymPatientAndProjectID() {
		return Stream.of(Arguments.of(new Patient("pseudo", "123", "456", "789", "101112", 900L), "123101112900"),
				Arguments.of(new Patient("pseudo", "123", "456", "789", "", 128L), "123128"),
				Arguments.of(new Patient("pseudo", "EREN", "Patient", "Name", "PDA", 524L), "ERENPDA524"));
	}

	private static Stream<Arguments> providerGenerateKeyPatientMetadata() {
		return Stream.of(Arguments.of(new PatientMetadata(dataset, "PDA"), "ERENPDA"),
				Arguments.of(new PatientMetadata(dataset, ""), "EREN"),
				Arguments.of(new PatientMetadata(datasetWithIssuer, "TEST"), "ERENPDA"),
				Arguments.of(new PatientMetadata(datasetWithIssuer, ""), "ERENPDA"));
	}

	@ParameterizedTest
	@MethodSource("providerGenerateKey")
	void generateKey(String PatientID, String IssuerOfPatientID, String output) {
		assertEquals(PatientClientUtil.generateKey(PatientID, IssuerOfPatientID), output);
	}

	@ParameterizedTest
	@MethodSource("providerGenerateKeyPseudonymPatient")
	void generateKeyPseudonymPatient(Patient patient, String output) {
		assertEquals(PatientClientUtil.generateKey(patient), output);
	}

	@ParameterizedTest
	@MethodSource("providerGenerateKeyPseudonymPatientAndProjectID")
	void providerGenerateKeyPseudonymPatientAndProjectID(Patient patient, String output) {
		assertEquals(PatientClientUtil.generateKey(patient, patient.getProjectID()), output);
	}

	@ParameterizedTest
	@MethodSource("providerGenerateKeyPatientMetadata")
	void generateKeyPatientMetadata(PatientMetadata patientMetadata, String output) {
		assertEquals(PatientClientUtil.generateKey(patientMetadata), output);
	}

}
