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
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import org.karnak.backend.constant.ApiVersion;
import org.karnak.backend.constant.EndPoint;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.model.profile.ProfileElementMapper;
import org.karnak.backend.model.profile.ProfileElementModel;
import org.karnak.backend.model.profile.ProfileMapper;
import org.karnak.backend.model.profile.ProfileModel;
import org.karnak.backend.model.profilebody.ProfilePipeBody;
import org.karnak.backend.service.profilepipe.ProfilePipeService;
import org.karnak.frontend.profile.component.editprofile.ProfileYamlSerializer;
import org.karnak.frontend.profile.component.errorprofile.ProfileError;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Rest controller managing de-identification profiles and their profile elements. Only
 * {@link ProfileModel} / {@link ProfileElementModel} (never the JPA entities) are
 * exposed at the REST boundary, decoupling the wire format from the persistence model.
 */
@RestController
@RequestMapping(EndPoint.PROFILES_PATH)
@Tag(name = "Profile", description = "API Endpoints for Profiles")
public class ProfileController {

	/** Matches characters that are not safe to use as-is in a downloaded file name. */
	private static final Pattern UNSAFE_FILENAME_CHARS = Pattern.compile("[^a-zA-Z0-9._-]");

	private final ProfilePipeService profilePipeService;

	@Autowired
	public ProfileController(final ProfilePipeService profilePipeService) {
		this.profilePipeService = profilePipeService;
	}


	// ======== Profiles ========

