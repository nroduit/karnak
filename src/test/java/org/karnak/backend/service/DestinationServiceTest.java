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
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

class DestinationServiceTest {

	// Application Event Publisher
	private final ApplicationEventPublisher applicationEventPublisherMock = Mockito
		.mock(ApplicationEventPublisher.class);

	// Repositories
	private final DestinationRepo destinationRepoMock = Mockito.mock(DestinationRepo.class);

	// Services
	private final ForwardNodeService forwardNodeServiceMock = Mockito.mock(ForwardNodeService.class);

	private final KheopsAlbumsService kheopsAlbumsServiceMock = Mockito.mock(KheopsAlbumsService.class);

	// Service
	private DestinationService destinationService;

	@BeforeEach
	public void setUp() {
		// Build mocked service
		destinationService = new DestinationService(destinationRepoMock, forwardNodeServiceMock,
				kheopsAlbumsServiceMock, applicationEventPublisherMock);
	}

	@Test
	void should_save_destination_update_forward_node_update_albums() {
		// Init data
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();
		DestinationEntity destinationEntity = new DestinationEntity();
		destinationEntity.setId(1L);

		// Call service
		destinationService.save(forwardNodeEntity, destinationEntity);

		// Test results
		Mockito.verify(forwardNodeServiceMock, Mockito.times(1))
			.updateDestination(Mockito.any(ForwardNodeEntity.class), Mockito.any(DestinationEntity.class));
		Mockito.verify(destinationRepoMock, Mockito.times(1)).saveAndFlush(Mockito.any(DestinationEntity.class));
		Mockito.verify(kheopsAlbumsServiceMock, Mockito.times(1))
			.updateSwitchingAlbumsFromDestination(Mockito.any(DestinationEntity.class));
	}

	@Test
	void should_delete_destination_update_forward_node() {
		// Init data
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();
		DestinationEntity destinationEntity = new DestinationEntity();
		destinationEntity.setId(1L);
		destinationEntity.setForwardNodeEntity(forwardNodeEntity);

		// Call service
		destinationService.delete(destinationEntity);

		// Test results
		Mockito.verify(forwardNodeServiceMock, Mockito.times(1))
			.deleteDestination(Mockito.any(ForwardNodeEntity.class), Mockito.any(DestinationEntity.class));
	}

	@Test
	void should_retrieve_destinations() {
		// Init data
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();

		// Call service
		destinationService.retrieveDestinations(forwardNodeEntity);

		// Test results
		Mockito.verify(forwardNodeServiceMock, Mockito.times(1))
			.getAllDestinations(Mockito.any(ForwardNodeEntity.class));
	}

}
