/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.extid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.cache.ExternalIDCache;
import org.karnak.backend.cache.Patient;
import org.karnak.backend.data.entity.ProjectEntity;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class ExternalIDGridTest {

	@Mock
	private ExternalIDCache externalIDCache; // Standalone Mockito mock replacing the
												// @MockBean

	@InjectMocks
	private ExternalIDGrid externalIDGrid; // Class under test, dependencies injected by
											// Mockito

	@BeforeEach
	void setUp() {
		// Initialize mocks and inject them into the tested object
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void should_create_external_id_grid() {
		// Test results
		assertNotNull(externalIDGrid);
	}

	@Test
	void should_read_cache() {
		// Mock
		Patient patient = new Patient("pseudonym", "patientId", "patientFirstName", "patientLastName",
				"issuerOfPatientId", 1L);

		when(externalIDCache.getAll()).thenReturn(List.of(patient));

		patient.setProjectID(1L);
		externalIDCache.put("key", patient);
		externalIDGrid.setExternalIDCache(externalIDCache);

		// set project
		ProjectEntity projectEntity = new ProjectEntity();
		projectEntity.setId(1L);
		externalIDGrid.setProjectEntity(projectEntity);

		// Call method
		externalIDGrid.readAllCacheValue();

		// Test results
		assertNotNull(externalIDGrid);
		assertNotNull(externalIDGrid.getExternalIDCache());
		assertNotNull(externalIDGrid.getProjectEntity());
		assertEquals("pseudonym", externalIDGrid.getPatientsListInCache().get(0).getPseudonym());
	}

	@Test
	void should_add_patient_and_check_existence() {

		// Mock
		Patient patient = new Patient("pseudonym", "patientId", "patientFirstName", "patientLastName",
				"issuerOfPatientId", 1L);
		when(externalIDCache.getAll()).thenReturn(List.of(patient));

		patient.setProjectID(1L);

		// set project
		ProjectEntity projectEntity = new ProjectEntity();
		projectEntity.setId(1L);
		externalIDGrid.setProjectEntity(projectEntity);
		//
		// Test no patient in cache
		assertFalse(externalIDGrid.patientExist(patient));

		// Mock
		when(externalIDCache.get(any())).thenReturn(patient);

		// Add patient
		externalIDGrid.addPatient(patient);
		//
		// Test patient in cache
		assertTrue(externalIDGrid.patientExist(patient));
	}

}
