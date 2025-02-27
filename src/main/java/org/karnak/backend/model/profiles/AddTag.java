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
import org.dcm4che3.data.VR;
import org.dcm4che3.util.TagUtils;
import org.karnak.backend.config.AppConfig;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.action.Add;
import org.karnak.backend.model.action.Keep;
import org.karnak.backend.model.expression.ExprCondition;
import org.karnak.backend.model.expression.ExpressionError;
import org.karnak.backend.model.expression.ExpressionResult;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.TagActionMap;
import org.karnak.backend.model.standard.StandardDICOM;

@Slf4j
public class AddTag extends AbstractProfileItem {

	private final TagActionMap tagsAction;

	private final ActionItem actionByDefault;

	private boolean tagAdded = false;

	private final StandardDICOM standardDICOM;

	public AddTag(ProfileElementEntity profileElementEntity) throws Exception {
		super(profileElementEntity);
		tagsAction = new TagActionMap();
		actionByDefault = new Keep("K");
		profileValidation();
		setActionHashMap();

		standardDICOM = AppConfig.getInstance().getStandardDICOM();
	}

	private void setActionHashMap() throws Exception {

		if (tagEntities != null && tagEntities.size() > 0) {
			for (IncludedTagEntity tag : tagEntities) {
				tagsAction.put(tag.getTagValue(), actionByDefault);
			}
		}
	}

	@Override
	public ActionItem getAction(Attributes dcm, Attributes dcmCopy, int tag, HMAC hmac) {
		if (!tagAdded) {
			IncludedTagEntity t = tagEntities.getFirst();
			String tagValue = t.getTagValue().replaceAll("[(),]", "");

			String value = "";
			VR vr = null;
			for (ArgumentEntity ae : argumentEntities) {
				if ("value".equals(ae.getArgumentKey())) {
					value = ae.getArgumentValue();
				} else if ("vr".equals(ae.getArgumentKey())) {
					vr = VR.valueOf(ae.getArgumentValue());
				}
			}
			if (vr == null) {
				vr = VR.valueOf(standardDICOM.getAttributeDetail(tagValue).getValueRepresentation());
			}
			tagAdded = true;
			return new Add("A", TagUtils.intFromHexString(tagValue), vr, value);
		}
		return null;
	}

	@Override
	public void profileValidation() throws Exception {
		if (argumentEntities == null || argumentEntities.isEmpty()) {
			throw new Exception("Cannot build the profile " + codeName + ": Need to specify value argument");
		}
		if (tagEntities != null && tagEntities.size() > 1) {
			throw new Exception("Cannot build the profile " + codeName + ": Exactly one tag is required");
		}

		final ExpressionError expressionError = ExpressionResult.isValid(condition, new ExprCondition(new Attributes()),
				Boolean.class);
		if (condition != null && !expressionError.isValid()) {
			throw new Exception(expressionError.getMsg());
		}
	}
}
