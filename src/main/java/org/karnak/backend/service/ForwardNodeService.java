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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.DicomSourceNodeEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.entity.ForwardNodeGroupEntity;
import org.karnak.backend.data.repo.ForwardNodeGroupRepo;
import org.karnak.backend.data.repo.ForwardNodeRepo;
import org.karnak.frontend.util.CollatorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@NullUnmarked
public class ForwardNodeService {

	// Repositories
	private final ForwardNodeRepo forwardNodeRepo;

	private final ForwardNodeGroupRepo forwardNodeGroupRepo;

	@Autowired
	public ForwardNodeService(final ForwardNodeRepo forwardNodeRepo, final ForwardNodeGroupRepo forwardNodeGroupRepo) {
		this.forwardNodeRepo = forwardNodeRepo;
		this.forwardNodeGroupRepo = forwardNodeGroupRepo;
	}

	/** Retrieve all forward node groups, sorted by name. */
	public List<ForwardNodeGroupEntity> getAllGroups() {
		List<ForwardNodeGroupEntity> groups = forwardNodeGroupRepo.findAll();
		groups.sort(CollatorUtils.comparing(ForwardNodeGroupEntity::getName));
		return groups;
	}

	/** Create and persist a new forward node group. */
	public ForwardNodeGroupEntity saveGroup(String name) {
		return forwardNodeGroupRepo.saveAndFlush(new ForwardNodeGroupEntity(name));
	}

	/** Rename an existing forward node group. */
	public void renameGroup(ForwardNodeGroupEntity group, String name) {
		if (group == null || group.getId() == null) {
			return;
		}
		forwardNodeGroupRepo.findById(group.getId()).ifPresent(persisted -> {
			persisted.setName(name);
			forwardNodeGroupRepo.saveAndFlush(persisted);
		});
	}

	/**
	 * Delete a forward node group. Members are detached first (their group is set to
	 * null) so they fall back to the root of the list instead of being deleted.
	 */
	public void deleteGroup(ForwardNodeGroupEntity group) {
		if (group == null || group.getId() == null) {
			return;
		}
		forwardNodeRepo.findAll().forEach(forwardNode -> {
			if (forwardNode.getGroup() != null && Objects.equals(forwardNode.getGroup().getId(), group.getId())) {
				forwardNode.setGroup(null);
				forwardNodeRepo.saveAndFlush(forwardNode);
			}
		});
		forwardNodeGroupRepo.deleteById(group.getId());
		forwardNodeGroupRepo.flush();
	}

	/** Assign a forward node to a group (null group removes it from any group). */
	public void assignToGroup(ForwardNodeEntity forwardNode, ForwardNodeGroupEntity group) {
		if (forwardNode == null || forwardNode.getId() == null) {
			return;
		}
		forwardNodeRepo.findById(forwardNode.getId()).ifPresent(persisted -> {
			persisted.setGroup(group);
			forwardNodeRepo.saveAndFlush(persisted);
		});
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
	 *
	 * <p>
	 * The forward node form only edits the node's own fields (AE title, description). Its
	 * destinations and sources are persisted through their own save flows. Because those
	 * collections cascade from the forward node, an in-memory forward node that has not
	 * been refreshed since a destination/source was edited would otherwise overwrite the
	 * freshly saved children with a stale state. Reload the children from the database
	 * first so only the forward node's own fields are updated.
	 * @param forwardNodeEntity the updated or new forwardNodeEntity
	 */
	public void save(ForwardNodeEntity forwardNodeEntity) {
		// Reload sources and destinations to avoid persisting a stale state
		reloadChildren(forwardNodeEntity);
		// Save forward node
		forwardNodeRepo.saveAndFlush(forwardNodeEntity);
	}

	/**
	 * Replaces the in-memory sources and destinations of an existing forward node with
	 * the persisted ones, so a stale collection cannot overwrite freshly saved children.
	 * @param forwardNodeEntity the forward node about to be saved
	 */
	private void reloadChildren(ForwardNodeEntity forwardNodeEntity) {
		if (forwardNodeEntity == null || forwardNodeEntity.getId() == null) {
			return;
		}
		forwardNodeRepo.findById(forwardNodeEntity.getId()).ifPresent(persisted -> {
			forwardNodeEntity.setSourceNodes(new HashSet<>(persisted.getSourceNodes()));
			forwardNodeEntity.setDestinationEntities(new HashSet<>(persisted.getDestinationEntities()));
		});
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
		return getAllDestinations(forwardNodeEntity).stream()
			.filter(destinationEntity -> Objects.equals(destinationEntity.getId(), dataId))
			.findFirst()
			.orElse(null);
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
		return getAllSourceNodes(forwardNodeEntity).stream()
			.filter(sourceNode -> Objects.equals(sourceNode.getId(), dataId))
			.findFirst()
			.orElse(null);
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
