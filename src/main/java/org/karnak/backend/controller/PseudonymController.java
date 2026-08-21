/*
 * Copyright (c) 2024-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import org.karnak.backend.constant.ApiVersion;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.model.patient.PatientModel;
import org.karnak.backend.cache.PseudonymCache;
import org.karnak.backend.constant.EndPoint;

import org.karnak.backend.service.ProjectService;
import org.karnak.backend.util.PatientClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller managing pseudonyms. 
 */
@RestController
@RequestMapping(EndPoint.PSEUDONYMS_PATH)
@Tag(name = "Pseudonym", description = "API Endpoints for pseudonyms")
public class PseudonymController {

	private final PseudonymCache pseudonymCache;

	private final ProjectService projectService;

	@Autowired
	public PseudonymController(final PseudonymCache pseudonymCache, final ProjectService projectService) {
		this.pseudonymCache = pseudonymCache;
		this.projectService = projectService;
	}

	@Operation(summary = "List all pseudonyms")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Pseudonyms found"),
			@ApiResponse(responseCode = "204", description = "No pseudonym configured", content = @Content) })
	@GetMapping(produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_read')")
	public ResponseEntity<List<PatientModel>> retrieveAllPatients() {
		List<PatientModel> patients = pseudonymCache.getAll().stream().toList();
		if (patients.isEmpty()) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(patients);
	}

	@Operation(summary = "Get a patient by pseudonym")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Patient found"),
			@ApiResponse(responseCode = "404", description = "Patient not found", content = @Content) })
	@GetMapping(value = "/{pseudonym}", produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_read')")
	public ResponseEntity<PatientModel> retrievePatient(@PathVariable("pseudonym") String pseudonym) {
        return pseudonymCache.findPatientByPseudonym(pseudonym)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
    }

	@Operation(summary = "Create a pseudonym",
			description = "The pseudonym will be stored in the external ID cache for the given project.")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Pseudonym created"),
			@ApiResponse(responseCode = "400",
					description = "Missing or invalid pseudonym, patientId, or projectUUID",
					content = @Content),
			@ApiResponse(responseCode = "404", description = "Project not found", content = @Content),
			@ApiResponse(responseCode = "409",
					description = "A pseudonym already exists for this patient and project",
					content = @Content) })
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_create')")
	public ResponseEntity<PatientModel> createPatient(@Valid @RequestBody PatientModel patientModel) {
		ProjectEntity existing = projectService.retrieveProjectByUuid(patientModel.getProjectUUID());
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		else {
			patientModel.setProjectID(existing.getId());
		}
		String key = PatientClientUtil.generateKey(patientModel, patientModel.getProjectID());
		PatientModel previous = pseudonymCache.put(key, patientModel);
		if (previous != null) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(previous);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(patientModel);
	}

	@Operation(summary = "Update a pseudonym/patient",
			description = "Update the patient identified by pseudonym.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Patient updated"),
			@ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
			@ApiResponse(responseCode = "404", description = "Patient not found", content = @Content) })
	@PutMapping(value = "/{pseudonym}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = org.karnak.backend.constant.ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_update')")
	public ResponseEntity<PatientModel> updatePatient(@PathVariable("pseudonym") String pseudonym,
			@Valid @RequestBody PatientModel patientModel) {
		Optional<PatientModel> existingOpt = pseudonymCache.findPatientByPseudonym(pseudonym);
		if (existingOpt.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		PatientModel existing = existingOpt.get();
		String oldKey = PatientClientUtil.generateKey(existing, existing.getProjectID());
		pseudonymCache.remove(oldKey);
		existing.updatePatientModel(patientModel);
		pseudonymCache.put(PatientClientUtil.generateKey(existing, existing.getProjectID()), existing);
		return ResponseEntity.ok(existing);
	}

	@Operation(summary = "Delete a pseudonym")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Patient deleted"),
			@ApiResponse(responseCode = "404", description = "Patient not found", content = @Content) })
	@DeleteMapping(value = "/{pseudonym}", produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_delete')")
	public ResponseEntity<Void> deletePatient(@PathVariable("pseudonym") String pseudonym) {
		Optional<PatientModel> patientOpt = pseudonymCache.findPatientByPseudonym(pseudonym);
		if (patientOpt.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		PatientModel patient = patientOpt.get();
		pseudonymCache.remove(PatientClientUtil.generateKey(patient, patient.getProjectID()));
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Delete all pseudonym for a project")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "All pseudonym deleted"),
			@ApiResponse(responseCode = "404", description = "No project found", content = @Content) })
	@DeleteMapping(value = "/project/{projectUUID}", produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_delete')")
	public ResponseEntity<Void> deleteAllPseudonymByProject(@NotNull @PathVariable("projectUUID") UUID projectUUID) {
		ProjectEntity projectFound = projectService.retrieveProjectByUuid(projectUUID);
		if (projectFound == null) {
			return ResponseEntity.notFound().build();
		}
		pseudonymCache.removeAllPseudonymByProject(projectFound);
		return ResponseEntity.noContent().build();
	}

}
