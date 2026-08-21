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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.ExcludedTagEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;

/**
 * Maps between {@link ProfileElementEntity} and {@link ProfileElementModel}.
 */
public final class ProfileElementMapper {

	private ProfileElementMapper() {
	}

	public static ProfileElementModel toModel(ProfileElementEntity entity) {
		if (entity == null) {
			return null;
		}
		ProfileElementModel model = new ProfileElementModel();
		model.setUuid(entity.getUuid());
		model.setName(entity.getName());
		model.setCodename(entity.getCodename());
		model.setCondition(entity.getCondition());
		model.setAction(entity.getAction());
		model.setOption(entity.getOption());
		model.setTags(entity.getIncludedTagEntities().stream().map(IncludedTagEntity::getTagValue).toList());
		model.setExcludedTags(entity.getExcludedTagEntities().stream().map(ExcludedTagEntity::getTagValue).toList());
		Map<String, String> arguments = new LinkedHashMap<>();
		entity.getArgumentEntities().forEach(argument -> arguments.put(argument.getArgumentKey(), argument.getArgumentValue()));
		model.setArguments(arguments);
		return model;
	}

	public static ProfileElementEntity toEntity(ProfileElementModel model) {
		if (model == null) {
			return null;
		}
		ProfileElementEntity entity = new ProfileElementEntity();
		entity.setName(model.getName());
		entity.setCodename(model.getCodename());
		entity.setCondition(model.getCondition());
		entity.setAction(model.getAction());
		entity.setOption(model.getOption());
		List<IncludedTagEntity> includedTagEntities = new ArrayList<>();
		if (model.getTags() != null) {
			model.getTags().forEach(tag -> includedTagEntities.add(new IncludedTagEntity(tag, null)));
		}
		entity.setIncludedTagEntities(includedTagEntities);
		List<ExcludedTagEntity> excludedTagEntities = new ArrayList<>();
		if (model.getExcludedTags() != null) {
			model.getExcludedTags().forEach(tag -> excludedTagEntities.add(new ExcludedTagEntity(tag, null)));
		}
		entity.setExcludedTagEntities(excludedTagEntities);
		List<ArgumentEntity> argumentEntities = new ArrayList<>();
		if (model.getArguments() != null) {
			model.getArguments()
				.forEach((key, value) -> argumentEntities.add(new ArgumentEntity(key, value, null)));
		}
		entity.setArgumentEntities(argumentEntities);
		// uuid, id, position and the parent profile association are set by the entity
		// constructor, the caller, or the service's own attach/save logic, not from the
		// model.
		return entity;
	}

}

