/*
 * Copyright (c) 2024-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.forwardnode;

import java.util.stream.Collectors;
import org.karnak.backend.data.entity.ForwardNodeEntity;

/**
 * Maps between {@link ForwardNodeEntity} and {@link ForwardNodeModel}.
 */
public final class ForwardNodeMapper {

	private ForwardNodeMapper() {
	}

	public static ForwardNodeModel toModel(ForwardNodeEntity entity) {
		if (entity == null) {
			return null;
		}
		ForwardNodeModel model = new ForwardNodeModel();
		model.setUuid(entity.getUuid());
		model.setFwdDescription(entity.getFwdDescription());
		model.setFwdAeTitle(entity.getFwdAeTitle());
		model.setSourceNodes(entity.getSourceNodes()
			.stream()
			.map(DicomSourceNodeMapper::toModel)
			.collect(Collectors.toSet()));
		model.setDestinations(entity.getDestinationEntities()
			.stream()
			.map(DestinationMapper::toModel)
			.collect(Collectors.toSet()));
		return model;
	}

	public static ForwardNodeEntity toEntity(ForwardNodeModel model) {
		if (model == null) {
			return null;
		}
		ForwardNodeEntity entity = new ForwardNodeEntity();
		entity.setFwdDescription(model.getFwdDescription());
		entity.setFwdAeTitle(model.getFwdAeTitle());
		return entity;
	}

}




