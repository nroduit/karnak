/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

/**
 * {@link MetadataDICOMObject} walks up the parent objects of a nested item to resolve a
 * value or build a tag path.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class MetadataDICOMObjectTest {

	private static Attributes nestedItem(Attributes parent) {
		Attributes item = new Attributes();
		Sequence seq = parent.newSequence(Tag.ReferencedImageSequence, 1);
		seq.add(item);
		return item;
	}

	@Test
	void get_value_reads_a_value_present_on_the_object() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.PatientName, VR.PN, "Doe^John");

		assertEquals("Doe^John", MetadataDICOMObject.getValue(dcm, Tag.PatientName));
	}

	@Test
	void get_value_falls_back_to_the_parent_object() {
		Attributes parent = new Attributes();
		parent.setString(Tag.PatientName, VR.PN, "Doe^John");
		Attributes item = nestedItem(parent);

		assertEquals("Doe^John", MetadataDICOMObject.getValue(item, Tag.PatientName));
	}

	@Test
	void get_value_returns_null_when_the_tag_is_nowhere() {
		Attributes parent = new Attributes();
		Attributes item = nestedItem(parent);

		assertNull(MetadataDICOMObject.getValue(item, Tag.PatientName));
	}

	@Test
	void get_tag_path_of_a_root_tag_is_just_the_tag() {
		Attributes dcm = new Attributes();

		assertEquals("(0010,0010)", MetadataDICOMObject.getTagPath(dcm, Tag.PatientName));
	}

	@Test
	void get_tag_path_of_a_nested_tag_includes_the_parent_sequence() {
		Attributes parent = new Attributes();
		Attributes item = nestedItem(parent);

		assertEquals("00081140:(0010,0010)", MetadataDICOMObject.getTagPath(item, Tag.PatientName));
	}

}