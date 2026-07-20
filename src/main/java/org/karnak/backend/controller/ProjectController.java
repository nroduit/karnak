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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.karnak.backend.cache.Patient;
import org.karnak.backend.cache.PatientClient;
import org.karnak.backend.constant.EndPoint;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.entity.SecretEntity;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.service.ProjectService;
import org.karnak.backend.service.profilepipe.ProfilePipeService;
import org.karnak.backend.util.PatientClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller managing projects (de-identification configuration) and their external
 * IDs
 */
@RestController
@RequestMapping(EndPoint.PROJECTS_PATH)
@Tag(name = "Project", description = "API Endpoints for Projects and External IDs")
public class ProjectController {

	private final ProjectService projectService;

	private final ProfilePipeService profilePipeService;

	private final PatientClient patientClient;

	@Autowired
	public ProjectController(final ProjectService projectService, final ProfilePipeService profilePipeService,
			@Qualifier("patientClient") final PatientClient patientClient) {
		this.projectService = projectService;
		this.profilePipeService = profilePipeService;
		this.patientClient = patientClient;
	}

	// ======== Projects ========

	@Operation(summary = "List all projects")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Projects found"),
			@ApiResponse(responseCode = "204", description = "No projects", content = @Content) })
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<ProjectEntity>> getAllProjects() {
		List<ProjectEntity> projects = projectService.getAllProjects();
		return projects.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(projects);
	}

	@Operation(summary = "Create a project", description = "Body: {\"name\": \"...\", \"profileId\": 1 (optional)}")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Project created"),
			@ApiResponse(responseCode = "400", description = "Missing name or invalid profileId", content = @Content),
			@ApiResponse(responseCode = "404", description = "Profile not found", content = @Content) })
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ProjectEntity> createProject(@RequestBody Map<String, Object> body) {
		String name = (String) body.get("name");
		if (name == null || name.isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		ProjectEntity project = new ProjectEntity();
		project.setName(name);
		if (body.get("profileId") != null) {
			Long profileId;
			try {
				profileId = Long.valueOf(body.get("profileId").toString());
			}
			catch (NumberFormatException e) {
				return ResponseEntity.badRequest().build();
			}
			ProfileEntity profile = findProfileById(profileId);
			if (profile == null) {
				return ResponseEntity.notFound().build();
			}
			project.setProfileEntity(profile);
		}
		ProjectEntity saved = projectService.save(project);
		return ResponseEntity.status(201).body(saved);
	}

	private ProfileEntity findProfileById(Long profileId) {
		return profilePipeService.getAllProfiles()
			.stream()
			.filter(p -> p.getId().equals(profileId))
			.findFirst()
			.orElse(null);
	}

	@Operation(summary = "Get a project by ID")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Project found"),
			@ApiResponse(responseCode = "404", description = "Project not found", content = @Content) })
	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ProjectEntity> getProject(@PathVariable Long id) {
		ProjectEntity project = projectService.retrieveProject(id);
		return project == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(project);
	}

	@Operation(summary = "Update a project",
			description = "Body: {\"name\": \"...\", \"profileId\": 1 (optional, null to unset)}")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Project updated"),
			@ApiResponse(responseCode = "404", description = "Project or profile not found", content = @Content) })
	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ProjectEntity> updateProject(@PathVariable Long id, @RequestBody Map<String, Object> body) {
		ProjectEntity project = projectService.retrieveProject(id);
		if (project == null) {
			return ResponseEntity.notFound().build();
		}
		if (body.containsKey("name")) {
			project.setName((String) body.get("name"));
		}
		if (body.containsKey("profileId")) {
			Object profileIdRaw = body.get("profileId");
			if (profileIdRaw == null) {
				project.setProfileEntity(null);
			}
			else {
				Long profileId;
				try {
					profileId = Long.valueOf(profileIdRaw.toString());
				}
				catch (NumberFormatException e) {
					return ResponseEntity.badRequest().build();
				}
				ProfileEntity profile = findProfileById(profileId);
				if (profile == null) {
					return ResponseEntity.notFound().build();
				}
				project.setProfileEntity(profile);
			}
		}
		projectService.update(project);
		return ResponseEntity.ok(project);
	}

	@Operation(summary = "Delete a project")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Project deleted"),
			@ApiResponse(responseCode = "404", description = "Project not found", content = @Content) })
	@DeleteMapping(value = "/{id}")
	public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
		ProjectEntity project = projectService.retrieveProject(id);
		if (project == null) {
			return ResponseEntity.notFound().build();
		}
		projectService.remove(project);
		return ResponseEntity.noContent().build();
	}

	// ======== Secrets ========

	@Operation(summary = "Add or generate a HMAC secret for a project",
			description = "Body: {\"hexKey\": \"...\"} — omit hexKey for auto-generation. "
					+ "The key must be a 32-char hex string (16 bytes).")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Secret added"),
			@ApiResponse(responseCode = "400", description = "Invalid hex key format", content = @Content),
			@ApiResponse(responseCode = "404", description = "Project not found", content = @Content) })
	@PostMapping(value = "/{id}/secrets", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> addSecret(@PathVariable Long id,
			@RequestBody(required = false) Map<String, String> body) {
		ProjectEntity project = projectService.retrieveProject(id);
		if (project == null) {
			return ResponseEntity.notFound().build();
		}
		byte[] keyBytes;
		String hexInput = body != null ? body.get("hexKey") : null;
		if (hexInput != null && !hexInput.isBlank()) {
			String cleaned = hexInput.replace("-", "");
			if (cleaned.length() != HMAC.KEY_BYTE_LENGTH * 2) {
				return ResponseEntity.badRequest()
					.body(Map.of("error",
							"Invalid hex key: expected " + (HMAC.KEY_BYTE_LENGTH * 2) + " hex characters"));
			}
			try {
				keyBytes = HexFormat.of().parseHex(cleaned);
			}
			catch (IllegalArgumentException e) {
				return ResponseEntity.badRequest().body(Map.of("error", "Invalid hex key: " + e.getMessage()));
			}
		}
		else {
			keyBytes = HMAC.generateRandomKey();
		}
		SecretEntity secret = new SecretEntity(keyBytes);
		project.addActiveSecretEntity(secret);
		ProjectEntity updated = projectService.save(project);
		SecretEntity activeSecret = updated.retrieveActiveSecret();
		String hexKey = HMAC.byteToHex(activeSecret.getSecretKey());
		return ResponseEntity.status(201)
			.body(Map.of("projectId", id, "hexKey", hexKey, "displayKey", HMAC.showHexKey(hexKey), "active", true));
	}

	// ======== External IDs ========

	@Operation(summary = "List external ID mappings for a project",
			description = "Returns the in-memory pseudonym→patientId cache for the given project")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Patients found"),
			@ApiResponse(responseCode = "404", description = "Project not found", content = @Content) })
	@GetMapping(value = "/{id}/external-ids", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Collection<Patient>> getExternalIds(@PathVariable Long id) {
		if (projectService.retrieveProject(id) == null) {
			return ResponseEntity.notFound().build();
		}
		List<Patient> patients = patientClient.getAll().stream().filter(p -> id.equals(p.getProjectID())).toList();
		return ResponseEntity.ok(patients);
	}

	@Operation(summary = "Add a patient external ID mapping",
			description = "Body: {\"pseudonym\": \"...\", \"patientId\": \"...\", \"patientFirstName\": \"...\","
					+ " \"patientLastName\": \"...\", \"issuerOfPatientId\": \"...\","
					+ " \"patientBirthDate\": \"YYYY-MM-DD\" (opt), \"patientSex\": \"M/F\" (opt)}")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Patient mapping added"),
			@ApiResponse(responseCode = "400", description = "Missing required fields", content = @Content),
			@ApiResponse(responseCode = "404", description = "Project not found", content = @Content),
			@ApiResponse(responseCode = "409", description = "Patient already exists", content = @Content) })
	@PostMapping(value = "/{id}/external-ids", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Patient> addExternalId(@PathVariable Long id, @RequestBody Map<String, String> body) {
		if (projectService.retrieveProject(id) == null) {
			return ResponseEntity.notFound().build();
		}
		String pseudonym = body.get("pseudonym");
		String patientId = body.get("patientId");
		if (pseudonym == null || pseudonym.isBlank() || patientId == null || patientId.isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		String firstName = body.getOrDefault("patientFirstName", "");
		String lastName = body.getOrDefault("patientLastName", "");
		String issuer = body.getOrDefault("issuerOfPatientId", "");
		LocalDate birthDate;
		try {
			birthDate = parseOptionalDate(body.get("patientBirthDate"));
		}
		catch (DateTimeParseException e) {
			return ResponseEntity.badRequest().build();
		}
		String sex = body.getOrDefault("patientSex", "");
		Patient patient = new Patient(pseudonym, patientId, firstName, lastName, birthDate, sex, issuer);
		patient.setProjectID(id);
		String key = PatientClientUtil.generateKey(patient, id);
		Patient existing = patientClient.put(key, patient);
		if (existing != null) {
			return ResponseEntity.status(409).build();
		}
		return ResponseEntity.status(201).body(patient);
	}

	private static LocalDate parseOptionalDate(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return LocalDate.parse(value);
	}

	@Operation(summary = "Update a patient external ID mapping",
			description = "patientId and issuerOfPatientId identify the existing record. "
					+ "Body contains fields to update.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Patient updated"),
			@ApiResponse(responseCode = "404", description = "Project or patient not found", content = @Content) })
	@PutMapping(value = "/{id}/external-ids/{patientId}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Patient> updateExternalId(@PathVariable Long id, @PathVariable String patientId,
			@RequestParam(defaultValue = "") String issuerId, @RequestBody Map<String, String> body) {
		if (projectService.retrieveProject(id) == null) {
			return ResponseEntity.notFound().build();
		}
		String oldKey = PatientClientUtil.generateKey(patientId, issuerId) + id;
		Patient existing = patientClient.get(oldKey);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		patientClient.remove(oldKey);
		if (body.containsKey("pseudonym")) {
			existing.setPseudonym(body.get("pseudonym"));
		}
		if (body.containsKey("patientId")) {
			existing.setPatientId(body.get("patientId"));
		}
		if (body.containsKey("patientFirstName")) {
			existing.updatePatientFirstName(body.get("patientFirstName"));
		}
		if (body.containsKey("patientLastName")) {
			existing.updatePatientLastName(body.get("patientLastName"));
		}
		if (body.containsKey("issuerOfPatientId")) {
			existing.setIssuerOfPatientId(body.get("issuerOfPatientId"));
		}
		if (body.containsKey("patientBirthDate")) {
			try {
				existing.setPatientBirthDate(parseOptionalDate(body.get("patientBirthDate")));
			}
			catch (DateTimeParseException e) {
				// Re-insert the original record before bailing so the in-memory cache
				// state
				// is restored — the remove() above must not be observed as a side-effect
				// of
				// a malformed update payload.
				patientClient.put(oldKey, existing);
				return ResponseEntity.badRequest().build();
			}
		}
		if (body.containsKey("patientSex")) {
			existing.setPatientSex(body.get("patientSex"));
		}
		patientClient.put(PatientClientUtil.generateKey(existing, id), existing);
		return ResponseEntity.ok(existing);
	}

	@Operation(summary = "Delete a patient external ID mapping",
			description = "patientId path var; use ?issuerId= to disambiguate if needed")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Patient deleted"),
			@ApiResponse(responseCode = "404", description = "Project or patient not found", content = @Content) })
	@DeleteMapping(value = "/{id}/external-ids/{patientId}")
	public ResponseEntity<Void> deleteExternalId(@PathVariable Long id, @PathVariable String patientId,
			@RequestParam(defaultValue = "") String issuerId) {
		if (projectService.retrieveProject(id) == null) {
			return ResponseEntity.notFound().build();
		}
		String key = PatientClientUtil.generateKey(patientId, issuerId) + id;
		if (patientClient.get(key) == null) {
			return ResponseEntity.notFound().build();
		}
		patientClient.remove(key);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Delete all patient external ID mappings for a project")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "All patients deleted"),
			@ApiResponse(responseCode = "404", description = "Project not found", content = @Content) })
	@DeleteMapping(value = "/{id}/external-ids")
	public ResponseEntity<Void> deleteAllExternalIds(@PathVariable Long id) {
		if (projectService.retrieveProject(id) == null) {
			return ResponseEntity.notFound().build();
		}
		List<Patient> patients = patientClient.getAll().stream().filter(p -> id.equals(p.getProjectID())).toList();
		patients.forEach(p -> patientClient.remove(PatientClientUtil.generateKey(p, id)));
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Bulk import patient external ID mappings",
			description = "Body: JSON array of patient objects. Each must have pseudonym and patientId.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Import complete with result summary"),
			@ApiResponse(responseCode = "404", description = "Project not found", content = @Content) })
	@PostMapping(value = "/{id}/external-ids/import", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> importExternalIds(@PathVariable Long id,
			@RequestBody List<Map<String, String>> patients) {
		if (projectService.retrieveProject(id) == null) {
			return ResponseEntity.notFound().build();
		}
		int added = 0;
		int skipped = 0;
		for (Map<String, String> p : patients) {
			String pseudonym = p.get("pseudonym");
			String patientId = p.get("patientId");
			if (pseudonym == null || pseudonym.isBlank() || patientId == null || patientId.isBlank()) {
				skipped++;
				continue;
			}
			String issuer = p.getOrDefault("issuerOfPatientId", "");
			String firstName = p.getOrDefault("patientFirstName", "");
			String lastName = p.getOrDefault("patientLastName", "");
			LocalDate birthDate;
			try {
				birthDate = parseOptionalDate(p.get("patientBirthDate"));
			}
			catch (DateTimeParseException e) {
				skipped++;
				continue;
			}
			String sex = p.getOrDefault("patientSex", "");
			Patient patient = new Patient(pseudonym, patientId, firstName, lastName, birthDate, sex, issuer);
			patient.setProjectID(id);
			String key = PatientClientUtil.generateKey(patient, id);
			if (patientClient.put(key, patient) != null) {
				skipped++;
			}
			else {
				added++;
			}
		}
		return ResponseEntity.ok(Map.of("added", added, "skipped", skipped));
	}

}
