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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.KheopsAlbumsEntity;
import org.karnak.backend.data.repo.KheopsAlbumsRepo;
import org.mockito.Mockito;

class KheopsAlbumsServiceTest {

	// Repositories
	private final KheopsAlbumsRepo kheopsAlbumsRepoMock = Mockito.mock(KheopsAlbumsRepo.class);

	// Service
	private KheopsAlbumsService kheopsAlbumsService;

	@BeforeEach
	public void setUp() {
		// Build mocked service
		kheopsAlbumsService = new KheopsAlbumsService(kheopsAlbumsRepoMock);
	}

	@Test
	void should_add_new_album() {
		// Init data
		KheopsAlbumsEntity kheopsAlbumsEntity = new KheopsAlbumsEntity();
		DestinationEntity destinationEntity = new DestinationEntity();
		destinationEntity.setId(1L);
		kheopsAlbumsEntity.setDestinationEntity(destinationEntity);

		// Call service
		kheopsAlbumsService.newSwitchingAlbum(kheopsAlbumsEntity);

		// Test results
		Mockito.verify(kheopsAlbumsRepoMock, Mockito.times(1)).saveAndFlush(Mockito.any(KheopsAlbumsEntity.class));
	}

	@Test
	void should_update_album_from_destination_and_delete_diff() {
		// Init data
		DestinationEntity destinationEntity = new DestinationEntity();
		destinationEntity.setId(1L);
		KheopsAlbumsEntity kheopsAlbumsEntityFromDestination = new KheopsAlbumsEntity();
		kheopsAlbumsEntityFromDestination.setId(2L);
		destinationEntity.setKheopsAlbumEntities(Collections.singletonList(kheopsAlbumsEntityFromDestination));
		DestinationEntity destinationEntityFromKheopsAlbum = new DestinationEntity();
		destinationEntityFromKheopsAlbum.setId(3L);
		kheopsAlbumsEntityFromDestination.setDestinationEntity(destinationEntityFromKheopsAlbum);

		// Mock
		// findAllByDestinationEntity
		List<KheopsAlbumsEntity> kheopsAlbumsEntities = new ArrayList<>();
		KheopsAlbumsEntity kheopsAlbumsEntityFirst = new KheopsAlbumsEntity();
		kheopsAlbumsEntityFirst.setId(88L);
		KheopsAlbumsEntity kheopsAlbumsEntitySecond = new KheopsAlbumsEntity();
		kheopsAlbumsEntityFirst.setId(99L);
		kheopsAlbumsEntities.add(kheopsAlbumsEntityFirst);
		kheopsAlbumsEntities.add(kheopsAlbumsEntitySecond);
		Mockito.when(kheopsAlbumsRepoMock.findAllByDestinationEntity(Mockito.any(DestinationEntity.class)))
			.thenReturn(kheopsAlbumsEntities);

		// Call service
		kheopsAlbumsService.updateSwitchingAlbumsFromDestination(destinationEntity);

		// Test results
		Mockito.verify(kheopsAlbumsRepoMock, Mockito.times(1)).saveAndFlush(Mockito.any(KheopsAlbumsEntity.class));
		Mockito.verify(kheopsAlbumsRepoMock, Mockito.times(1))
			.findAllByDestinationEntity(Mockito.any(DestinationEntity.class));
		Mockito.verify(kheopsAlbumsRepoMock, Mockito.times(1)).deleteById(Mockito.anyLong());
	}

	@Test
	void should_update_album_from_destination_and_delete_all() {
		// Init data
		DestinationEntity destinationEntity = new DestinationEntity();
		destinationEntity.setId(1L);
		KheopsAlbumsEntity kheopsAlbumsEntityFromDestination = new KheopsAlbumsEntity();
		kheopsAlbumsEntityFromDestination.setId(2L);
		DestinationEntity destinationEntityFromKheopsAlbum = new DestinationEntity();
		destinationEntityFromKheopsAlbum.setId(3L);
		kheopsAlbumsEntityFromDestination.setDestinationEntity(destinationEntityFromKheopsAlbum);

		// Mock
		// findAllByDestinationEntity
		List<KheopsAlbumsEntity> kheopsAlbumsEntities = new ArrayList<>();
		KheopsAlbumsEntity kheopsAlbumsEntityFirst = new KheopsAlbumsEntity();
		kheopsAlbumsEntityFirst.setId(88L);
		kheopsAlbumsEntities.add(kheopsAlbumsEntityFirst);
		Mockito.when(kheopsAlbumsRepoMock.findAllByDestinationEntity(Mockito.any(DestinationEntity.class)))
			.thenReturn(kheopsAlbumsEntities);

		// Call service
		kheopsAlbumsService.updateSwitchingAlbumsFromDestination(destinationEntity);

		// Test results
		Mockito.verify(kheopsAlbumsRepoMock, Mockito.times(1))
			.findAllByDestinationEntity(Mockito.any(DestinationEntity.class));
		Mockito.verify(kheopsAlbumsRepoMock, Mockito.times(1)).deleteById(Mockito.anyLong());
	}

}
