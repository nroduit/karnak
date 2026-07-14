/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profiles;

import org.dcm4che3.data.Attributes;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.profilepipe.HMAC;

public class CleanPixelData extends AbstractProfileItem {

	private final boolean automaticMasksGeneration;

	public CleanPixelData(ProfileElementEntity profileElementEntity) throws ProfileException {
		super(profileElementEntity);
		this.automaticMasksGeneration = argumentEntities != null && argumentEntities.stream()
			.filter(argument -> "automaticMasksGeneration".equals(argument.getArgumentKey()))
			.map(argument -> Boolean.parseBoolean(argument.getArgumentValue()))
			.findFirst()
			.orElse(false);
		profileValidation();
	}

	public boolean isAutomaticMasksGeneration() {
		return automaticMasksGeneration;
	}

	@Override
	public @Nullable ActionItem getAction(Attributes dcm, Attributes dcmCopy, int tag, HMAC hmac) {
		// Action handles in the DICOM content not in metadata.
		return null;
	}

}
