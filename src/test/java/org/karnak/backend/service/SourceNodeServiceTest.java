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
import org.karnak.backend.data.entity.DicomSourceNodeEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.repo.DicomSourceNodeRepo;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

class SourceNodeServiceTest {

	// Application Event Publisher
	private final ApplicationEventPublisher applicationEventPublisherMock = Mockito
		.mock(ApplicationEventPublisher.class);

	// Repositories
	private final DicomSourceNodeRepo dicomSourceNodeRepoMock = Mockito.mock(DicomSourceNodeRepo.class);

	// Services
	private final ForwardNodeService forwardNodeServiceMock = Mockito.mock(ForwardNodeService.class);

	private SourceNodeService sourceNodeService;

	@BeforeEach
	public void setUp() {
		// Build mocked service
		sourceNodeService = new SourceNodeService(dicomSourceNodeRepoMock, forwardNodeServiceMock,
				applicationEventPublisherMock);
	}

	@Test
	void should_retrieve_source_nodes_from_forward_node() {
		// Init data
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();

		// Call service
		sourceNodeService.retrieveSourceNodes(forwardNodeEntity);

		// Test results
		Mockito.verify(forwardNodeServiceMock, Mockito.times(1))
			.getAllSourceNodes(Mockito.any(ForwardNodeEntity.class));
	}

	@Test
	void should_save_dicom_source_node() {
		// Init data
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();
		DicomSourceNodeEntity dicomSourceNodeEntity = new DicomSourceNodeEntity();

		// Mock service
		Mockito
			.when(forwardNodeServiceMock.updateSourceNode(Mockito.any(ForwardNodeEntity.class),
					Mockito.any(DicomSourceNodeEntity.class)))
			.thenReturn(dicomSourceNodeEntity);

		// Call service
		sourceNodeService.save(forwardNodeEntity, dicomSourceNodeEntity);

		// Test results
		Mockito.verify(forwardNodeServiceMock, Mockito.times(1))
			.updateSourceNode(Mockito.any(ForwardNodeEntity.class), Mockito.any(DicomSourceNodeEntity.class));
	}

	@Test
	void should_delete_source_node() {
		// Init data
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();
		DicomSourceNodeEntity dicomSourceNodeEntity = new DicomSourceNodeEntity();
		dicomSourceNodeEntity.setId(1L);
		dicomSourceNodeEntity.setForwardNodeEntity(forwardNodeEntity);

		// Call service
		sourceNodeService.delete(dicomSourceNodeEntity);

		// Test results
		Mockito.verify(forwardNodeServiceMock, Mockito.times(1))
			.deleteSourceNode(Mockito.any(ForwardNodeEntity.class), Mockito.any(DicomSourceNodeEntity.class));
	}

}
