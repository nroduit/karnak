/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profiles;

import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.util.TagUtils;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.action.Add;
import org.karnak.backend.model.action.Keep;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.TagActionMap;
import org.karnak.backend.model.standard.StandardDICOM;

@Slf4j
public class AddPrivateTag extends AbstractProfileItem {

	private boolean tagAdded;

    private String currentInstanceUID;

	private static final String LOG_PATTERN = "SOPInstanceUID={} TAG={} ACTION={} REASON={}";

	public AddPrivateTag(ProfileElementEntity profileElementEntity) throws ProfileException {
		super(profileElementEntity);

		TagActionMap tagsAction = new TagActionMap();
		ActionItem actionByDefault = new Keep("K");
		profileValidation();
		mapTagsToAction(tagsAction, null, actionByDefault);
        this.currentInstanceUID = "";
        this.tagAdded = false;
	}

	@Override
	public @Nullable ActionItem getAction(Attributes dcm, Attributes dcmCopy, int tag, HMAC hmac) {
        String currentUID = dcm.getString(Tag.SOPInstanceUID);
        if (!currentInstanceUID.equals(currentUID) && currentUID != null) {
            currentInstanceUID = currentUID;
            tagAdded = false;
        }
        if (!tagAdded) {
			IncludedTagEntity t = tagEntities.getFirst();
			String tagValue = StandardDICOM.cleanTagPath(t.getTagValue());

			String value = "";
			String privateCreator = null;
			VR vr = null;
			for (ArgumentEntity ae : argumentEntities) {
				if ("value".equals(ae.getArgumentKey())) {
					value = ae.getArgumentValue();
				}
				else if ("vr".equals(ae.getArgumentKey())) {
					vr = VR.valueOf(ae.getArgumentValue());
				}
				else if ("privateCreator".equals(ae.getArgumentKey())) {
					privateCreator = ae.getArgumentValue();
				}
			}

			int creatorTag = TagUtils.creatorTagOf(TagUtils.intFromHexString(tagValue));

			if (privateCreator != null && dcm.contains(creatorTag)
					&& !dcm.getString(creatorTag).equals(privateCreator)) {
				// Collision between multiple Private Creator Tags, do not add the current
				// tag and log a warning
				tagAdded = true;
				if (log.isWarnEnabled()) {
					log.warn(LOG_PATTERN, dcm.getString(Tag.SOPInstanceUID), tagValue, "A",
							"Tag not added, PrivateCreatorID collision " + TagUtils.toString(creatorTag) + " existing: "
									+ dcm.getString(creatorTag) + " - new: " + privateCreator);
				}
			}
			else {
				tagAdded = true;
				return new Add("A", TagUtils.intFromHexString(tagValue), vr, value, privateCreator);
			}
		}
		return null;
	}

	@Override
	public void profileValidation() throws ProfileException {
		if (argumentEntities == null || argumentEntities.size() < 2) {
			throw new ProfileException(
					"Cannot build the profile " + codeName + ": Need to specify value and vr argument");
		}
		if (tagEntities == null || tagEntities.size() > 1) {
			throw new ProfileException("Cannot build the profile " + codeName + ": Exactly one tag is required");
		}

		if (!TagUtils.isPrivateTag(
				TagUtils.intFromHexString(StandardDICOM.cleanTagPath(tagEntities.getFirst().getTagValue())))) {
			throw new ProfileException("Cannot build the profile " + codeName + ": the tag "
					+ tagEntities.getFirst().getTagValue() + " is not a private tag");
		}

		validateCondition();
	}

}
