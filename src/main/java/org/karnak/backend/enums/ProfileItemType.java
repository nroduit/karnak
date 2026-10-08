/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.enums;

import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NullUnmarked;
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
import org.karnak.backend.model.profiles.ReplaceApi;
import org.karnak.backend.model.profiles.UpdateUIDsProfile;

@NullUnmarked
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
	ADD_PRIVATE_TAG(AddPrivateTag.class, "action.add.private.tag", null, null),
	REPLACE_API(ReplaceApi.class, "action.replace.api", null, null);

	/** Alias of the Basic DICOM confidentiality profile, which must run last. */
	public static final String BASIC_DICOM_ALIAS = "basic.dicom.profile";

	@Getter
	private final Class<? extends ProfileItem> profileClass;

	@Getter
	private final String classAlias;

	private final String codeValue;

	private final String codeMeaning;

	ProfileItemType(Class<? extends ProfileItem> profileClass, String alias, String codeValue, String codeMeaning) {
		this.profileClass = profileClass;
		this.classAlias = alias;
		this.codeValue = codeValue;
		this.codeMeaning = codeMeaning;
	}

	/** Types that may appear at most once in a pipeline (and need no specific name). */
	private static final Set<ProfileItemType> UNIQUE_TYPES = Set.of(BASIC_DICOM, CLEAN_PIXEL_DATA, DEFACING);

	/** Whether this type may appear at most once in a profile. */
	public boolean isUnique() {
		return UNIQUE_TYPES.contains(this);
	}

	/** Whether the type with this alias may appear at most once in a profile. */
	public static boolean isUnique(String alias) {
		ProfileItemType type = getType(alias);
		return type != null && type.isUnique();
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
		ProfileItemType t = getType(alias);
		return t != null ? t.codeValue : null;
	}

	public static String getCodeMeaning(String alias) {
		ProfileItemType t = getType(alias);
		return t != null ? t.codeMeaning : null;
	}

}
