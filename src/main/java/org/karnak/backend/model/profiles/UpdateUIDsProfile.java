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
import org.karnak.backend.model.action.AbstractAction;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.action.Remove;
import org.karnak.backend.model.action.ReplaceNull;
import org.karnak.backend.model.action.UID;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.TagActionMap;
import org.karnak.backend.model.profilepipe.TagPath;

/**
 * Profile element {@code replace.uid}: applies a UID action ({@code U} to generate a new
 * UID, {@code X} to remove, {@code Z} to empty) to the configured tags. Tags may be plain
 * tags, patterns or tag paths, and the excluded tags are left untouched.
 */
public class UpdateUIDsProfile extends AbstractProfileItem {

	private final TagActionMap tagsAction;

	private final TagActionMap exceptedTagsAction;

	public UpdateUIDsProfile(ProfileElementEntity profileElementEntity) throws ProfileException {
		super(profileElementEntity);
		tagsAction = new TagActionMap();
		exceptedTagsAction = new TagActionMap();
		ActionItem actionByDefault = AbstractAction.convertAction(this.action);
		profileValidation();
		if (actionByDefault != null) {
			mapTagsToAction(tagsAction, exceptedTagsAction, actionByDefault);
		}
	}

	@Override
	public @Nullable ActionItem getAction(Attributes dcm, Attributes original, int tag, HMAC hmac) {
		return getAction(dcm, original, tag, hmac, TagPath.ROOT);
	}

	@Override
	public @Nullable ActionItem getAction(Attributes dcm, Attributes original, int tag, HMAC hmac, TagPath path) {
		ActionItem action = tagMap.get(tag);
		if (action != null) {
			return action;
		}
		if (exceptedTagsAction.get(tag, path) != null) {
			return null;
		}
		return tagsAction.get(tag, path);
	}

	@Override
	public @Nullable ActionItem put(int tag, ActionItem action) {
		if (!isConsistent(action)) {
			throw new IllegalStateException(String.format("The action %s is not consistent !", action));
		}
		return tagMap.put(tag, action);
	}

	@Override
	public void profileValidation() throws ProfileException {
		String errorMessage = "Cannot build the profile " + codeName;
		if (tagEntities != null && !tagEntities.isEmpty()) {
			if (action == null) {
				throw new ProfileException(errorMessage + ": Unknown Action");
			}
			if (!isConsistent(AbstractAction.convertAction(action))) {
				throw new ProfileException(errorMessage + ": the action " + action
						+ " is not consistent, only U (new UID), X (remove) and Z (replace with null) are allowed");
			}
		}
		super.profileValidation();
	}

	private static boolean isConsistent(@Nullable ActionItem action) {
		return action instanceof UID || action instanceof Remove || action instanceof ReplaceNull;
	}

}
