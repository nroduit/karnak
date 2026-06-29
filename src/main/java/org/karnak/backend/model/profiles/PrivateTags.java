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
import org.dcm4che3.util.TagUtils;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.AbstractAction;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.TagActionMap;

public class PrivateTags extends AbstractProfileItem {

	private final TagActionMap tagsAction;

	private final TagActionMap exceptedTagsAction;

	private final ActionItem actionByDefault;

	public PrivateTags(ProfileElementEntity profileElementEntity) throws ProfileException {
		super(profileElementEntity);
		tagsAction = new TagActionMap();
		exceptedTagsAction = new TagActionMap();
		actionByDefault = AbstractAction.convertAction(this.action);
		profileValidation();
		mapTagsToAction(tagsAction, exceptedTagsAction, actionByDefault);
	}

	@Override
	public @Nullable ActionItem getAction(Attributes dcm, Attributes dcmCopy, int tag, HMAC hmac) {
		if (TagUtils.isPrivateGroup(tag)) {
			if (!tagsAction.isEmpty() && exceptedTagsAction.isEmpty()) {
				return tagsAction.get(tag);
			}

			if (tagsAction.isEmpty() && !exceptedTagsAction.isEmpty()) {
				if (exceptedTagsAction.get(tag) != null) {
					return null;
				}
			}

			if (!tagsAction.isEmpty() && !exceptedTagsAction.isEmpty()) {
				// TODO check tag value?
				if (exceptedTagsAction.get(tag) == null) {
					return tagsAction.get(tag);
				}
				return null;
			}
			return actionByDefault;
		}
		return null;
	}

	@Override
	public void profileValidation() throws ProfileException {
		if (action == null) {
			throw new ProfileException("Cannot build the profile " + codeName + ": Unknown Action");
		}

		validateCondition();
	}

}
