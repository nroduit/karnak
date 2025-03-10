/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profiles;

import java.awt.Color;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.karnak.backend.data.entity.ExcludedTagEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.AbstractAction;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.expression.ExprCondition;
import org.karnak.backend.model.expression.ExpressionError;
import org.karnak.backend.model.expression.ExpressionResult;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.TagActionMap;

@Slf4j
public class ActionTags extends AbstractProfileItem {

	private final TagActionMap tagsAction;

	private final TagActionMap exceptedTagsAction;

	private final ActionItem actionByDefault;

	public ActionTags(ProfileElementEntity profileElementEntity) throws ProfileException {
		super(profileElementEntity);
		tagsAction = new TagActionMap();
		exceptedTagsAction = new TagActionMap();
		actionByDefault = AbstractAction.convertAction(this.action);
		profileValidation();
		setActionHashMap();
	}

	public static String color2Hexadecimal(Color c, boolean alpha) {
		int val = c == null ? 0 : alpha ? c.getRGB() : c.getRGB() & 0x00ffffff;
		return Integer.toHexString(val);
	}

	public static Color hexadecimal2Color(String hexColor) {
		int intValue = 0xff000000;

		try {
			if (hexColor != null && hexColor.length() > 6) {
				intValue = (int) (Long.parseLong(hexColor, 16) & 0xffffffff);
			}
			else {
				intValue |= Integer.parseInt(hexColor, 16);
			}
		}
		catch (NumberFormatException e) {
			log.error("Cannot parse color {} into int", hexColor); // $NON-NLS-1$
		}
		return new Color(intValue, true);
	}

	private void setActionHashMap() {
		for (IncludedTagEntity tag : tagEntities) {
			tagsAction.put(tag.getTagValue(), actionByDefault);
		}
		if (excludedTagEntities != null) {
			for (ExcludedTagEntity tag : excludedTagEntities) {
				exceptedTagsAction.put(tag.getTagValue(), actionByDefault);
			}
		}
	}

	@Override
	public ActionItem getAction(Attributes dcm, Attributes dcmCopy, int tag, HMAC hmac) {
		if (exceptedTagsAction.get(tag) == null) {
			return tagsAction.get(tag);
		}
		return null;
	}

	@Override
	public void profileValidation() throws ProfileException {
		String errorMessage = "Cannot build the profile ";
		if (action == null && (tagEntities == null || tagEntities.isEmpty())) {
			throw new ProfileException(errorMessage + codeName + ": Unknown Action and no tags defined");
		}

		if (action == null) {
			throw new ProfileException(errorMessage + codeName + ": Unknown Action");
		}

		if (tagEntities == null || tagEntities.isEmpty()) {
			throw new ProfileException(errorMessage + codeName + ": No tags defined");
		}

		final ExpressionError expressionError = ExpressionResult.isValid(condition, new ExprCondition(new Attributes()),
				Boolean.class);
		if (condition != null && !expressionError.isValid()) {
			throw new ProfileException(expressionError.getMsg());
		}
	}

}
