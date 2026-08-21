/*
 * Copyright (c) 2024-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profile;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;

/**
 * Maps between {@link ProfileEntity} and {@link ProfileModel}.
 */
public final class ProfileMapper {

	private ProfileMapper() {
	}

	public static ProfileModel toModel(ProfileEntity entity) {
		if (entity == null) {
			return null;
		}
		ProfileModel model = new ProfileModel();
		model.setUuid(entity.getUuid());
		model.setName(entity.getName());
		model.setVersion(entity.getVersion());
		model.setMinimumKarnakVersion(entity.getMinimumKarnakVersion());
		model.setByDefault(entity.getByDefault());
		model.setProfileElements(entity.getProfileElementEntities()
			.stream()
			.sorted(Comparator.comparing(ProfileElementEntity::getPosition,
					Comparator.nullsLast(Comparator.naturalOrder())))
			.map(ProfileElementMapper::toModel)
			.collect(Collectors.toCollection(LinkedHashSet::new)));
		return model;
	}

	public static ProfileEntity toEntity(ProfileModel model) {
		if (model == null) {
			return null;
		}
		ProfileEntity entity = new ProfileEntity();
		entity.setName(model.getName());
		entity.setVersion(model.getVersion());
		entity.setMinimumKarnakVersion(model.getMinimumKarnakVersion());
		// uuid, id, byDefault, group, profile elements and masks are set by the entity
		// constructor, the caller, or their own services/endpoints, not from the model.
		return entity;
	}

}

