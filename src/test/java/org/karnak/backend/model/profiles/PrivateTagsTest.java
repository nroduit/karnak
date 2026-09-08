/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profiles;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ExcludedTagEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.Remove;

@DisplayNameGeneration(ReplaceUnderscores.class)
class PrivateTagsTest {

	// A tag in an odd (private) group.
	private static final int PRIVATE_TAG = 0x00090010;

	private static final int PUBLIC_TAG = Tag.PatientName;

	private static ProfileElementEntity element(String action) {
		return new ProfileElementEntity("name", "action.private.tags", null, action, null, 0, null);
	}

	@Test
	void rejects_a_missing_action() {
		assertThrows(ProfileException.class, () -> new PrivateTags(element(null)));
	}

	@Test
	void rejects_an_unknown_action_symbol() {
		assertThrows(ProfileException.class, () -> new PrivateTags(element("DDum")));
	}

	@Nested
	class GetAction {

		@Test
		void applies_the_default_action_to_any_private_tag_when_no_tags_listed() throws ProfileException {
			PrivateTags profile = new PrivateTags(element("X"));

			assertInstanceOf(Remove.class, profile.getAction(new Attributes(), new Attributes(), PRIVATE_TAG, null));
		}

		@Test
		void ignores_public_tags() throws ProfileException {
			PrivateTags profile = new PrivateTags(element("X"));

			assertNull(profile.getAction(new Attributes(), new Attributes(), PUBLIC_TAG, null));
		}

		@Test
		void applies_the_action_only_to_included_private_tags() throws ProfileException {
			ProfileElementEntity element = element("X");
			element.addIncludedTag(new IncludedTagEntity("(0009,0010)", element));
			PrivateTags profile = new PrivateTags(element);

			assertInstanceOf(Remove.class, profile.getAction(new Attributes(), new Attributes(), PRIVATE_TAG, null));
			assertNull(profile.getAction(new Attributes(), new Attributes(), 0x00090020, null));
		}

		@Test
		void skips_excepted_private_tags() throws ProfileException {
			ProfileElementEntity element = element("X");
			element.addExceptedtags(new ExcludedTagEntity("(0009,0010)", element));
			PrivateTags profile = new PrivateTags(element);

			assertNull(profile.getAction(new Attributes(), new Attributes(), PRIVATE_TAG, null));
		}

	}

}