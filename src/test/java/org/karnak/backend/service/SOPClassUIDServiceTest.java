/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.repo.SOPClassUIDRepo;
import org.mockito.Mockito;

class SOPClassUIDServiceTest {

	// Repositories
	private final SOPClassUIDRepo sopClassUIDRepoMock = Mockito.mock(SOPClassUIDRepo.class);

	// Service
	private SOPClassUIDService sopClassUIDService;

	@BeforeEach
	public void setUp() {
		// Build mocked service
		sopClassUIDService = new SOPClassUIDService(sopClassUIDRepoMock);
	}

	@Test
	void should_retrieve_sopClassUID_by_id() {
		// Call service
		sopClassUIDService.get(1L);

		// Test results
		Mockito.verify(sopClassUIDRepoMock, Mockito.times(1)).getSOPClassUIDById(Mockito.anyLong());
	}

	@Test
	void should_retrieve_sopClassUID_by_name() {
		// Call service
		sopClassUIDService.getByName("name");

		// Test results
		Mockito.verify(sopClassUIDRepoMock, Mockito.times(1)).getSOPClassUIDByName(Mockito.anyString());
	}

	@Test
	void should_retrieve_all_sopClassUID() {
		// Call service
		sopClassUIDService.getAllSOPClassUIDs();

		// Test results
		Mockito.verify(sopClassUIDRepoMock, Mockito.atLeast(1)).findAll();
	}

	@Test
	void should_retrieve_all_names_from_sopClassUID() {
		// Call service
		sopClassUIDService.getAllSOPClassUIDsName();

		// Test results
		Mockito.verify(sopClassUIDRepoMock, Mockito.atLeast(1)).findAll();
	}

}
