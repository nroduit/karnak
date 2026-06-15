/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.profilepipe.option.datemanager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.DatePrecision;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.util.DateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.util.ShiftDate;

class ShiftDateTest {

	private static final Attributes dataset = new Attributes();

	private static final List<ArgumentEntity> argumentEntities = new ArrayList<>();

	private static final ArgumentEntity seconds = new ArgumentEntity();

	private static final ArgumentEntity days = new ArgumentEntity();

	@BeforeAll
	protected static void setUpBeforeClass() throws Exception {
		seconds.setArgumentKey("seconds");
		seconds.setArgumentValue("500");
		days.setArgumentKey("days");
		days.setArgumentValue("40");

		argumentEntities.add(seconds);
		argumentEntities.add(days);

		dataset.setString(Tag.StudyDate, VR.DA, "20180209");
		dataset.setString(Tag.StudyTime, VR.TM, "120843");
		dataset.setString(Tag.PatientAge, VR.AS, "043Y");
		dataset.setString(Tag.AcquisitionDateTime, VR.DT, "20180209120854.354");
		dataset.setString(Tag.AcquisitionTime, VR.TM, "000134");
	}

	@Test
	void DAbyDays() {
		assertEquals("19930822", ShiftDate.dateByDays("19930823", 1));
		assertEquals("20391231", ShiftDate.dateByDays("20400120", 20));
		assertEquals("19920102", ShiftDate.dateByDays("19930101", 365));
		assertEquals("19940101", ShiftDate.dateByDays("19930101", -365));

		Assertions.assertThrows(DateTimeParseException.class, () -> {
			ShiftDate.dateByDays("199", 365);
		});

		Assertions.assertThrows(DateTimeParseException.class, () -> {
			ShiftDate.dateByDays("19932", 365);
		});

		Assertions.assertThrows(DateTimeParseException.class, () -> {
			ShiftDate.dateByDays("199320", 365);
		});

		Assertions.assertThrows(DateTimeParseException.class, () -> {
			ShiftDate.dateByDays("1993021", 365);
		});

		Assertions.assertThrows(DateTimeParseException.class, () -> {
			ShiftDate.dateByDays("19930254", 365);
		});
	}

	// @Test
	void TMbySeconds() {
		assertEquals("070906.070500", ShiftDate.timeBySeconds("070907.0705", 1));
		assertEquals("100959.000000", ShiftDate.timeBySeconds("1010", 1));
		assertEquals("100900.000000", ShiftDate.timeBySeconds("1010", 60));
		assertEquals("091000.000000", ShiftDate.timeBySeconds("1010", 60 * 60));
		assertEquals("101000.000000", ShiftDate.timeBySeconds("1010", 24 * 60 * 60));

		Assertions.assertThrows(DateTimeParseException.class, () -> {
			ShiftDate.timeBySeconds("1", 15);
		});
		Assertions.assertThrows(DateTimeParseException.class, () -> {
			ShiftDate.timeBySeconds("35", 15);
		});
		Assertions.assertThrows(DateTimeParseException.class, () -> {
			ShiftDate.timeBySeconds("125", 15);
		});
		Assertions.assertThrows(DateTimeParseException.class, () -> {
			ShiftDate.timeBySeconds("1270", 15);
		});
		Assertions.assertThrows(DateTimeParseException.class, () -> {
			ShiftDate.timeBySeconds("12598", 15);
		});
		Assertions.assertThrows(DateTimeParseException.class, () -> {
			ShiftDate.timeBySeconds("125980", 15);
		});
	}

	@Test
	void ASbyDays() {
		assertEquals("019M", ShiftDate.ageByDays("018M", 40));
		assertEquals("009M", ShiftDate.ageByDays("008M", 40));
		assertEquals("009M", ShiftDate.ageByDays("009M", 20));
		assertEquals("002Y", ShiftDate.ageByDays("001Y", 365));
		assertEquals("031Y", ShiftDate.ageByDays("029Y", 730));
		assertEquals("009D", ShiftDate.ageByDays("008D", 1));
	}

	// @Test
	void DTbyDays() {
		DatePrecision prec = new DatePrecision();
		assertEquals("20180228235900.000000",
				ShiftDate.datetimeByDays(DateUtils.parseDT(TimeZone.getDefault(), "20180302", prec), 1, 60));
		assertEquals("20080728131403.000000",
				ShiftDate.datetimeByDays(DateUtils.parseDT(TimeZone.getDefault(), "20080729131503", prec), 1, 60));
		assertEquals("20201210235930.000000",
				ShiftDate.datetimeByDays(DateUtils.parseDT(TimeZone.getDefault(), "20201212000030", prec), 1, 60));
		assertEquals("20201211000030.000000",
				ShiftDate.datetimeByDays(DateUtils.parseDT(TimeZone.getDefault(), "20201212000130", prec), 1, 60));
		assertEquals("20080721235900.000000",
				ShiftDate.datetimeByDays(DateUtils.parseDT(TimeZone.getDefault(), "2008072824", prec), 7, 60));

		Assertions.assertThrows(DateTimeParseException.class, () -> {
			ShiftDate.datetimeByDays(DateUtils.parseDT(TimeZone.getDefault(), "200807281", prec), 365, 60);
		});
		Assertions.assertThrows(DateTimeParseException.class, () -> {
			ShiftDate.datetimeByDays(DateUtils.parseDT(TimeZone.getDefault(), "2008072825", prec), 365, 60);
		});
		Assertions.assertThrows(DateTimeParseException.class, () -> {
			ShiftDate.datetimeByDays(DateUtils.parseDT(TimeZone.getDefault(), "20080728121", prec), 365, 60);
		});
		Assertions.assertThrows(DateTimeParseException.class, () -> {
			ShiftDate.datetimeByDays(DateUtils.parseDT(TimeZone.getDefault(), "200807281261", prec), 365, 60);
		});
		Assertions.assertThrows(DateTimeParseException.class, () -> {
			ShiftDate.datetimeByDays(DateUtils.parseDT(TimeZone.getDefault(), "2008072812592", prec), 365, 60);
		});
		Assertions.assertThrows(DateTimeParseException.class, () -> {
			ShiftDate.datetimeByDays(DateUtils.parseDT(TimeZone.getDefault(), "20080728125989", prec), 365, 60);
		});
	}

	// @Test
	void shift() {

		assertEquals("20171231", ShiftDate.shift(dataset, Tag.StudyDate, argumentEntities));
		assertEquals("120023.000000", ShiftDate.shift(dataset, Tag.StudyTime, argumentEntities));
		assertEquals("043Y", ShiftDate.shift(dataset, Tag.PatientAge, argumentEntities));
		assertEquals("20171231120034.354000", ShiftDate.shift(dataset, Tag.AcquisitionDateTime, argumentEntities));
		assertEquals("235314.000000", ShiftDate.shift(dataset, Tag.AcquisitionTime, argumentEntities));

		seconds.setArgumentKey("notseconds");
		days.setArgumentKey("days");
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ShiftDate.shift(dataset, Tag.AcquisitionTime, argumentEntities);
		});

		seconds.setArgumentKey("seconds");
		days.setArgumentKey("notdays");
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ShiftDate.shift(dataset, Tag.AcquisitionTime, argumentEntities);
		});

		seconds.setArgumentKey("notseconds");
		days.setArgumentKey("notdays");
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ShiftDate.shift(dataset, Tag.AcquisitionTime, argumentEntities);
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			ShiftDate.shift(dataset, Tag.AcquisitionTime, new ArrayList<>());
		});
	}

}
