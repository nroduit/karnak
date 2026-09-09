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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ExcludedTagEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.Keep;
import org.karnak.backend.model.action.Remove;
import org.karnak.backend.model.action.ReplaceNull;
import org.karnak.backend.model.action.UID;

@DisplayNameGeneration(ReplaceUnderscores.class)
class UpdateUIDsProfileTest {

	private static ProfileElementEntity element(String action) {
		return new ProfileElementEntity("name", "replace.uid", null, action, null, 0, null);
	}

	private static UpdateUIDsProfile profile() throws ProfileException {
		return new UpdateUIDsProfile(element(null));
	}

	@Test
	void applies_the_configured_action_to_the_configured_tags() throws ProfileException {
		ProfileElementEntity element = element("U");
		element.addIncludedTag(new IncludedTagEntity("(0020,000D)", element));
		element.addIncludedTag(new IncludedTagEntity("(0020,000E)", element));
		element.addExceptedtags(new ExcludedTagEntity("(0020,000E)", element));
		UpdateUIDsProfile profile = new UpdateUIDsProfile(element);

		assertInstanceOf(UID.class, profile.getAction(new Attributes(), new Attributes(), Tag.StudyInstanceUID, null));
		assertNull(profile.getAction(new Attributes(), new Attributes(), Tag.SeriesInstanceUID, null));
		assertNull(profile.getAction(new Attributes(), new Attributes(), Tag.SOPInstanceUID, null));
	}

	@Test
	void rejects_an_inconsistent_configured_action() {
		ProfileElementEntity element = element("K");
		element.addIncludedTag(new IncludedTagEntity("(0020,000D)", element));

		assertThrows(ProfileException.class, () -> new UpdateUIDsProfile(element));
	}

	@Test
	void rejects_configured_tags_without_action() {
		ProfileElementEntity element = element(null);
		element.addIncludedTag(new IncludedTagEntity("(0020,000D)", element));

		assertThrows(ProfileException.class, () -> new UpdateUIDsProfile(element));
	}

	@Test
	void accepts_uid_remove_and_replace_null_actions() throws ProfileException {
		UpdateUIDsProfile profile = profile();

		profile.put(Tag.StudyInstanceUID, new UID("U"));
		profile.put(Tag.SeriesInstanceUID, new Remove("X"));
		profile.put(Tag.SOPInstanceUID, new ReplaceNull("Z"));

		assertInstanceOf(UID.class, profile.getAction(new Attributes(), new Attributes(), Tag.StudyInstanceUID, null));
		assertInstanceOf(Remove.class,
				profile.getAction(new Attributes(), new Attributes(), Tag.SeriesInstanceUID, null));
		assertInstanceOf(ReplaceNull.class,
				profile.getAction(new Attributes(), new Attributes(), Tag.SOPInstanceUID, null));
	}

	@Test
	void rejects_an_inconsistent_action() throws ProfileException {
		UpdateUIDsProfile profile = profile();

		assertThrows(IllegalStateException.class, () -> profile.put(Tag.StudyInstanceUID, new Keep("K")));
	}

	@Test
	void returns_null_for_an_unmapped_tag() throws ProfileException {
		assertNull(profile().getAction(new Attributes(), new Attributes(), Tag.StudyInstanceUID, null));
	}

	@Test
	void removes_and_clears_mapped_actions() throws ProfileException {
		UpdateUIDsProfile profile = profile();
		UID uid = new UID("U");
		profile.put(Tag.StudyInstanceUID, uid);

		assertSame(uid, profile.remove(Tag.StudyInstanceUID));
		assertNull(profile.getAction(new Attributes(), new Attributes(), Tag.StudyInstanceUID, null));

		profile.put(Tag.SeriesInstanceUID, new Remove("X"));
		profile.clearTagMap();
		assertNull(profile.getAction(new Attributes(), new Attributes(), Tag.SeriesInstanceUID, null));
	}

}
