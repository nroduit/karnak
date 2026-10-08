/*
 * Copyright (c) 2019-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profilepipe;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.Test;

class SensitiveTagDefinitionTest {

	@Test
	void extractSensitiveData_works_correctly() {
		String patientName = "DOE^JOHN";
		String patientId = "962738";
		String patientBirthDate = "19280303";
		String patientSex = "M";
		String patientAge = "61";

		Attributes attributes = new Attributes();
		attributes.setString(Tag.PatientName, VR.PN, patientName);
		attributes.setString(Tag.PatientID, VR.LO, patientId);
		attributes.setString(Tag.PatientBirthDate, VR.DA, patientBirthDate);
		attributes.setString(Tag.PatientSex, VR.CS, patientSex);
		attributes.setString(Tag.PatientAge, VR.AS, patientAge);

		Map<String, String> retrievedData = SensitiveTagDefinition.extractSensitiveData(attributes);

		assertThat(retrievedData).containsEntry("PatientName", patientName)
			.containsEntry("PatientID", patientId)
			.containsEntry("PatientBirthDate", patientBirthDate)
			.containsEntry("PatientSex", patientSex)
			.containsEntry("PatientAge", patientAge)
			.hasSize(5);
	}

	@Test
	void extractSensitiveData_only_present_tags_included() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PatientName, VR.PN, "TEST^PRENOM");

		Map<String, String> retrievedData = SensitiveTagDefinition.extractSensitiveData(attributes);

		assertThat(retrievedData).containsEntry("PatientName", "TEST^PRENOM").doesNotContainKey("PatientID");
	}

	@Test
	void extractSensitiveData_no_tags_should_return_empty() {
		Attributes attributes = new Attributes();

		Map<String, String> retrievedData = SensitiveTagDefinition.extractSensitiveData(attributes);

		assertThat(retrievedData).isEmpty();
	}

}
