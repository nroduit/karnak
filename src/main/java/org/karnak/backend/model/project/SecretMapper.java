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

import org.karnak.backend.data.entity.SecretEntity;
import org.karnak.backend.model.profilepipe.HMAC;

/**
 * Maps between {@link SecretEntity} and {@link SecretModel}.
 */
public final class SecretMapper {

	private SecretMapper() {
	}

	public static SecretModel toModel(SecretEntity entity) {
		if (entity == null) {
			return null;
		}
		SecretModel model = new SecretModel();
		model.setUuid(entity.getUuid());
		model.setKey(HMAC.showHexKey(HMAC.byteToHex(entity.getSecretKey())));
		model.setCreationDate(entity.getCreationDate());
		model.setActive(entity.isActive());
		return model;
	}

}
