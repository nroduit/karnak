/*
 * Copyright (c) 2021 Karnak Team and other contributors.
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
import org.karnak.backend.config.AppConfig;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.action.Add;
import org.karnak.backend.model.action.Keep;
import org.karnak.backend.model.expression.ExprCondition;
import org.karnak.backend.model.expression.ExpressionError;
import org.karnak.backend.model.expression.ExpressionResult;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.TagActionMap;
import org.karnak.backend.model.standard.AttributeDetail;
import org.karnak.backend.model.standard.StandardDICOM;

@Slf4j
public class AddTag extends AbstractProfileItem {

	private final TagActionMap tagsAction;

	private final ActionItem actionByDefault;

	private boolean tagAdded;

	private final StandardDICOM standardDICOM;

	private static final String LOG_PATTERN = "SOPInstanceUID={} TAG={} ACTION={} REASON={}";

	public AddTag(ProfileElementEntity profileElementEntity) throws ProfileException {
		super(profileElementEntity);
		standardDICOM = AppConfig.getInstance().getStandardDICOM();

		tagsAction = new TagActionMap();
		actionByDefault = new Keep("K");
		profileValidation();
		setActionHashMap();
	}

	private void setActionHashMap() {

		if (tagEntities != null && !tagEntities.isEmpty()) {
			for (IncludedTagEntity tag : tagEntities) {
				tagsAction.put(tag.getTagValue(), actionByDefault);
			}
		}
	}

	@Override
	public ActionItem getAction(Attributes dcm, Attributes dcmCopy, int tag, HMAC hmac) {
		if (!tagAdded) {
			IncludedTagEntity t = tagEntities.getFirst();
			String tagValue = StandardDICOM.cleanTagPath(t.getTagValue());

			if (!standardDICOM.getAttributesBySOP(dcm.getString(Tag.SOPClassUID), tagValue).isEmpty()) {

				String value = "";
				VR vr = VR.valueOf(standardDICOM.getAttributeDetail(tagValue).getValueRepresentation());
				for (ArgumentEntity ae : argumentEntities) {
					if ("value".equals(ae.getArgumentKey())) {
						value = ae.getArgumentValue();
					}
				}
				tagAdded = true;
				return new Add("A", TagUtils.intFromHexString(tagValue), vr, value);
			}
			else {
				// Tag cannot be added in this instance, flag it as added so that the
				// action is not applied on every attribute in the instance
				tagAdded = true;
				if (log.isWarnEnabled()) {
					log.warn(LOG_PATTERN, dcm.getString(Tag.SOPInstanceUID), tagValue, "A",
							"Tag not added, it is not defined in current SOP " + dcm.getString(Tag.SOPClassUID));
				}
			}
		}
		return null;
	}

	@Override
	public final void profileValidation() throws ProfileException {
		if (argumentEntities == null || argumentEntities.isEmpty()) {
			throw new ProfileException("Cannot build the profile " + codeName + ": Need to specify value argument");
		}

		AttributeDetail attr = standardDICOM
			.getAttributeDetail(StandardDICOM.cleanTagPath(tagEntities.getFirst().getTagValue()));

		if (attr == null) {
			throw new ProfileException("Cannot build the profile " + codeName + ": the tag "
					+ tagEntities.getFirst().getTagValue() + " does not exist in the DICOM Standard");
		}
		else {
			try {
				// The VR is currently retrieved from the DICOM Standard, in a very few
				// cases, we cannot infer this value
				// It should only concern fields that would not be included in profiles
				VR.valueOf(attr.getValueRepresentation());
			}
			catch (IllegalArgumentException e) {
				throw new ProfileException("Cannot build the profile " + codeName + ": the tag "
						+ tagEntities.getFirst().getTagValue() + " is not supported and cannot be added");
			}
		}

		final ExpressionError expressionError = ExpressionResult.isValid(condition, new ExprCondition(new Attributes()),
				Boolean.class);
		if (condition != null && !expressionError.isValid()) {
			throw new ProfileException(expressionError.getMsg());
		}
	}

}
