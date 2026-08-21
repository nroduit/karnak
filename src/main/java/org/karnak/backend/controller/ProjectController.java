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
import java.util.Objects;
import java.util.UUID;

import org.karnak.backend.constant.ApiVersion;
import org.karnak.backend.constant.EndPoint;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.entity.SecretEntity;
import org.karnak.backend.model.project.ProjectMapper;
import org.karnak.backend.model.project.ProjectModel;
import org.karnak.backend.model.project.SecretMapper;
import org.karnak.backend.model.project.SecretModel;
import org.karnak.backend.service.ProjectService;
import org.karnak.backend.service.SecretService;
import org.karnak.backend.service.profilepipe.ProfilePipeService;
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
 * Rest controller managing projects (a de-identification profile applied to one or more
 * destinations). Only {@link ProjectModel} (never the JPA entity) is exposed at the REST
 * boundary, decoupling the wire format from the persistence model.
 */
@RestController
@RequestMapping(EndPoint.PROJECTS_PATH)
@Tag(name = "Project", description = "API Endpoints for Projects")
public class ProjectController {

	private final ProjectService projectService;

	private final ProfilePipeService profilePipeService;

	private final SecretService secretService;

	@Autowired
	public ProjectController(final ProjectService projectService, final ProfilePipeService profilePipeService,
			final SecretService secretService) {
		this.projectService = projectService;
		this.profilePipeService = profilePipeService;
		this.secretService = secretService;
	}

