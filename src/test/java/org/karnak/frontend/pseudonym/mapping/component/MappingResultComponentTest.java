/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.pseudonym.mapping.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.karnak.backend.cache.Patient;

class MappingResultComponentTest {

	@Test
	void should_create_mapping_result_component() {
		// Call constructor
		MappingResultComponent mappingResultComponent = new MappingResultComponent();

		// Test results
		assertNotNull(mappingResultComponent);
		assertNotNull(mappingResultComponent.getPatientFoundDetails());
	}

	@Test
	void should_handle_result_patient_found() {
		// Init data
		Patient patient = new Patient("pseudonym", "patientId", "patientFirstName", "patientLastName",
				"issuerOfPatientId", 1L);

		// Call method
		MappingResultComponent mappingResultComponent = new MappingResultComponent();
		mappingResultComponent.handleResultFindPatient(patient, "PSEUDO", "Cache");

		// Test results
		assertEquals("Cache", mappingResultComponent.getPatientFoundDetails().getSummaryText());
		assertNotNull(mappingResultComponent.getPatientFoundDetails().getContent());
	}

}
