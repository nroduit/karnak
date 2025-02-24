/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.KheopsAlbumsEntity;
import org.karnak.backend.data.repo.KheopsAlbumsRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KheopsAlbumsService {

	private final KheopsAlbumsRepo kheopsAlbumsRepo;

	@Autowired
	public KheopsAlbumsService(final KheopsAlbumsRepo kheopsAlbumsRepo) {
		this.kheopsAlbumsRepo = kheopsAlbumsRepo;
	}

	public void newSwitchingAlbum(KheopsAlbumsEntity kheopsAlbum) {
		Long destinationID = kheopsAlbum.getDestinationEntity() != null ? kheopsAlbum.getDestinationEntity().getId()
				: null;
		if (destinationID != null) {
			kheopsAlbumsRepo.saveAndFlush(kheopsAlbum);
		}
	}

	public void updateSwitchingAlbumsFromDestination(DestinationEntity destinationEntity) {
		if (destinationEntity.getKheopsAlbumEntities() != null) {
			for (KheopsAlbumsEntity kheopsAlbum : destinationEntity.getKheopsAlbumEntities()) {
				setDestination(destinationEntity, kheopsAlbum);
			}
		}
		removeKheopsAlbums(destinationEntity);
	}

	private void removeKheopsAlbums(DestinationEntity destinationEntity) {
		List<KheopsAlbumsEntity> kheopsAlbumsEntityListDatabase = new ArrayList<>();
		kheopsAlbumsRepo.findAllByDestinationEntity(destinationEntity) //
			.forEach(kheopsAlbumsEntityListDatabase::add);
		deleteDiffCurrentAndDatabase(destinationEntity, kheopsAlbumsEntityListDatabase);
		deleteAll(destinationEntity, kheopsAlbumsEntityListDatabase);
	}

	private void deleteDiffCurrentAndDatabase(DestinationEntity destinationEntity,
			List<KheopsAlbumsEntity> kheopsAlbumsEntityListDatabase) {
		if (destinationEntity.getKheopsAlbumEntities() != null && kheopsAlbumsEntityListDatabase != null
				&& destinationEntity.getKheopsAlbumEntities().size() != kheopsAlbumsEntityListDatabase.size()) {
			for (KheopsAlbumsEntity kheopsAlbumDatabase : kheopsAlbumsEntityListDatabase) {
				Predicate<KheopsAlbumsEntity> idIsAlwaysPresent = kheopsAlbum -> kheopsAlbum.getId()
					.equals(kheopsAlbumDatabase.getId());
				if (!destinationEntity.getKheopsAlbumEntities().stream().anyMatch(idIsAlwaysPresent)) {
					deleteSwitchingAlbums(kheopsAlbumDatabase);
				}
			}
		}
	}

	private void deleteAll(DestinationEntity destinationEntity,
			List<KheopsAlbumsEntity> kheopsAlbumsEntityListDatabase) {
		if (destinationEntity.getKheopsAlbumEntities() == null && kheopsAlbumsEntityListDatabase != null) {
			deleteListSwitchingAlbums(kheopsAlbumsEntityListDatabase);
		}
	}

	public void setDestination(DestinationEntity destinationEntity, KheopsAlbumsEntity kheopsAlbum) {
		Long destinationID = kheopsAlbum.getDestinationEntity() != null ? kheopsAlbum.getDestinationEntity().getId()
				: null;
		if (destinationID == null) {
			kheopsAlbum.setDestinationEntity(destinationEntity);
		}
		kheopsAlbumsRepo.saveAndFlush(kheopsAlbum);
	}

	public void deleteSwitchingAlbums(KheopsAlbumsEntity kheopsAlbumsEntity) {
		kheopsAlbumsRepo.deleteById(kheopsAlbumsEntity.getId());
		kheopsAlbumsRepo.flush();
	}

	public void deleteListSwitchingAlbums(List<KheopsAlbumsEntity> kheopsAlbumsEntityList) {
		if (kheopsAlbumsEntityList != null) {
			kheopsAlbumsEntityList.forEach(kheopsAlbums -> {
				kheopsAlbumsRepo.deleteById(kheopsAlbums.getId());
			});
			kheopsAlbumsRepo.flush();
		}
	}

}
