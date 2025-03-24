/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.enums;

import org.karnak.backend.model.profiles.ActionDates;
import org.karnak.backend.model.profiles.ActionTags;
import org.karnak.backend.model.profiles.AddPrivateTag;
import org.karnak.backend.model.profiles.AddTag;
import org.karnak.backend.model.profiles.BasicProfile;
import org.karnak.backend.model.profiles.CleanPixelData;
import org.karnak.backend.model.profiles.Defacing;
import org.karnak.backend.model.profiles.Expression;
import org.karnak.backend.model.profiles.PrivateTags;
import org.karnak.backend.model.profiles.ProfileItem;
import org.karnak.backend.model.profiles.UpdateUIDsProfile;

public enum ProfileItemType {

	BASIC_DICOM(BasicProfile.class, "basic.dicom.profile", "113100", "Basic Application Confidentiality Profile"),
	CLEAN_PIXEL_DATA(CleanPixelData.class, "clean.pixel.data", "113101", "Clean Pixel Data Option"),
	DEFACING(Defacing.class, "clean.recognizable.visual.features", "113102",
			"Clean Recognizable Visual Features Option"),
	REPLACE_UID(UpdateUIDsProfile.class, "replace.uid", null, null),
	ACTION_TAGS(ActionTags.class, "action.on.specific.tags", null, null),
	ACTION_PRIVATETAGS(PrivateTags.class, "action.on.privatetags", "113111", "Retain Safe Private Option"),
	ACTION_DATES(ActionDates.class, "action.on.dates", "113107",
			"Retain Longitudinal Temporal Information Modified Dates Option"),
	EXPRESSION_TAGS(Expression.class, "expression.on.tags", null, null),
	ADD_TAG(AddTag.class, "action.add.tag", null, null),
	ADD_PRIVATE_TAG(AddPrivateTag.class, "action.add.private.tag", null, null);

	private final Class<? extends ProfileItem> profileClass;

	private final String classAlias;

	private final String codeValue;

	private final String codeMeaning;

	ProfileItemType(Class<? extends ProfileItem> profileClass, String alias, String codeValue, String codeMeaning) {
		this.profileClass = profileClass;
		this.classAlias = alias;
		this.codeValue = codeValue;
		this.codeMeaning = codeMeaning;
	}

	public static ProfileItemType getType(String alias) {
		for (ProfileItemType t : ProfileItemType.values()) {
			if (t.classAlias.equals(alias)) {
				return t;
			}
		}
		return null;
	}

	public static String getCodeValue(String alias) {
		for (ProfileItemType t : ProfileItemType.values()) {
			if (t.classAlias.equals(alias)) {
				return t.codeValue;
			}
		}
		return null;
	}

	public static String getCodeMeaning(String alias) {
		for (ProfileItemType t : ProfileItemType.values()) {
			if (t.classAlias.equals(alias)) {
				return t.codeMeaning;
			}
		}
		return null;
	}

	public Class<? extends ProfileItem> getProfileClass() {
		return profileClass;
	}

	public String getClassAlias() {
		return classAlias;
	}

}
