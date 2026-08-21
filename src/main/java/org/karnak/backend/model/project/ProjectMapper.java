/*
 * Copyright (c) 2024-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.project;

import java.util.stream.Collectors;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.ProjectEntity;

/**
 * Maps between {@link ProjectEntity} and {@link ProjectModel}.
 */
public final class ProjectMapper {

	private ProjectMapper() {
	}

	public static ProjectModel toModel(ProjectEntity entity) {
		if (entity == null) {
			return null;
		}
		ProjectModel model = new ProjectModel();
		model.setUuid(entity.getUuid());
		model.setName(entity.getName());
		model.setProfileUuid(entity.getProfileEntity() != null ? entity.getProfileEntity().getUuid() : null);
		if (entity.getDestinationEntities() != null) {
			model.setDestinationUuids(entity.getDestinationEntities()
				.stream()
				.map(DestinationEntity::getUuid)
				.collect(Collectors.toSet()));
		}
		return model;
	}

	public static ProjectEntity toEntity(ProjectModel model) {
		if (model == null) {
			return null;
		}
		ProjectEntity entity = new ProjectEntity();
		entity.setName(model.getName());
		// uuid, id, secrets, destinations and group are set by the entity constructor,
		// the caller, or their own services/endpoints, not from the model. The profile
		// association is resolved from profileUuid by the caller (ProfilePipeService
		// lookup required).
		return entity;
	}

}

