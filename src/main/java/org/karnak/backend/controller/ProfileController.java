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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.karnak.backend.constant.EndPoint;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.model.profilebody.ProfilePipeBody;
import org.karnak.backend.service.profilepipe.ProfilePipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

/**
 * Rest controller managing de-identification profiles
 */
@RestController
@RequestMapping(EndPoint.PROFILES_PATH)
@Tag(name = "Profile", description = "API Endpoints for De-identification Profiles")
public class ProfileController {

	private final ProfilePipeService profilePipeService;

	@Autowired
	public ProfileController(final ProfilePipeService profilePipeService) {
		this.profilePipeService = profilePipeService;
	}

	@Operation(summary = "List all profiles",
			description = "Returns id, name, version and minimumKarnakVersion for each profile")
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<Map<String, Object>>> getAllProfiles() {
		List<Map<String, Object>> result = profilePipeService.getAllProfiles().stream().map(p -> {
			Map<String, Object> m = new HashMap<>();
			m.put("id", p.getId());
			m.put("name", p.getName() != null ? p.getName() : "");
			m.put("version", p.getVersion() != null ? p.getVersion() : "");
			m.put("minimumKarnakVersion", p.getMinimumKarnakVersion() != null ? p.getMinimumKarnakVersion() : "");
			m.put("byDefault", Boolean.TRUE.equals(p.getByDefault()));
			return m;
		}).toList();
		return result.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(result);
	}

	@Operation(summary = "Upload a YAML profile", description = "Upload a YAML de-identification profile file")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Profile uploaded and saved"),
			@ApiResponse(responseCode = "400", description = "Invalid YAML, empty file, or unreadable upload",
					content = @Content),
			@ApiResponse(responseCode = "422", description = "Profile has validation errors", content = @Content) })
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> uploadProfile(@RequestParam("file") MultipartFile file) {
		if (file == null || file.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Uploaded file is empty"));
		}
		try (InputStream inputStream = file.getInputStream()) {
			Yaml yaml = new Yaml(new Constructor(ProfilePipeBody.class, new LoaderOptions()));
			ProfilePipeBody profilePipeBody = yaml.load(inputStream);
			if (profilePipeBody == null) {
				return ResponseEntity.badRequest()
					.body(Map.of("error", "YAML did not produce a profile (empty or malformed root)"));
			}
			var errors = profilePipeService.validateProfile(profilePipeBody);
			boolean hasErrors = errors.stream().anyMatch(e -> e.getError() != null);
			if (hasErrors) {
				List<String> errorMessages = errors.stream()
					.filter(e -> e.getError() != null)
					.map(e -> e.getProfileElement().getName() + ": " + e.getError())
					.toList();
				return ResponseEntity.unprocessableEntity().body(Map.of("errors", errorMessages));
			}
			ProfileEntity saved = profilePipeService.saveProfilePipe(profilePipeBody, false);
			return ResponseEntity.status(201).body(Map.of("id", saved.getId(), "name", saved.getName()));
		}
		catch (YAMLException e) {
			return ResponseEntity.badRequest().body(Map.of("error", "Invalid YAML file: " + e.getMessage()));
		}
		catch (IOException e) {
			return ResponseEntity.badRequest().body(Map.of("error", "Cannot read uploaded file"));
		}
	}

	@Operation(summary = "Get a profile by ID (JSON)")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Profile found"),
			@ApiResponse(responseCode = "404", description = "Profile not found", content = @Content) })
	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ProfileEntity> getProfile(@PathVariable Long id) {
		ProfileEntity profile = findById(id);
		return profile == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(profile);
	}

	@Operation(summary = "Download a profile as YAML")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Profile YAML file"),
			@ApiResponse(responseCode = "404", description = "Profile not found", content = @Content),
			@ApiResponse(responseCode = "500", description = "Serialization error", content = @Content) })
	@GetMapping(value = "/{id}/download", produces = "application/x-yaml")
	public ResponseEntity<String> downloadProfile(@PathVariable Long id) throws Exception {
		ProfileEntity profile = findById(id);
		if (profile == null) {
			return ResponseEntity.notFound().build();
		}
		ObjectMapper mapper = new ObjectMapper(new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));
		String yaml = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(profile);
		String filename = (profile.getName() != null ? profile.getName() : "profile").replace(" ", "-") + ".yml";
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
			.body(yaml);
	}

	@Operation(summary = "Update profile metadata (name, version, minimumKarnakVersion)")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Profile updated"),
			@ApiResponse(responseCode = "404", description = "Profile not found", content = @Content) })
	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> updateProfile(@PathVariable Long id,
			@RequestBody Map<String, String> body) {
		ProfileEntity profile = findById(id);
		if (profile == null) {
			return ResponseEntity.notFound().build();
		}
		if (body.containsKey("name")) {
			profile.setName(body.get("name"));
		}
		if (body.containsKey("version")) {
			profile.setVersion(body.get("version"));
		}
		if (body.containsKey("minimumKarnakVersion")) {
			profile.setMinimumKarnakVersion(body.get("minimumKarnakVersion"));
		}
		profilePipeService.updateProfile(profile);
		return ResponseEntity.ok(Map.of("id", profile.getId(), "name", profile.getName()));
	}

	@Operation(summary = "Delete a profile")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Profile deleted"),
			@ApiResponse(responseCode = "404", description = "Profile not found", content = @Content) })
	@DeleteMapping(value = "/{id}")
	public ResponseEntity<Void> deleteProfile(@PathVariable Long id) {
		ProfileEntity profile = findById(id);
		if (profile == null) {
			return ResponseEntity.notFound().build();
		}
		profilePipeService.deleteProfile(profile);
		return ResponseEntity.noContent().build();
	}

	private ProfileEntity findById(Long id) {
		return profilePipeService.getAllProfiles().stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
	}

}
