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
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.action.Remove;
import org.karnak.backend.model.action.ReplaceNull;
import org.karnak.backend.model.action.UID;
import org.karnak.backend.model.profilepipe.HMAC;

public class UpdateUIDsProfile extends AbstractProfileItem {

	public UpdateUIDsProfile(ProfileElementEntity profileElementEntity) {
		super(profileElementEntity);
	}

	@Override
	public @Nullable ActionItem getAction(Attributes dcm, Attributes dcmCopy, int tag, HMAC hmac) {
		return tagMap.get(tag);
	}

	@Override
	public @Nullable ActionItem put(int tag, ActionItem action) {
		if (!(action instanceof UID) && !(action instanceof Remove) && !(action instanceof ReplaceNull)) {
			throw new IllegalStateException(String.format("The action %s is not consistent !", action));
		}
		return tagMap.put(tag, action);
	}

}
