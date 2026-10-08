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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ShiftDateTest {

	private static List<ArgumentEntity> shiftArgs(String days, String seconds) {
		return List.of(new ArgumentEntity("days", days), new ArgumentEntity("seconds", seconds));
	}

	@Test
	void date_by_days_shifts_backwards() {
		assertEquals("20200105", ShiftDate.dateByDays("20200110", 5));
	}

	@Test
	void time_by_seconds_shifts_backwards() {
		assertEquals("115900.000000", ShiftDate.timeBySeconds("120000", 60));
	}

	@Test
	void age_by_days_shifts_years() {
		assertEquals("031Y", ShiftDate.ageByDays("030Y", 365));
	}

	@Test
	void age_by_days_shifts_months() {
		assertEquals("011M", ShiftDate.ageByDays("010M", 30));
	}

	@Test
	void age_by_days_shifts_weeks() {
		assertEquals("006W", ShiftDate.ageByDays("005W", 7));
	}

	@Test
	void age_by_days_shifts_days_for_an_unknown_unit() {
		assertEquals("105D", ShiftDate.ageByDays("100D", 5));
	}

	@Test
	void shift_value_dispatches_on_the_date_vr() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.StudyDate, VR.DA, "20200110");

		assertEquals("20200105", ShiftDate.shiftValue(dcm, Tag.StudyDate, "20200110", 5, 0));
	}

	@Test
	void shift_value_returns_null_for_a_null_input() {
		Attributes dcm = new Attributes();

		assertNull(ShiftDate.shiftValue(dcm, Tag.StudyDate, null, 5, 0));
	}

	@Test
	void shift_value_returns_null_for_an_unsupported_vr() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.PatientName, VR.PN, "Doe^John");

		assertNull(ShiftDate.shiftValue(dcm, Tag.PatientName, "Doe^John", 5, 0));
	}

	@Test
	void shift_value_handles_a_datetime_tag() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.AcquisitionDateTime, VR.DT, "20200615120000");

		assertNotNull(ShiftDate.shiftValue(dcm, Tag.AcquisitionDateTime, "20200615120000", 5, 30));
	}

	@Test
	void shift_reads_the_day_and_second_arguments() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.StudyDate, VR.DA, "20200110");

		assertEquals("20200105", ShiftDate.shift(dcm, Tag.StudyDate, shiftArgs("5", "0")));
	}

	@Test
	void verify_shift_arguments_rejects_a_missing_argument() {
		List<ArgumentEntity> args = List.of(new ArgumentEntity("days", "5"));

		assertThrows(IllegalArgumentException.class, () -> ShiftDate.verifyShiftArguments(args));
	}

}