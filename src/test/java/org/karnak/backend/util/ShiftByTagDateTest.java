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

import java.util.List;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;

/**
 * {@link ShiftByTagDate} reads the shift amounts from other tags of the same study, named
 * by the {@code days_tag} / {@code seconds_tag} arguments.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class ShiftByTagDateTest {

	// SeriesNumber (0020,0011) holds the days, AcquisitionNumber (0020,0012) the seconds
	private static final List<ArgumentEntity> ARGUMENTS = List.of(new ArgumentEntity("days_tag", "00200011"),
			new ArgumentEntity("seconds_tag", "00200012"));

	private static Attributes dataset(String studyDate, String days, String seconds) {
		var dcm = new Attributes();
		dcm.setString(Tag.StudyDate, VR.DA, studyDate);
		if (days != null) {
			dcm.setString(Tag.SeriesNumber, VR.IS, days);
		}
		if (seconds != null) {
			dcm.setString(Tag.AcquisitionNumber, VR.IS, seconds);
		}
		return dcm;
	}

	@Test
	void shifts_a_date_by_the_value_held_in_another_tag() {
		var dcm = dataset("20200110", "5", "0");

		assertEquals("20200105", ShiftByTagDate.shift(dcm, dcm, Tag.StudyDate, ARGUMENTS));
	}

	@Test
	void shift_tags_holding_zero_leave_the_date_unchanged() {
		var dcm = dataset("20200110", "0", "0");

		assertEquals("20200110", ShiftByTagDate.shift(dcm, dcm, Tag.StudyDate, ARGUMENTS));
	}

	@Test
	void unconfigured_shift_tags_leave_the_date_unchanged() {
		var dcm = dataset("20200110", null, null);

		assertEquals("20200110", ShiftByTagDate.shift(dcm, dcm, Tag.StudyDate, List.of()));
	}

	@Test
	void returns_null_when_a_configured_shift_tag_is_absent() {
		var dcm = dataset("20200110", null, "0");

		assertNull(ShiftByTagDate.shift(dcm, dcm, Tag.StudyDate, ARGUMENTS));
	}

	@Test
	void returns_null_when_a_configured_shift_tag_is_not_a_number() {
		var dcm = dataset("20200110", "ANONYMIZED", "0");

		assertNull(ShiftByTagDate.shift(dcm, dcm, Tag.StudyDate, ARGUMENTS));
	}

	@Test
	void reads_the_shift_tags_from_the_original_copy_not_from_the_de_identified_dataset() {
		// The shift tags sort before StudyDate is reached, so the pipeline may already
		// have replaced them in the working dataset
		var context = dataset("20200110", "5", "0");
		var dcm = dataset("20200110", "99", "0");

		assertEquals("20200105", ShiftByTagDate.shift(dcm, context, Tag.StudyDate, ARGUMENTS));
	}

	@Test
	void shifts_a_date_nested_in_a_sequence_using_the_top_level_shift_tags() {
		var context = new Attributes();
		context.setString(Tag.SeriesNumber, VR.IS, "5");
		context.setString(Tag.AcquisitionNumber, VR.IS, "0");
		Sequence sequence = context.newSequence(Tag.RequestAttributesSequence, 1);
		var item = new Attributes();
		item.setString(Tag.ScheduledProcedureStepStartDate, VR.DA, "20200110");
		sequence.add(item);

		// During sequence recursion the working dataset is the item, while the copy stays
		// the top-level dataset holding the shift tags
		assertEquals("20200105", ShiftByTagDate.shift(item, context, Tag.ScheduledProcedureStepStartDate, ARGUMENTS));
	}

	@Test
	void falls_back_to_the_working_dataset_when_the_shift_tag_is_nested_beside_the_date() {
		var item = new Attributes();
		item.setString(Tag.ScheduledProcedureStepStartDate, VR.DA, "20200110");
		item.setString(Tag.SeriesNumber, VR.IS, "5");
		item.setString(Tag.AcquisitionNumber, VR.IS, "0");

		assertEquals("20200105",
				ShiftByTagDate.shift(item, new Attributes(), Tag.ScheduledProcedureStepStartDate, ARGUMENTS));
	}

	@Test
	void shifts_a_time_by_the_seconds_held_in_another_tag() {
		var dcm = new Attributes();
		dcm.setString(Tag.StudyTime, VR.TM, "131503.000000");
		dcm.setString(Tag.SeriesNumber, VR.IS, "0");
		dcm.setString(Tag.AcquisitionNumber, VR.IS, "60");

		assertEquals("131403.000000", ShiftByTagDate.shift(dcm, dcm, Tag.StudyTime, ARGUMENTS));
	}

}