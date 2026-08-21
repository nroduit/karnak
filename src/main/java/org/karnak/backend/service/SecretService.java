/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.entity.SecretEntity;
import org.karnak.backend.data.repo.ProjectRepo;
import org.karnak.backend.data.repo.SecretRepo;
import org.karnak.backend.model.profilepipe.HMAC;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class SecretService {

	// Repositories
	private final SecretRepo secretRepo;

	private final ProjectRepo projectRepo;

	@Autowired
	public SecretService(final SecretRepo secretRepo, final ProjectRepo projectRepo) {
		this.secretRepo = secretRepo;
		this.projectRepo = projectRepo;
	}

	/**
	 * Save a secret in db
	 * @param secretEntity Secret to save
	 * @return Secret saved
	 */
	public SecretEntity save(SecretEntity secretEntity) {
		return secretRepo.saveAndFlush(secretEntity);
	}

	/**
	 * Activate the given secret for the project (deactivating the others) and persist it.
	 * @param secretEntity Secret to activate
	 * @param projectEntity Project the secret belongs to
	 * @return The saved and activated secret
	 */
	public SecretEntity saveActiveSecret(SecretEntity secretEntity, ProjectEntity projectEntity) {
		secretEntity.setProjectEntity(projectEntity);
		projectEntity.applyActiveSecret(secretEntity);
		return secretRepo.saveAndFlush(secretEntity);
	}

	/**
	 * Generate a new random secret for the given project and activate it.
	 * @param projectEntity Project to associate the secret with
	 * @return The newly created and activated secret
	 */
	public SecretEntity generateSecret(ProjectEntity projectEntity) {
		SecretEntity secret = new SecretEntity(projectEntity, HMAC.generateRandomKey());
		projectEntity.addActiveSecretEntity(secret);
		return secret;
	}

	/**
	 * Import a secret from a hex string for the given project and activate it.
	 * @param projectEntity Project to associate the secret with
	 * @param hexKey Hexadecimal key (dashed groups allowed)
	 * @return The newly created and activated secret
	 * @throws IllegalArgumentException if the key is not valid
	 */
	public SecretEntity importSecret(ProjectEntity projectEntity, String hexKey) {
		if (!HMAC.validateKey(hexKey)) {
			throw new IllegalArgumentException("Invalid secret key");
		}
		SecretEntity secret = new SecretEntity(projectEntity, HMAC.hexToByte(hexKey));
		projectEntity.addActiveSecretEntity(secret);
		return secret;
	}

	/**
	 * Retrieve all secrets of a project.
	 * @param projectEntity Project
	 * @return List of secrets
	 */
	public List<SecretEntity> findSecretsByProject(ProjectEntity projectEntity) {
		return projectEntity.getSecretEntities();
	}

	/**
	 * Find a secret by its public UUID.
	 * @param uuid Public UUID of the secret
	 * @return Secret found or null
	 */
	@Nullable
	public SecretEntity findSecretByUuid(UUID uuid) {
		return secretRepo.findByUuid(uuid).orElse(null);
	}

	/**
	 * Delete a secret from the database.
	 * @param secretEntity Secret to delete
	 */
	public void delete(SecretEntity secretEntity) {
		secretRepo.delete(secretEntity);
	}

	/**
	 * Delete a secret and persist the project in a single transaction.
	 * This ensures Hibernate correctly handles the bidirectional relationship
	 * (cascade ALL on ProjectEntity.secretEntities) without re-inserting the
	 * deleted entity during the project merge.
	 * @param projectEntity Project owning the secret
	 * @param secretEntity Secret to delete
	 */
	@Transactional
	public void deleteSecret(ProjectEntity projectEntity, SecretEntity secretEntity) {
		projectEntity.getSecretEntities().removeIf(s -> s.getId() != null && Objects.equals(s.getId(), secretEntity.getId()));
		secretRepo.delete(secretEntity);
		projectRepo.saveAndFlush(projectEntity);
	}

}
