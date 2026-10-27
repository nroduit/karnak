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
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.karnak.backend.cache.PseudonymCache;
import org.karnak.backend.constant.ApiVersion;
import org.karnak.backend.constant.EndPoint;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.model.patient.PatientModel;
import org.karnak.backend.model.patient.PseudonymImportResult;
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

	/**
	 * Validates each row of a bulk import on its own, so that one invalid row is reported
	 * instead of failing the whole batch the way {@code @Valid} on the request body
	 * would. The bean comes from {@link org.karnak.backend.config.ValidationConfig}.
	 */
	private final Validator validator;

	@Autowired
	public PseudonymController(final PseudonymCache pseudonymCache, final ProjectService projectService,
			final Validator validator) {
		this.pseudonymCache = pseudonymCache;
		this.projectService = projectService;
		this.validator = validator;
	}

	@Operation(summary = "List all pseudonyms")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Pseudonyms found"),
			@ApiResponse(responseCode = "204", description = "No pseudonym configured", content = @Content) })
	@GetMapping(produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyAuthority('collaborator','backend') AND hasAuthority('karnak_read')")
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
	@PreAuthorize("hasAnyAuthority('collaborator','backend') AND hasAuthority('karnak_read')")
	public ResponseEntity<PatientModel> retrievePatient(@PathVariable("pseudonym") String pseudonym) {
		return pseudonymCache.findPatientByPseudonym(pseudonym)
			.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.notFound().build());
	}

	@Operation(summary = "Create a pseudonym",
			description = "The pseudonym will be stored in the external ID cache for the given project.")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Pseudonym created"),
			@ApiResponse(responseCode = "400", description = "Missing or invalid pseudonym, patientId, or projectUUID",
					content = @Content),
			@ApiResponse(responseCode = "404", description = "Project not found", content = @Content),
			@ApiResponse(responseCode = "409", description = "A pseudonym already exists for this patient and project",
					content = @Content) })
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyAuthority('collaborator','backend') AND hasAuthority('karnak_create')")
	public ResponseEntity<PatientModel> createPatient(@Valid @RequestBody PatientModel patientModel) {
		CreateOutcome outcome = createOne(patientModel);
		return switch (outcome.status()) {
			case PROJECT_NOT_FOUND -> ResponseEntity.notFound().build();
			case CONFLICT -> ResponseEntity.status(HttpStatus.CONFLICT).body(outcome.existing());
			case CREATED -> ResponseEntity.status(HttpStatus.CREATED).body(outcome.patientModel());
		};
	}

	@Operation(summary = "Bulk-create pseudonyms",
			description = "Imports a JSON array of patients in one request, running each row through the same "
					+ "logic as the single-create endpoint. A partial failure never fails the whole request: "
					+ "invalid, unresolvable-project, or conflicting rows are reported in the response summary "
					+ "instead of aborting the batch.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200",
			description = "Import processed, see the summary for the per-row outcome") })
	@PostMapping(value = "/import", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_create')")
	public ResponseEntity<PseudonymImportResult> importPseudonyms(@RequestBody List<PatientModel> patientModels) {
		PseudonymImportResult result = new PseudonymImportResult();
		for (int index = 0; index < patientModels.size(); index++) {
			PatientModel patientModel = patientModels.get(index);
			if (patientModel == null) {
				result.addError(index, "Row is null");
				continue;
			}
			Set<ConstraintViolation<PatientModel>> violations = validator.validate(patientModel);
			if (!violations.isEmpty()) {
				String reason = violations.stream()
					.map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
					.collect(Collectors.joining("; "));
				result.addError(index, reason);
				continue;
			}
			try {
				CreateOutcome outcome = createOne(patientModel);
				switch (outcome.status()) {
					case CREATED -> result.addCreated();
					case CONFLICT -> result.addConflict(index,
							"A pseudonym already exists for this patient and project", outcome.existing());
					case PROJECT_NOT_FOUND -> result.addError(index, "Project not found");
				}
			}
			catch (RuntimeException e) {
				result.addError(index, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
			}
		}
		return ResponseEntity.ok(result);
	}

	/**
	 * Shared create path used by both the single-create endpoint and the bulk import, so
	 * their semantics (project lookup, key generation, conflict detection) match exactly.
	 */
	private CreateOutcome createOne(PatientModel patientModel) {
		ProjectEntity project = projectService.retrieveProjectByUuid(patientModel.getProjectUUID());
		if (project == null) {
			return new CreateOutcome(CreateStatus.PROJECT_NOT_FOUND, null, null);
		}
		patientModel.setProjectID(project.getId());
		String key = PatientClientUtil.generateKey(patientModel, patientModel.getProjectID());
		PatientModel previous = pseudonymCache.put(key, patientModel);
		if (previous != null) {
			return new CreateOutcome(CreateStatus.CONFLICT, previous, null);
		}
		return new CreateOutcome(CreateStatus.CREATED, null, patientModel);
	}

	private enum CreateStatus {

		CREATED, CONFLICT, PROJECT_NOT_FOUND

	}

	private record CreateOutcome(CreateStatus status, PatientModel existing, PatientModel patientModel) {
	}

	@Operation(summary = "Update a pseudonym/patient", description = "Update the patient identified by pseudonym.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Patient updated"),
			@ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content),
			@ApiResponse(responseCode = "404", description = "Patient not found", content = @Content) })
	@PutMapping(value = "/{pseudonym}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = org.karnak.backend.constant.ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyAuthority('collaborator','backend') AND hasAuthority('karnak_update')")
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
	@PreAuthorize("hasAnyAuthority('collaborator','backend') AND hasAuthority('karnak_delete')")
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
	@PreAuthorize("hasAnyAuthority('collaborator','backend') AND hasAuthority('karnak_delete')")
	public ResponseEntity<Void> deleteAllPseudonymByProject(@NotNull @PathVariable("projectUUID") UUID projectUUID) {
		ProjectEntity projectFound = projectService.retrieveProjectByUuid(projectUUID);
		if (projectFound == null) {
			return ResponseEntity.notFound().build();
		}
		pseudonymCache.removeAllPseudonymByProject(projectFound);
		return ResponseEntity.noContent().build();
	}

}