	@Operation(summary = "List all projects")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Projects found"),
			@ApiResponse(responseCode = "204", description = "No projects configured", content = @Content) })
	@GetMapping(produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_read')")
	public ResponseEntity<List<ProjectModel>> retrieveAllProjects() {
		List<ProjectEntity> projects = projectService.retrieveAllProjects();
		if (projects.isEmpty()) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(projects.stream().map(ProjectMapper::toModel).toList());
	}

	@Operation(summary = "Create a project",
			description = "The profileUuid, when provided, must reference an existing profile.")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Project created"),
			@ApiResponse(responseCode = "400",
					description = "Missing or invalid name, or profileUuid does not reference an existing profile",
					content = @Content) })
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_create')")
	public ResponseEntity<ProjectModel> createProject(@Valid @RequestBody ProjectModel projectModel) {
		ProfileEntity profile = null;
		if (projectModel.getProfileUuid() != null) {
			profile = profilePipeService.retrieveProfileByUuid(projectModel.getProfileUuid());
			if (profile == null) {
				return ResponseEntity.badRequest().build();
			}
		}
		ProjectEntity entity = ProjectMapper.toEntity(projectModel);
		entity.setProfileEntity(profile);
		ProjectEntity saved = projectService.save(entity);
		return ResponseEntity.status(HttpStatus.CREATED).body(ProjectMapper.toModel(saved));
	}

	@Operation(summary = "Get a project by uuid")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Project found"),
			@ApiResponse(responseCode = "404", description = "Project not found", content = @Content) })
	@GetMapping(value = "/{projectUuid}", produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_read')")
	public ResponseEntity<ProjectModel> retrieveProject(@PathVariable("projectUuid") UUID projectUuid) {
		ProjectEntity project = projectService.retrieveProjectByUuid(projectUuid);
		return project == null ? ResponseEntity.notFound().build()
				: ResponseEntity.ok(ProjectMapper.toModel(project));
	}

	@Operation(summary = "Update a project",
			description = "Only name and profileUuid are updatable. Destinations and secrets are preserved "
					+ "(manage them via their own endpoints). ProfileUuid must reference an existing "
					+ "profile.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Project updated"),
			@ApiResponse(responseCode = "400", description = "profileUuid does not reference an existing profile",
					content = @Content),
			@ApiResponse(responseCode = "404", description = "Project not found", content = @Content) })
	@PutMapping(value = "/{projectUuid}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_update')")
	public ResponseEntity<ProjectModel> updateProject(@PathVariable("projectUuid") UUID projectUuid,
			@Valid @RequestBody ProjectModel projectModel) {
		ProjectEntity existing = projectService.retrieveProjectByUuid(projectUuid);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		// Merge only name/profile onto existing entity so that the associated
		// destinations and secrets (cascade / FK-mapped collections) are not silently
		// wiped out by an incoming partial payload.
		if (projectModel.getName() != null && !projectModel.getName().isBlank()) {
			existing.setName(projectModel.getName());
		}

		ProfileEntity profile = profilePipeService.retrieveProfileByUuid(projectModel.getProfileUuid());
		if (profile == null) {
			return ResponseEntity.badRequest().build();
		}
		existing.setProfileEntity(profile);

		projectService.update(existing);
		return ResponseEntity.ok(ProjectMapper.toModel(existing));
	}

	@Operation(summary = "Delete a project")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Project deleted"),
			@ApiResponse(responseCode = "404", description = "Project not found", content = @Content) })
	@DeleteMapping(value = "/{projectUuid}", produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_delete')")
	public ResponseEntity<Void> deleteProject(@PathVariable("projectUuid") UUID projectUuid) {
		ProjectEntity existing = projectService.retrieveProjectByUuid(projectUuid);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		projectService.remove(existing);
		return ResponseEntity.noContent().build();
	}

	// ==================== Secrets ====================

	@Operation(summary = "List secrets of a project")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Secrets found"),
			@ApiResponse(responseCode = "204", description = "No secrets", content = @Content),
			@ApiResponse(responseCode = "404", description = "Project not found", content = @Content) })
	@GetMapping(value = "/{projectUuid}/secrets", produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_read')")
	public ResponseEntity<List<SecretModel>> listSecrets(@PathVariable("projectUuid") UUID projectUuid) {
		ProjectEntity project = projectService.retrieveProjectByUuid(projectUuid);
		if (project == null) {
			return ResponseEntity.notFound().build();
		}
		List<SecretEntity> secrets = secretService.findSecretsByProject(project);
		if (secrets.isEmpty()) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(secrets.stream().map(SecretMapper::toModel).toList());
	}

	@Operation(summary = "Generate a new secret for a project",
			description = "Generates a random HMAC key and activates it (deactivating previous ones).")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Secret generated"),
			@ApiResponse(responseCode = "404", description = "Project not found", content = @Content) })
	@PostMapping(value = "/{projectUuid}/secrets/generate", produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_create')")
	public ResponseEntity<SecretModel> generateSecret(@PathVariable("projectUuid") UUID projectUuid) {
		ProjectEntity project = projectService.retrieveProjectByUuid(projectUuid);
		if (project == null) {
			return ResponseEntity.notFound().build();
		}
		SecretEntity secret = secretService.generateSecret(project);
		projectService.update(project);
		return ResponseEntity.status(HttpStatus.CREATED).body(SecretMapper.toModel(secret));
	}

	@Operation(summary = "Import a secret for a project",
			description = "Imports a hex key (32 hex chars, dashed groups allowed) and activates it.")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Secret imported"),
			@ApiResponse(responseCode = "400", description = "Invalid key", content = @Content),
			@ApiResponse(responseCode = "404", description = "Project not found", content = @Content) })
	@PostMapping(value = "/{projectUuid}/secrets/import", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_create')")
	public ResponseEntity<SecretModel> importSecret(@PathVariable("projectUuid") UUID projectUuid,
			@Valid @RequestBody SecretModel secretModel) {
		ProjectEntity project = projectService.retrieveProjectByUuid(projectUuid);
		if (project == null) {
			return ResponseEntity.notFound().build();
		}
		try {
			SecretEntity secret = secretService.importSecret(project, secretModel.getKey());
			projectService.update(project);
			return ResponseEntity.status(HttpStatus.CREATED).body(SecretMapper.toModel(secret));
		} catch (IllegalArgumentException _) {
			return ResponseEntity.badRequest().build();
		}
	}

	@Operation(summary = "Delete a secret")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Secret deleted"),
			@ApiResponse(responseCode = "404", description = "Secret not found", content = @Content) })
	@DeleteMapping(value = "/{projectUuid}/secrets/{secretUuid}", produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_delete')")
	public ResponseEntity<Void> deleteSecret(@PathVariable("projectUuid") UUID projectUuid,
			@PathVariable("secretUuid") UUID secretUuid) {
		ProjectEntity project = projectService.retrieveProjectByUuid(projectUuid);
		if (project == null) {
			return ResponseEntity.notFound().build();
		}
		SecretEntity secret = secretService.findSecretByUuid(secretUuid);
		if (secret == null || secret.getProjectEntity() == null
				|| !Objects.equals(project.getId(), secret.getProjectEntity().getId())) {
			return ResponseEntity.notFound().build();
		}
		secretService.deleteSecret(project, secret);
		return ResponseEntity.noContent().build();
	}

}