	@Operation(summary = "List all profiles")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Profiles found"),
			@ApiResponse(responseCode = "204", description = "No profiles configured", content = @Content) })
	@GetMapping(produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_read')")
	public ResponseEntity<List<ProfileModel>> retrieveAllProfiles() {
		List<ProfileEntity> profiles = profilePipeService.retrieveAllProfiles();
		if (profiles.isEmpty()) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(profiles.stream().map(ProfileMapper::toModel).toList());
	}

	@Operation(summary = "Create a profile",
			description = "Creates a new, empty profile. Elements are then managed via their own endpoints. "
					+ "The same name can be reused across different versions; the (name, version) pair must be "
					+ "unique.")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Profile created"),
			@ApiResponse(responseCode = "400", description = "Missing or invalid name", content = @Content),
			@ApiResponse(responseCode = "409", description = "A profile with the same name and version already exists",
					content = @Content) })
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_create')")
	public ResponseEntity<ProfileModel> createProfile(@Valid @RequestBody ProfileModel profileModel) {
		if (profilePipeService.retrieveAllProfiles()
			.stream()
			.anyMatch(p -> sameNameAndVersion(p, profileModel.getName(), profileModel.getVersion()))) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ProfileMapper.toModel(
						profilePipeService.createEmptyProfile(profileModel.getName(),
							profileModel.getVersion(), profileModel.getMinimumKarnakVersion())));
	}

	@Operation(summary = "Get a profile by uuid")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Profile found"),
			@ApiResponse(responseCode = "404", description = "Profile not found", content = @Content) })
	@GetMapping(value = "/{profileUuid}", produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_read')")
	public ResponseEntity<ProfileModel> retrieveProfile(@PathVariable("profileUuid") UUID profileUuid) {
		ProfileEntity profile = profilePipeService.retrieveProfileByUuid(profileUuid);
		return profile == null ? ResponseEntity.notFound().build()
				: ResponseEntity.ok(ProfileMapper.toModel(profile));
	}

	@Operation(summary = "Update a profile",
			description = "Only name, version and minimumKarnakVersion are updatable. "
					+ "Profile elements are preserved (manage them via their own endpoints). "
					+ "The same name can be reused across different versions; the (name, version) pair must stay "
					+ "unique. Default (built-in) profiles cannot be updated.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Profile updated"),
			@ApiResponse(responseCode = "404", description = "Profile not found", content = @Content),
			@ApiResponse(responseCode = "409",
					description = "Profile is a default profile, or another profile with the same name and version "
							+ "already exists",
					content = @Content) })
	@PutMapping(value = "/{profileUuid}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_update')")
	public ResponseEntity<ProfileModel> updateProfile(@PathVariable("profileUuid") UUID profileUuid,
			@RequestBody ProfileModel profileModel) {
		ProfileEntity existing = profilePipeService.retrieveProfileByUuid(profileUuid);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		if (Boolean.TRUE.equals(existing.getByDefault())) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		// Merge only name/version/minimumKarnakVersion onto existing entity so that the
		// associated profile elements and masks (cascade=ALL + orphanRemoval) are not
		// silently wiped out by an incoming partial payload.
		String newName = profileModel.getName() != null && !profileModel.getName().isBlank() ? profileModel.getName()
				: existing.getName();
		String newVersion = profileModel.getVersion() != null ? profileModel.getVersion() : existing.getVersion();
		if (profilePipeService.retrieveAllProfiles()
			.stream()
			.anyMatch(p -> !Objects.equals(p.getId(), existing.getId()) && sameNameAndVersion(p, newName, newVersion))) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		existing.setName(newName);
		existing.setVersion(newVersion);
		if (profileModel.getMinimumKarnakVersion() != null) {
			existing.setMinimumKarnakVersion(profileModel.getMinimumKarnakVersion());
		}
		return ResponseEntity.ok(ProfileMapper.toModel(profilePipeService.updateProfile(existing)));
	}

	@Operation(summary = "Delete a profile")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Profile deleted"),
			@ApiResponse(responseCode = "400", description = "Profile is used by one or more projects", content = @Content),
			@ApiResponse(responseCode = "404", description = "Profile not found", content = @Content) })
	@DeleteMapping(value = "/{profileUuid}", produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_delete')")
	public ResponseEntity<?> deleteProfile(@PathVariable("profileUuid") UUID profileUuid) {
		ProfileEntity existing = profilePipeService.retrieveProfileByUuid(profileUuid);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		ProfilePipeService.DeleteProfileResult result = profilePipeService.deleteProfile(existing);
		if (!result.success()) {
			return ResponseEntity.badRequest().body(List.of(result.errorMessage()));
		}
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Upload (import) a profile from a YAML file",
			description = "Parses and validates the uploaded YAML profile (same format as the profile export/editor). "
					+ "The profile is only persisted when the YAML is well-formed, every profile element is valid, "
					+ "and no existing profile shares the same name and version.")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Profile imported"),
			@ApiResponse(responseCode = "400", description = "Missing file, invalid YAML or missing profile name/elements",
					content = @Content),
			@ApiResponse(responseCode = "409", description = "A profile with the same name and version already exists",
					content = @Content),
			@ApiResponse(responseCode = "422", description = "One or more profile elements failed validation",
					content = @Content) })
	@PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
			produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_create')")
	public ResponseEntity<?> importProfile(@RequestParam("file") MultipartFile file) {
		ProfilePipeBody profilePipeBody;
		if (file.isEmpty()) {
			return ResponseEntity.badRequest().body(List.of("No file was uploaded, or the file is empty."));
		}

		try (InputStream inputStream = file.getInputStream()) {
			profilePipeBody = readProfileYaml(inputStream);
		}
		catch (JsonProcessingException e) {
			return ResponseEntity.badRequest().body(List.of(formatYamlError(e)));
		}
		catch (IOException _) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(List.of("Unable to read the uploaded file."));
		}

		ResponseEntity<?> validationError = validateImportedProfile(profilePipeBody);
		if (validationError != null) {
			return validationError;
		}

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ProfileMapper.toModel(profilePipeService.saveProfilePipe(profilePipeBody, false)));
	}

	@Operation(summary = "Download a profile as a YAML file",
			description = "Returns the profile (metadata, masks and ordered elements) serialized as YAML, "
					+ "in the same format accepted by the import endpoint and the raw YAML editor.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Profile YAML file"),
			@ApiResponse(responseCode = "404", description = "Profile not found", content = @Content) })
	@GetMapping(value = "/{profileUuid}/download", produces = "application/x-yaml")
	@PreAuthorize("hasAuthority('karnak_read')")
	public ResponseEntity<String> downloadProfile(@PathVariable("profileUuid") UUID profileUuid) {
		ProfileEntity profile = profilePipeService.retrieveProfileByUuid(profileUuid);
		if (profile == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType("application/x-yaml"))
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\""
					.formatted(buildDownloadFileName(profile)))
			.body(ProfileYamlSerializer.toYaml(profile));
	}


	// ======== Profile Elements ========

	@Operation(summary = "List elements of a profile")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Profile elements found"),
			@ApiResponse(responseCode = "404", description = "Profile not found", content = @Content) })
	@GetMapping(value = "/{profileUuid}/elements", produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_read')")
	public ResponseEntity<Collection<ProfileElementModel>> retrieveElements(
			@PathVariable("profileUuid") UUID profileUuid) {
		ProfileEntity profile = profilePipeService.retrieveProfileByUuid(profileUuid);
		if (profile == null) {
			return ResponseEntity.notFound().build();
		}
		List<ProfileElementModel> elements = profile.getProfileElementEntities()
			.stream()
			.sorted(Comparator.comparing(ProfileElementEntity::getPosition,
					Comparator.nullsLast(Comparator.naturalOrder())))
			.map(ProfileElementMapper::toModel)
			.toList();
		return ResponseEntity.ok(elements);
	}

	@Operation(summary = "Add an element to a profile", description = "Default (built-in) profiles are left untouched.")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Profile element added"),
			@ApiResponse(responseCode = "404", description = "Profile not found", content = @Content) })
	@PostMapping(value = "/{profileUuid}/elements", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_update')")
	public ResponseEntity<ProfileElementModel> addElement(@PathVariable("profileUuid") UUID profileUuid,
			@Valid @RequestBody ProfileElementModel profileElementModel) {
		ProfileEntity profile = profilePipeService.retrieveProfileByUuid(profileUuid);
		if (profile == null) {
			return ResponseEntity.notFound().build();
		}
		ProfileElementEntity newElement = ProfileElementMapper.toEntity(profileElementModel);
		if (newElement == null) {
			return ResponseEntity.badRequest().build();
		}
		ProfileEntity saved = profilePipeService.saveElement(profile.getId(), newElement);
		if (saved == null) {
			return ResponseEntity.notFound().build();
		}
		ProfileElementEntity createdElement = profilePipeService.retrieveElementByUuid(saved, newElement.getUuid());
		if (createdElement == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(ProfileElementMapper.toModel(createdElement));
	}

	@Operation(summary = "Update an element of a profile",
			description = "Default (built-in) profiles are left untouched.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Profile element updated"),
			@ApiResponse(responseCode = "404", description = "Profile or element not found", content = @Content) })
	@PutMapping(value = "/{profileUuid}/elements/{elementUuid}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_update')")
	public ResponseEntity<ProfileElementModel> updateElement(@PathVariable("profileUuid") UUID profileUuid,
			@PathVariable("elementUuid") UUID elementUuid, @Valid @RequestBody ProfileElementModel profileElementModel) {
		ProfileEntity profile = profilePipeService.retrieveProfileByUuid(profileUuid);
		if (profile == null) {
			return ResponseEntity.notFound().build();
		}
		ProfileElementEntity existingElement = profilePipeService.retrieveElementByUuid(profile, elementUuid);
		if (existingElement == null) {
			return ResponseEntity.notFound().build();
		}
		ProfileElementEntity updatedElement = ProfileElementMapper.toEntity(profileElementModel);
		if (updatedElement == null) {
			return ResponseEntity.badRequest().build();
		}
		// Preserve the technical id so the service replaces the existing element (at its
		// current position) instead of appending a new one.
		updatedElement.setId(existingElement.getId());
		ProfileEntity saved = profilePipeService.saveElement(profile.getId(), updatedElement);
		ProfileElementEntity savedElement = profilePipeService.retrieveElementByUuid(saved, updatedElement.getUuid());
		if (savedElement == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(ProfileElementMapper.toModel(savedElement));
	}

	@Operation(summary = "Delete an element from a profile",
			description = "Default (built-in) profiles are left untouched.")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Profile element deleted"),
			@ApiResponse(responseCode = "404", description = "Profile or element not found", content = @Content) })
	@DeleteMapping(value = "/{profileUuid}/elements/{elementUuid}", produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('karnak_delete')")
	public ResponseEntity<Void> deleteElement(@PathVariable("profileUuid") UUID profileUuid,
			@PathVariable("elementUuid") UUID elementUuid) {
		ProfileEntity profile = profilePipeService.retrieveProfileByUuid(profileUuid);
		if (profile == null) {
			return ResponseEntity.notFound().build();
		}
		ProfileElementEntity element = profilePipeService.retrieveElementByUuid(profile, elementUuid);
		if (element == null) {
			return ResponseEntity.notFound().build();
		}
		profilePipeService.deleteElement(profile.getId(), element.getId());
		return ResponseEntity.noContent().build();
	}


	/** True when {@code profile} has the given name and version (both compared as-is). */
	private static boolean sameNameAndVersion(ProfileEntity profile, String name, String version) {
		return Objects.equals(profile.getName(), name) && Objects.equals(profile.getVersion(), version);
	}

	/** Parse an uploaded/edited YAML profile into its {@link ProfilePipeBody} model. */
	private static ProfilePipeBody readProfileYaml(InputStream stream) throws IOException {
		return new YAMLMapper().readValue(stream, ProfilePipeBody.class);
	}

	/**
	 * Build a human-readable, positional message from a Jackson YAML parse error. Uses
	 * the error location (line/column, both 1-based) when available.
	 */
	private static String formatYamlError(JsonProcessingException e) {
		JsonLocation location = e.getLocation();
		String where = location != null
				? "Line %d, column %d: ".formatted(location.getLineNr(), location.getColumnNr()) : "";
		String problem = e.getOriginalMessage() != null ? e.getOriginalMessage() : "invalid YAML structure";
		return where + problem;
	}

	/** Prefix a validation error with the name of the offending profile element. */
	private static String formatValidationError(ProfileError profileError) {
		String name = profileError.getProfileElement() != null ? profileError.getProfileElement().getName() : null;
		return (name != null ? "%s: ".formatted(name) : "") + profileError.getError();
	}

	/** Build a filesystem-safe file name for a profile YAML download. */
	private static String buildDownloadFileName(ProfileEntity profile) {
		String base = profile.getVersion() != null && !profile.getVersion().isBlank()
				? "%s-%s".formatted(profile.getName(), profile.getVersion()) : profile.getName();
		return "%s.yml".formatted(UNSAFE_FILENAME_CHARS.matcher(base).replaceAll("_"));
	}

	/**
	 * Validate an imported YAML profile body before it is persisted.
	 * @param profilePipeBody the parsed body (may be {@code null} when the YAML was empty)
	 * @return a ready-to-return error response, or {@code null} when the profile is valid
	 * and can be persisted
	 */
	private @Nullable ResponseEntity<?> validateImportedProfile(@Nullable ProfilePipeBody profilePipeBody) {
		String basicError = basicImportError(profilePipeBody);
		if (basicError != null) {
			return ResponseEntity.badRequest().body(List.of(basicError));
		}
		if (profilePipeService.retrieveAllProfiles()
				.stream()
				.anyMatch(p -> sameNameAndVersion(p, profilePipeBody.getName(), profilePipeBody.getVersion()))) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(List.of("A profile with the same name and version already exists."));
		}
		List<String> validationErrors = profilePipeService.validateProfile(profilePipeBody)
				.stream()
				.filter(profileError -> profileError.getError() != null)
				.map(ProfileController::formatValidationError)
				.toList();
		if (!validationErrors.isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(validationErrors);
		}
		return null;
	}

	/**
	 * Check the structural prerequisites of an imported profile (non-empty YAML, a
	 * name, and a {@code profileElements} list).
	 * @return a human-readable error message, or {@code null} when the body is
	 * structurally valid
	 */
	private static @Nullable String basicImportError(@Nullable ProfilePipeBody profilePipeBody) {
		if (profilePipeBody == null) {
			return "The YAML content is empty.";
		}
		if (profilePipeBody.getName() == null || profilePipeBody.getName().isBlank()) {
			return "The profile must have a name.";
		}
		if (profilePipeBody.getProfileElements() == null) {
			return "The profile must contain a \"profileElements\" list.";
		}
		return null;
	}
}
