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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.TransferStatusEntity;
import org.karnak.backend.data.repo.TransferStatusRepo;
import org.karnak.backend.data.repo.specification.TransferStatusSpecification;
import org.karnak.backend.model.event.TransferMonitoringEvent;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class TransferMonitoringServiceTest {

	// Repositories
	private final TransferStatusRepo transferStatusRepoMock = Mockito.mock(TransferStatusRepo.class);

	// Service
	private TransferMonitoringService transferMonitoringService;

	@BeforeEach
	void setUp() {
		// Mock repo
		TransferStatusEntity transferStatusEntity = new TransferStatusEntity();
		transferStatusEntity.setStudyUidOriginal("studyUidOriginal");
		Page<TransferStatusEntity> transferStatusEntitiesPage = new PageImpl<>(
				Collections.singletonList(transferStatusEntity), PageRequest.of(0, 3), 3);
		when(transferStatusRepoMock.findAll(any(Pageable.class))).thenReturn(transferStatusEntitiesPage);
		when(transferStatusRepoMock.findAll(any(TransferStatusSpecification.class), any(Pageable.class)))
			.thenReturn(transferStatusEntitiesPage);
		when(transferStatusRepoMock.count()).thenReturn(1L);
		when(transferStatusRepoMock.count(any(TransferStatusSpecification.class))).thenReturn(2L);

		// Build mocked service
		transferMonitoringService = new TransferMonitoringService(transferStatusRepoMock);
	}

	@Test
	void shouldSaveEventReceived() {
		// Init data
		TransferStatusEntity transferStatusEntity = new TransferStatusEntity();
		TransferMonitoringEvent transferMonitoringEvent = new TransferMonitoringEvent(transferStatusEntity);

		// Call service
		transferMonitoringService.onTransferMonitoringEvent(transferMonitoringEvent);

		// Test result
		Mockito.verify(transferStatusRepoMock, Mockito.times(1)).save(Mockito.any(TransferStatusEntity.class));
	}

	@Test
	void shouldRetrieveTransferStatusNoFilter() {
		// Init data
		TransferStatusFilter filter = new TransferStatusFilter();
		TransferStatusEntity transferStatusEntity = new TransferStatusEntity();
		Page<TransferStatusEntity> pageable = new PageImpl<>(Collections.singletonList(transferStatusEntity),
				PageRequest.of(0, 3), 3);

		// Call service
		Page<TransferStatusEntity> transferStatusEntities = transferMonitoringService
			.retrieveTransferStatusPageable(filter, pageable.getPageable());

		// Test result
		assertNotNull(transferStatusEntities);
		assertFalse(transferStatusEntities.toList().isEmpty());
		assertEquals("studyUidOriginal", transferStatusEntities.toList().get(0).getStudyUidOriginal());
		Mockito.verify(transferStatusRepoMock, Mockito.times(1)).findAll(Mockito.any(Pageable.class));
	}

	@Test
	void shouldRetrieveTransferStatusFilter() {
		// Init data
		TransferStatusFilter filter = new TransferStatusFilter();
		filter.setStudyUid("studyUid");
		TransferStatusEntity transferStatusEntity = new TransferStatusEntity();
		Page<TransferStatusEntity> pageable = new PageImpl<>(Collections.singletonList(transferStatusEntity),
				PageRequest.of(0, 3), 3);

		// Call service
		Page<TransferStatusEntity> transferStatusEntities = transferMonitoringService
			.retrieveTransferStatusPageable(filter, pageable.getPageable());

		// Test result
		assertNotNull(transferStatusEntities);
		assertFalse(transferStatusEntities.toList().isEmpty());
		assertEquals("studyUidOriginal", transferStatusEntities.toList().get(0).getStudyUidOriginal());
		Mockito.verify(transferStatusRepoMock, Mockito.times(1))
			.findAll(Mockito.any(TransferStatusSpecification.class), Mockito.any(Pageable.class));
	}

	@Test
	void shouldCountTransferStatusNoFilter() {
		// Init data
		TransferStatusFilter filter = new TransferStatusFilter();

		// Call service
		int count = transferMonitoringService.countTransferStatus(filter);

		// Test result
		assertEquals(1L, count);
		Mockito.verify(transferStatusRepoMock, Mockito.times(1)).count();
	}

	@Test
	void shouldCountTransferStatusFilter() {
		// Init data
		TransferStatusFilter filter = new TransferStatusFilter();
		filter.setStudyUid("studyUid");

		// Call service
		int count = transferMonitoringService.countTransferStatus(filter);

		// Test result
		assertEquals(2L, count);
		Mockito.verify(transferStatusRepoMock, Mockito.times(1)).count(Mockito.any(TransferStatusSpecification.class));
	}

	@Test
	void shouldDeleteAllTransferStatus() {
		// Call service
		transferMonitoringService.deleteAllTransferStatus();

		// Test result: verify deleteAll() is called exactly once on the repository
		Mockito.verify(transferStatusRepoMock, Mockito.times(1)).deleteAll();
	}

}
