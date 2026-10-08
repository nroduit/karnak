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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DateFormatTest {

	@Test
	void formats_a_local_date_with_a_pattern() {
		assertEquals("15/06/2020", DateFormat.format(LocalDate.of(2020, 6, 15), DateFormat.FORMAT_DDMMYYYY_SLASH));
	}

	@Test
	void formats_a_null_local_date_as_null() {
		assertNull(DateFormat.format((LocalDate) null, DateFormat.FORMAT_DDMMYYYY_SLASH));
	}

	@Test
	void formats_a_local_date_time_with_a_pattern() {
		assertEquals("15/06/2020 13:30:45", DateFormat.format(LocalDateTime.of(2020, 6, 15, 13, 30, 45),
				DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS));
	}

	@Test
	void formats_a_null_local_date_time_as_null() {
		assertNull(DateFormat.format((LocalDateTime) null, DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS));
	}

	@Test
	void format_da_truncates_to_the_first_of_the_month_for_the_day_option() {
		assertEquals("20200601", DateFormat.formatDA("20200615", "day"));
	}

	@Test
	void format_da_truncates_to_the_first_of_the_year_for_the_month_day_option() {
		assertEquals("20200101", DateFormat.formatDA("20200615", "month_day"));
	}

	@Test
	void format_da_keeps_the_date_for_an_unknown_option() {
		assertEquals("20200615", DateFormat.formatDA("20200615", "other"));
	}

	@Test
	void format_dt_truncates_to_the_first_of_the_month_for_the_day_option() {
		assertTrue(DateFormat.formatDT("20200615120000", "day").startsWith("20200601120000"));
	}

	@Test
	void format_from_attributes_applies_the_day_option_on_a_da_tag() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.StudyDate, VR.DA, "20200615");
		List<ArgumentEntity> args = List.of(new ArgumentEntity("remove", "day"));

		assertEquals("20200601", DateFormat.format(dcm, Tag.StudyDate, args));
	}

	@Test
	void format_from_attributes_returns_null_when_the_tag_is_absent() {
		Attributes dcm = new Attributes();
		List<ArgumentEntity> args = List.of(new ArgumentEntity("remove", "day"));

		assertNull(DateFormat.format(dcm, Tag.StudyDate, args));
	}

	@Test
	void format_from_attributes_returns_null_for_an_unsupported_vr() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.PatientName, VR.PN, "Doe^John");
		List<ArgumentEntity> args = List.of(new ArgumentEntity("remove", "day"));

		assertNull(DateFormat.format(dcm, Tag.PatientName, args));
	}

	@Test
	void verify_pattern_arguments_rejects_a_missing_remove_argument() {
		List<ArgumentEntity> args = List.of(new ArgumentEntity("other", "day"));

		assertThrows(IllegalArgumentException.class, () -> DateFormat.verifyPatternArguments(args));
	}

	@Test
	void verify_pattern_arguments_rejects_an_unsupported_remove_value() {
		List<ArgumentEntity> args = List.of(new ArgumentEntity("remove", "year"));

		assertThrows(IllegalArgumentException.class, () -> DateFormat.verifyPatternArguments(args));
	}

}