/*
 * Copyright 2019-2026 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.karnak.backend.model.profilepipe;

import java.util.Map;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SensitiveTagDefinitionTest {

	@Test
	public void extractSensitiveData_works_correctly() {
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

		assertThat(retrievedData).containsEntry("PatientName", patientName);
		assertThat(retrievedData).containsEntry("PatientID", patientId);
		assertThat(retrievedData).containsEntry("PatientBirthDate", patientBirthDate);
		assertThat(retrievedData).containsEntry("PatientSex", patientSex);
		assertThat(retrievedData).containsEntry("PatientAge", patientAge);
		assertThat(retrievedData).hasSize(5);
	}

	@Test
	public void extractSensitiveData_only_present_tags_included() {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.PatientName, VR.PN, "TEST^PRENOM");

		Map<String, String> retrievedData = SensitiveTagDefinition.extractSensitiveData(attributes);

		assertThat(retrievedData).containsEntry("PatientName", "TEST^PRENOM");
		assertThat(retrievedData).doesNotContainKey("PatientID");
	}

	@Test
	public void extractSensitiveData_no_tags_should_return_empty() {
		Attributes attributes = new Attributes();

		Map<String, String> retrievedData = SensitiveTagDefinition.extractSensitiveData(attributes);

		assertThat(retrievedData).isEmpty();
	}
}
