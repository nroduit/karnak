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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DicomObjectToolsTest {

	private static Attributes patient() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.PatientName, VR.PN, "Doe^John");
		return dcm;
	}

	@Nested
	class ObjectEquality {

		@Test
		void two_equal_objects_are_equal() {
			assertTrue(DicomObjectTools.dicomObjectEquals(patient(), patient()));
		}

		@Test
		void two_different_objects_are_not_equal() {
			Attributes other = new Attributes();
			other.setString(Tag.PatientName, VR.PN, "Roe^Jane");

			assertFalse(DicomObjectTools.dicomObjectEquals(patient(), other));
		}

	}

	@Nested
	class TagResolution {

		@Test
		void resolves_a_hexadecimal_tag() {
			assertEquals(Tag.PatientName, DicomObjectTools.toTag("00100010"));
		}

		@Test
		void resolves_a_keyword() {
			assertEquals(Tag.PatientName, DicomObjectTools.toTag("PatientName"));
		}

		@Test
		void rejects_an_unknown_keyword() {
			assertThrows(IllegalArgumentException.class, () -> DicomObjectTools.toTag("NotARealKeyword"));
		}

		@Test
		void maps_a_list_of_keywords_to_tags() {
			assertEquals(List.of(Tag.PatientName, Tag.PatientID),
					DicomObjectTools.toTags(new String[] { "PatientName", "PatientID" }));
		}

	}

	@Nested
	class PathLookup {

		@Test
		void empty_path_is_rejected() {
			assertThrows(IllegalArgumentException.class, () -> DicomObjectTools.containsTagFromPath("", patient()));
		}

		@Test
		void finds_a_top_level_tag() {
			assertTrue(DicomObjectTools.containsTagFromPath("PatientName", patient()));
		}

		@Test
		void misses_a_top_level_tag_that_is_absent() {
			assertFalse(DicomObjectTools.containsTagFromPath("PatientID", patient()));
		}

		@Test
		void finds_a_tag_nested_one_level_in_a_sequence() {
			Attributes item = new Attributes();
			item.setString(Tag.ReferencedSOPClassUID, VR.UI, "1.2.3");
			Attributes dcm = new Attributes();
			Sequence seq = dcm.newSequence(Tag.ReferencedImageSequence, 1);
			seq.add(item);

			assertTrue(DicomObjectTools.containsTagFromPath("ReferencedImageSequence.ReferencedSOPClassUID", dcm));
			assertFalse(DicomObjectTools.containsTagFromPath("ReferencedImageSequence.PatientName", dcm));
		}

		@Test
		void finds_a_tag_nested_two_levels_in_sequences() {
			Attributes inner = new Attributes();
			inner.setString(Tag.ReferencedSOPInstanceUID, VR.UI, "1.2.3.4");
			Attributes mid = new Attributes();
			mid.newSequence(Tag.ReferencedImageSequence, 1).add(inner);
			Attributes dcm = new Attributes();
			dcm.newSequence(Tag.ReferencedSeriesSequence, 1).add(mid);

			assertTrue(DicomObjectTools
				.containsTagFromPath("ReferencedSeriesSequence.ReferencedImageSequence.ReferencedSOPInstanceUID", dcm));
		}

		@Test
		void returns_false_when_an_intermediate_sequence_is_missing() {
			assertFalse(
					DicomObjectTools.containsTagFromPath("ReferencedImageSequence.ReferencedSOPClassUID", patient()));
		}

	}

	@Nested
	class DeepContains {

		@Test
		void finds_a_top_level_tag() {
			assertTrue(DicomObjectTools.containsTagInAllAttributes(Tag.PatientName, patient()));
		}

		@Test
		void finds_a_tag_buried_in_a_sequence() {
			Attributes item = new Attributes();
			item.setString(Tag.ReferencedSOPClassUID, VR.UI, "1.2.3");
			Attributes dcm = new Attributes();
			dcm.newSequence(Tag.ReferencedImageSequence, 1).add(item);

			assertTrue(DicomObjectTools.containsTagInAllAttributes(Tag.ReferencedSOPClassUID, dcm));
		}

		@Test
		void returns_false_for_an_absent_tag() {
			assertFalse(DicomObjectTools.containsTagInAllAttributes(Tag.StudyDate, patient()));
		}

	}

	@Nested
	class ScopedLookup {

		private static Attributes study() {
			Attributes dcm = patient();
			dcm.setString(Tag.Modality, VR.CS, "CT");
			return dcm;
		}

		private static Attributes itemOf(Attributes dcm) {
			Attributes item = new Attributes();
			item.setString(Tag.ScheduledProcedureStepStartDate, VR.DA, "20200110");
			dcm.newSequence(Tag.RequestAttributesSequence, 1).add(item);
			return item;
		}

		@Test
		void finds_a_tag_of_the_dataset_itself() {
			assertEquals("CT", DicomObjectTools.getStringInScope(study(), Tag.Modality));
		}

		@Test
		void finds_a_tag_of_the_enclosing_dataset_from_a_sequence_item() {
			Attributes item = itemOf(study());

			assertEquals("CT", DicomObjectTools.getStringInScope(item, Tag.Modality));
		}

		@Test
		void finds_a_tag_of_the_root_from_a_deeply_nested_item() {
			Attributes dcm = study();
			Attributes inner = new Attributes();
			itemOf(dcm).newSequence(Tag.ReferencedImageSequence, 1).add(inner);

			assertEquals("CT", DicomObjectTools.getStringInScope(inner, Tag.Modality));
		}

		@Test
		void the_nearest_scope_wins() {
			Attributes dcm = study();
			Attributes item = itemOf(dcm);
			item.setString(Tag.Modality, VR.CS, "MR");

			assertEquals("MR", DicomObjectTools.getStringInScope(item, Tag.Modality));
			assertEquals("CT", DicomObjectTools.getStringInScope(dcm, Tag.Modality));
		}

		@Test
		void does_not_look_into_the_sequences_of_the_dataset() {
			Attributes dcm = study();
			itemOf(dcm);

			assertNull(DicomObjectTools.getStringInScope(dcm, Tag.ScheduledProcedureStepStartDate));
		}

		@Test
		void returns_null_for_a_tag_no_enclosing_dataset_holds() {
			assertNull(DicomObjectTools.getStringInScope(itemOf(study()), Tag.StudyDate));
		}

	}

}