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
import java.util.List;
import org.karnak.backend.constant.EndPoint;
import org.karnak.backend.data.entity.AuthConfigEntity;
import org.karnak.backend.data.repo.AuthConfigRepo;
import org.karnak.backend.enums.AuthConfigType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller managing OAuth2 authentication configurations for STOW-RS destinations
 */
@RestController
@RequestMapping(EndPoint.AUTH_CONFIGS_PATH)
@Tag(name = "AuthConfig", description = "API Endpoints for Authentication Configurations (OAuth2)")
public class AuthConfigController {

	private final AuthConfigRepo authConfigRepo;

	@Autowired
	public AuthConfigController(final AuthConfigRepo authConfigRepo) {
		this.authConfigRepo = authConfigRepo;
	}

	@Operation(summary = "List all authentication configurations")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Auth configs found"),
			@ApiResponse(responseCode = "204", description = "No auth configs", content = @Content) })
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<AuthConfigEntity>> getAllAuthConfigs() {
		List<AuthConfigEntity> configs = authConfigRepo.findAll();
		return configs.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(configs);
	}

	@Operation(summary = "Create an authentication configuration",
			description = "Body: {\"code\": \"my-auth\", \"clientId\": \"...\", \"clientSecret\": \"...\","
					+ " \"accessTokenUrl\": \"https://...\", \"scope\": \"openid\"}")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Auth config created"),
			@ApiResponse(responseCode = "400", description = "Missing required fields", content = @Content),
			@ApiResponse(responseCode = "409", description = "Code already exists", content = @Content) })
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AuthConfigEntity> createAuthConfig(@RequestBody AuthConfigEntity authConfigEntity) {
		if (authConfigEntity.getCode() == null || authConfigEntity.getCode().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		if (authConfigRepo.findByCode(authConfigEntity.getCode()) != null) {
			return ResponseEntity.status(409).build();
		}
		authConfigEntity.setId(null);
		if (authConfigEntity.getAuthConfigType() == null) {
			authConfigEntity.setAuthConfigType(AuthConfigType.OAUTH2);
		}
		AuthConfigEntity saved = authConfigRepo.save(authConfigEntity);
		return ResponseEntity.status(201).body(saved);
	}

	@Operation(summary = "Get an authentication configuration by its code identifier")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Auth config found"),
			@ApiResponse(responseCode = "404", description = "Auth config not found", content = @Content) })
	@GetMapping(value = "/{code}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AuthConfigEntity> getAuthConfig(@PathVariable String code) {
		AuthConfigEntity config = authConfigRepo.findByCode(code);
		return config == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(config);
	}

	@Operation(summary = "Update an authentication configuration",
			description = "Updatable fields: clientId, clientSecret, accessTokenUrl, scope")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Auth config updated"),
			@ApiResponse(responseCode = "404", description = "Auth config not found", content = @Content) })
	@PutMapping(value = "/{code}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AuthConfigEntity> updateAuthConfig(@PathVariable String code,
			@RequestBody AuthConfigEntity incoming) {
		AuthConfigEntity existing = authConfigRepo.findByCode(code);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		if (incoming.getClientId() != null) {
			existing.setClientId(incoming.getClientId());
		}
		if (incoming.getClientSecret() != null) {
			existing.setClientSecret(incoming.getClientSecret());
		}
		if (incoming.getAccessTokenUrl() != null) {
			existing.setAccessTokenUrl(incoming.getAccessTokenUrl());
		}
		if (incoming.getScope() != null) {
			existing.setScope(incoming.getScope());
		}
		return ResponseEntity.ok(authConfigRepo.save(existing));
	}

	@Operation(summary = "Delete an authentication configuration")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Auth config deleted"),
			@ApiResponse(responseCode = "404", description = "Auth config not found", content = @Content) })
	@DeleteMapping(value = "/{code}")
	public ResponseEntity<Void> deleteAuthConfig(@PathVariable String code) {
		AuthConfigEntity existing = authConfigRepo.findByCode(code);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		authConfigRepo.delete(existing);
		return ResponseEntity.noContent().build();
	}

}
