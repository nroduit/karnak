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
import org.karnak.backend.data.repo.SecretRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SecretService {

	// Repositories
	private final SecretRepo secretRepo;

	@Autowired
	public SecretService(final SecretRepo secretRepo) {
		this.secretRepo = secretRepo;
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
	 * Save in db a new active secret for the project in parameter
	 * @param secretEntity Secret to save
	 * @param projectEntity Project associated to the secret
	 * @return Secret saved
	 */
	public SecretEntity saveActiveSecret(SecretEntity secretEntity, ProjectEntity projectEntity) {
		secretEntity.setProjectEntity(projectEntity);
		secretEntity.setActive(true);
		return secretRepo.saveAndFlush(secretEntity);
	}

}
