/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.profilepipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Set;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.springframework.boot.test.context.SpringBootTest;
import org.weasis.dicom.param.AttributeEditorContext;

/**
 * The scoping of a profile item to a given sequence, end to end on a real dataset holding
 * the same tag at the root and inside two different sequences.
 *
 * <p>
 * Building a {@link Profile} reaches for the {@code AppConfig} singleton, which a
 * {@code @SpringBootTest} context provides.
 */
@SpringBootTest
@DisplayNameGeneration(ReplaceUnderscores.class)
class ProfileSequenceScopingTest {

	private static final String ROOT_VALUE = "rootStudy";

	private static final String IN_REQUEST_VALUE = "requestStudy";

	private static final String IN_REFERENCED_VALUE = "referencedStudy";

	/**
	 * StudyID at the root, inside a RequestAttributesSequence item and inside a
	 * ReferencedStudySequence item.
	 */
	private static Attributes dataset() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.StudyID, VR.SH, ROOT_VALUE);

		Sequence requestAttributes = dcm.newSequence(Tag.RequestAttributesSequence, 1);
		Attributes requestItem = new Attributes();
		requestItem.setString(Tag.StudyID, VR.SH, IN_REQUEST_VALUE);
		requestAttributes.add(requestItem);

		Sequence referencedStudy = dcm.newSequence(Tag.ReferencedStudySequence, 1);
		Attributes referencedItem = new Attributes();
		referencedItem.setString(Tag.StudyID, VR.SH, IN_REFERENCED_VALUE);
		referencedStudy.add(referencedItem);

		return dcm;
	}

	/** A profile removing {@code tagValue}, which may be a bare tag or a path. */
	private static Profile removeProfile(String tagValue) {
		ProfileElementEntity element = new ProfileElementEntity("remove", "action.on.specific.tags", null, "X", null, 0,
				null);
		element.addIncludedTag(new IncludedTagEntity(tagValue, element));

		ProfileEntity profileEntity = new ProfileEntity();
		profileEntity.setProfileElementEntities(Set.of(element));
		return new Profile(profileEntity);
	}

	private static Attributes applyRemoveOf(String tagValue) {
		Attributes dcm = dataset();
		removeProfile(tagValue).applyAction(dcm, new Attributes(dcm), null, null, null,
				new AttributeEditorContext("tsuid", null, null));
		return dcm;
	}

	private static String studyIdIn(Attributes dcm, int sequenceTag) {
		return dcm.getSequence(sequenceTag).get(0).getString(Tag.StudyID);
	}

	@Test
	void a_bare_tag_is_removed_at_every_depth() {
		Attributes dcm = applyRemoveOf("(0020,0010)");

		assertNull(dcm.getString(Tag.StudyID));
		assertNull(studyIdIn(dcm, Tag.RequestAttributesSequence));
		assertNull(studyIdIn(dcm, Tag.ReferencedStudySequence));
	}

	@Test
	void a_path_is_removed_only_inside_the_named_sequence() {
		Attributes dcm = applyRemoveOf("(0040,0275).(0020,0010)");

		assertEquals(ROOT_VALUE, dcm.getString(Tag.StudyID));
		assertNull(studyIdIn(dcm, Tag.RequestAttributesSequence));
		assertEquals(IN_REFERENCED_VALUE, studyIdIn(dcm, Tag.ReferencedStudySequence));
	}

	@Test
	void an_anchored_tag_is_removed_only_at_the_root() {
		Attributes dcm = applyRemoveOf(".(0020,0010)");

		assertNull(dcm.getString(Tag.StudyID));
		assertEquals(IN_REQUEST_VALUE, studyIdIn(dcm, Tag.RequestAttributesSequence));
		assertEquals(IN_REFERENCED_VALUE, studyIdIn(dcm, Tag.ReferencedStudySequence));
	}

	@Test
	void a_double_star_removes_everywhere_but_at_the_root() {
		Attributes dcm = applyRemoveOf(".**.(0020,0010)");

		assertEquals(ROOT_VALUE, dcm.getString(Tag.StudyID));
		assertNull(studyIdIn(dcm, Tag.RequestAttributesSequence));
		assertNull(studyIdIn(dcm, Tag.ReferencedStudySequence));
	}

}