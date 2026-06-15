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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.entity.SecretEntity;
import org.karnak.backend.data.repo.SecretRepo;
import org.mockito.Mockito;

class SecretServiceTest {

	// Repositories
	private final SecretRepo secretRepositoryMock = Mockito.mock(SecretRepo.class);

	// Service
	private SecretService secretService;

	@BeforeEach
	public void setUp() {
		// Build mocked service
		secretService = new SecretService(secretRepositoryMock);
	}

	@Test
	void should_call_save_from_repository() {
		// Init data
		SecretEntity secretEntity = new SecretEntity();

		// Call service
		secretService.save(secretEntity);

		// Test results
		Mockito.verify(secretRepositoryMock, Mockito.times(1)).saveAndFlush(Mockito.any(SecretEntity.class));
	}

	@Test
	void should_save_active_secret() {
		// Init data
		SecretEntity secretEntity = new SecretEntity();
		secretEntity.setActive(false);

		// Call service
		secretService.saveActiveSecret(secretEntity, new ProjectEntity());

		// Test results
		Mockito.verify(secretRepositoryMock, Mockito.times(1)).saveAndFlush(Mockito.any(SecretEntity.class));
		assertTrue(secretEntity.isActive());
	}

}
