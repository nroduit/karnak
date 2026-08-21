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

import org.karnak.backend.data.entity.DicomSourceNodeEntity;

/**
 * Maps between {@link DicomSourceNodeEntity} and {@link DicomSourceNodeModel}.
 */
public final class DicomSourceNodeMapper {

	private DicomSourceNodeMapper() {
	}

	public static DicomSourceNodeModel toModel(DicomSourceNodeEntity entity) {
		if (entity == null) {
			return null;
		}
		DicomSourceNodeModel model = new DicomSourceNodeModel();
		model.setUuid(entity.getUuid());
		model.setDescription(entity.getDescription());
		model.setAeTitle(entity.getAeTitle());
		model.setHostname(entity.getHostname());
		model.setCheckHostname(entity.getCheckHostname());
		return model;
	}

	public static DicomSourceNodeEntity toEntity(DicomSourceNodeModel model) {
		if (model == null) {
			return null;
		}
		DicomSourceNodeEntity entity = new DicomSourceNodeEntity();
		entity.setDescription(model.getDescription());
		entity.setAeTitle(model.getAeTitle());
		entity.setHostname(model.getHostname());
		entity.setCheckHostname(model.getCheckHostname());
		return entity;
	}

}




