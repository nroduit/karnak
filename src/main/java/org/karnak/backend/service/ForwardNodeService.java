/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.DicomSourceNodeEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.repo.ForwardNodeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ForwardNodeService {

	// Repositories
	private final ForwardNodeRepo forwardNodeRepo;

	@Autowired
	public ForwardNodeService(final ForwardNodeRepo forwardNodeRepo) {
		this.forwardNodeRepo = forwardNodeRepo;
	}

	/**
	 * Retrieves the ForwardNode according to its ID.
	 * @param dataId the data ID.
	 * @return the ForwardNodeEntity according to its ID; null if not found.
	 */
	public ForwardNodeEntity get(Long dataId) {
		return forwardNodeRepo.findById(dataId).orElse(null);
	}

	/**
	 * Store given ForwardNode.
	 * @param forwardNodeEntity the updated or new forwardNodeEntity
	 */
	public void save(ForwardNodeEntity forwardNodeEntity) {
		// Save forward node
		forwardNodeRepo.saveAndFlush(forwardNodeEntity);
	}

	/**
	 * Delete given data from the backing data service.
	 * @param data the data to be deleted
	 */
	public void delete(ForwardNodeEntity data) {
		forwardNodeRepo.deleteById(data.getId());
		forwardNodeRepo.flush();
	}

	public List<ForwardNodeEntity> getAllForwardNodes() {
		return forwardNodeRepo.findAll();
	}

	public Collection<DestinationEntity> getAllDestinations(ForwardNodeEntity forwardNodeEntity) {
		if (forwardNodeEntity != null) {
			return forwardNodeEntity.getDestinationEntities();
		}
		return new HashSet<>();
	}

	public DestinationEntity getDestinationById(ForwardNodeEntity forwardNodeEntity, Long dataId) {
		Collection<DestinationEntity> destinationEntities = getAllDestinations(forwardNodeEntity);
		for (DestinationEntity destinationEntity : destinationEntities) {
			if (Objects.equals(destinationEntity.getId(), dataId)) {
				return destinationEntity;
			}
		}
		return null;
	}

	public DestinationEntity updateDestination(ForwardNodeEntity forwardNodeEntity, DestinationEntity data) {
		if (forwardNodeEntity == null || data == null) {
			return null;
		}
		Collection<DestinationEntity> destinationEntities = getAllDestinations(forwardNodeEntity);
		if (!destinationEntities.contains(data)) {
			forwardNodeEntity.addDestination(data);
		}
		return data;
	}

	public void deleteDestination(ForwardNodeEntity forwardNodeEntity, DestinationEntity data) {
		if (forwardNodeEntity == null || data == null) {
			return;
		}
		forwardNodeEntity.removeDestination(data);
		forwardNodeRepo.saveAndFlush(forwardNodeEntity);
	}

	public Collection<DicomSourceNodeEntity> getAllSourceNodes(ForwardNodeEntity forwardNodeEntity) {
		if (forwardNodeEntity != null) {
			return forwardNodeEntity.getSourceNodes();
		}
		return new HashSet<>();
	}

	public DicomSourceNodeEntity getSourceNodeById(ForwardNodeEntity forwardNodeEntity, Long dataId) {
		Collection<DicomSourceNodeEntity> sourceNodes = getAllSourceNodes(forwardNodeEntity);
		for (DicomSourceNodeEntity sourceNode : sourceNodes) {
			if (Objects.equals(sourceNode.getId(), dataId)) {
				return sourceNode;
			}
		}
		return null;
	}

	public DicomSourceNodeEntity updateSourceNode(ForwardNodeEntity forwardNodeEntity, DicomSourceNodeEntity data) {
		if (forwardNodeEntity == null || data == null) {
			return null;
		}
		Collection<DicomSourceNodeEntity> sourceNodes = getAllSourceNodes(forwardNodeEntity);
		if (!sourceNodes.contains(data)) {
			forwardNodeEntity.addSourceNode(data);
		}
		forwardNodeRepo.saveAndFlush(forwardNodeEntity);
		return data;
	}

	public void deleteSourceNode(ForwardNodeEntity forwardNodeEntity, DicomSourceNodeEntity dicomSourceNodeEntity) {
		if (forwardNodeEntity == null || dicomSourceNodeEntity == null) {
			return;
		}
		forwardNodeEntity.removeSourceNode(dicomSourceNodeEntity);
		forwardNodeRepo.saveAndFlush(forwardNodeEntity);
	}

}
