/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.controller;

import jakarta.validation.Validation;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.cache.PseudonymCache;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.model.patient.PatientModel;
import org.karnak.backend.model.patient.PseudonymImportResult;
import org.karnak.backend.service.ProjectService;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Unit test for {@link PseudonymController}, focused on the bulk import endpoint. Calls
 * the controller directly against mocked collaborators and a real Bean Validation
 * validator, mirroring {@code ProjectServiceTest}'s direct-unit-test style.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class PseudonymControllerTest {

	private final PseudonymCache pseudonymCacheMock = Mockito.mock(PseudonymCache.class);

	private final ProjectService projectServiceMock = Mockito.mock(ProjectService.class);

	private PseudonymController pseudonymController;

	private static final UUID PROJECT_UUID = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		pseudonymController = new PseudonymController(pseudonymCacheMock, projectServiceMock,
				Validation.buildDefaultValidatorFactory().getValidator());

		ProjectEntity project = new ProjectEntity();
		project.setId(42L);
		Mockito.when(projectServiceMock.retrieveProjectByUuid(PROJECT_UUID)).thenReturn(project);
	}

	private static PatientModel patient(String pseudonym, String patientId) {
		PatientModel patientModel = new PatientModel();
		patientModel.setPseudonym(pseudonym);
		patientModel.setPatientId(patientId);
		patientModel.setProjectUUID(PROJECT_UUID);
		return patientModel;
	}

	@Test
	void createPatient_persists_and_returns_201() {
		Mockito.when(pseudonymCacheMock.put(Mockito.anyString(), Mockito.any())).thenReturn(null);

		ResponseEntity<PatientModel> response = pseudonymController.createPatient(patient("PSY_1", "PAT_1"));

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
	}

	@Test
	void createPatient_conflicts_and_returns_the_existing_patient() {
		PatientModel existing = patient("PSY_1", "PAT_1");
		Mockito.when(pseudonymCacheMock.put(Mockito.anyString(), Mockito.any())).thenReturn(existing);

		ResponseEntity<PatientModel> response = pseudonymController.createPatient(patient("PSY_1", "PAT_1"));

		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertEquals(existing, response.getBody());
	}

	@Test
	void importPseudonyms_reports_created_conflict_and_error_rows_without_failing_the_request() {
		PatientModel created = patient("PSY_1", "PAT_1");
		PatientModel conflicting = patient("PSY_2", "PAT_2");
		PatientModel existing = patient("PSY_2", "PAT_2_EXISTING");
		// Missing pseudonym and patientId: validation error
		PatientModel invalid = new PatientModel();
		PatientModel unknownProject = patient("PSY_4", "PAT_4");
		unknownProject.setProjectUUID(UUID.randomUUID());

		// First row: created. Second row: conflicts with an existing pseudonym.
		Mockito.when(pseudonymCacheMock.put(Mockito.anyString(), Mockito.any())).thenReturn(null).thenReturn(existing);

		ResponseEntity<PseudonymImportResult> response = pseudonymController
			.importPseudonyms(List.of(created, conflicting, invalid, unknownProject));

		assertEquals(HttpStatus.OK, response.getStatusCode());
		PseudonymImportResult result = response.getBody();
		assertNotNull(result);
		assertEquals(1, result.getCreated());
		assertEquals(1, result.getConflict());
		assertEquals(2, result.getError());
		assertEquals(3, result.getIssues().size());

		// Conflict row carries its index and the existing patient.
		assertEquals(1, result.getIssues().get(0).getIndex());
		assertEquals(existing, result.getIssues().get(0).getExisting());

		// Validation-error row.
		assertEquals(2, result.getIssues().get(1).getIndex());

		// Unresolvable-project row.
		assertEquals(3, result.getIssues().get(2).getIndex());
		assertEquals("Project not found", result.getIssues().get(2).getReason());
	}

	@Test
	void importPseudonyms_matches_single_create_semantics_for_a_conflict() {
		// Same row submitted through both endpoints must land in the same outcome
		// (409-equivalent: conflict reported, existing patient returned).
		PatientModel existing = patient("PSY_1", "PAT_1");
		Mockito.when(pseudonymCacheMock.put(Mockito.anyString(), Mockito.any())).thenReturn(existing);

		ResponseEntity<PseudonymImportResult> response = pseudonymController
			.importPseudonyms(List.of(patient("PSY_1", "PAT_1")));

		PseudonymImportResult result = response.getBody();
		assertNotNull(result);
		assertEquals(0, result.getCreated());
		assertEquals(1, result.getConflict());
		assertEquals(existing, result.getIssues().get(0).getExisting());
	}

}
