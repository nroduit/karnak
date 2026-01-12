/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.profilepipe.option.datemanager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.HashContext;
import org.karnak.backend.util.ShiftRangeDate;

class ShiftRangeDateTest {

	private static final Attributes dataset = new Attributes();

	private static final List<ArgumentEntity> argumentEntities = new ArrayList<>();

	private static final ArgumentEntity max_seconds = new ArgumentEntity();

	private static final ArgumentEntity max_days = new ArgumentEntity();

	private static final ArgumentEntity min_seconds = new ArgumentEntity();

	private static final ArgumentEntity min_days = new ArgumentEntity();

	@BeforeAll
	protected static void setUpBeforeClass() throws Exception {
		max_seconds.setArgumentKey("max_seconds");
		max_seconds.setArgumentValue("1000");
		max_days.setArgumentKey("max_days");
		max_days.setArgumentValue("200");

		argumentEntities.add(max_seconds);
		argumentEntities.add(max_days);
		dataset.setString(Tag.StudyDate, VR.DA, "20180209");
		dataset.setString(Tag.StudyTime, VR.TM, "120843");
		dataset.setString(Tag.PatientAge, VR.AS, "043Y");
		dataset.setString(Tag.AcquisitionDateTime, VR.DT, "20180209120854.354");
		dataset.setString(Tag.AcquisitionTime, VR.TM, "000134");
	}

	@Test
	void shift() {
		byte[] HMAC_KEY = { -116, -11, -20, 53, -37, -94, 64, 103, 63, -89, -108, -70, 84, 43, -74, -8 };
		String Patient_ID = "Patient 1";
		HashContext hashContext = new HashContext(HMAC_KEY, Patient_ID);
		HMAC hmac = new HMAC(hashContext);

		String Patient_ID_2 = "Patient 2";
		byte[] HMAC_KEY_2 = { -57, -80, 125, -55, 54, 85, 52, 102, 20, -116, -78, -6, 108, 47, -37, -43 };
		HashContext hashContext_2 = new HashContext(HMAC_KEY_2, Patient_ID_2);
		HMAC hmac_2 = new HMAC(hashContext_2);

		assertEquals("20171001", ShiftRangeDate.shift(dataset, Tag.StudyDate, argumentEntities, hmac));
		assertEquals("115745.000000", ShiftRangeDate.shift(dataset, Tag.StudyTime, argumentEntities, hmac));
		assertEquals("043Y", ShiftRangeDate.shift(dataset, Tag.PatientAge, argumentEntities, hmac));
		assertEquals("20171001115756.354000",
				ShiftRangeDate.shift(dataset, Tag.AcquisitionDateTime, argumentEntities, hmac));
		assertEquals("235036.000000", ShiftRangeDate.shift(dataset, Tag.AcquisitionTime, argumentEntities, hmac));
		assertEquals("20171114", ShiftRangeDate.shift(dataset, Tag.StudyDate, argumentEntities, hmac_2));
		assertEquals("120126.000000", ShiftRangeDate.shift(dataset, Tag.StudyTime, argumentEntities, hmac_2));
		assertEquals("043Y", ShiftRangeDate.shift(dataset, Tag.PatientAge, argumentEntities, hmac_2));
		assertEquals("20171114120137.354000",
				ShiftRangeDate.shift(dataset, Tag.AcquisitionDateTime, argumentEntities, hmac_2));
		assertEquals("235417.000000", ShiftRangeDate.shift(dataset, Tag.AcquisitionTime, argumentEntities, hmac_2));

		min_seconds.setArgumentKey("min_seconds");
		min_seconds.setArgumentValue("500");
		min_days.setArgumentKey("min_days");
		min_days.setArgumentValue("100");

		argumentEntities.add(min_seconds);
		argumentEntities.add(min_days);

		assertEquals("20170828", ShiftRangeDate.shift(dataset, Tag.StudyDate, argumentEntities, hmac));
		assertEquals("115454.000000", ShiftRangeDate.shift(dataset, Tag.StudyTime, argumentEntities, hmac));
		assertEquals("043Y", ShiftRangeDate.shift(dataset, Tag.PatientAge, argumentEntities, hmac));
		assertEquals("20170828115505.354000",
				ShiftRangeDate.shift(dataset, Tag.AcquisitionDateTime, argumentEntities, hmac));
		assertEquals("234745.000000", ShiftRangeDate.shift(dataset, Tag.AcquisitionTime, argumentEntities, hmac));

		assertEquals("20170919", ShiftRangeDate.shift(dataset, Tag.StudyDate, argumentEntities, hmac_2));
		assertEquals("115645.000000", ShiftRangeDate.shift(dataset, Tag.StudyTime, argumentEntities, hmac_2));
		assertEquals("043Y", ShiftRangeDate.shift(dataset, Tag.PatientAge, argumentEntities, hmac_2));
		assertEquals("20170919115656.354000",
				ShiftRangeDate.shift(dataset, Tag.AcquisitionDateTime, argumentEntities, hmac_2));
		assertEquals("234936.000000", ShiftRangeDate.shift(dataset, Tag.AcquisitionTime, argumentEntities, hmac_2));
		max_seconds.setArgumentKey("test_max_seconds");
		max_days.setArgumentKey("max_days");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> ShiftRangeDate.shift(dataset, Tag.AcquisitionTime, argumentEntities, hmac));
		max_seconds.setArgumentKey("test_max_seconds");
		max_days.setArgumentKey("max_days");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> ShiftRangeDate.shift(dataset, Tag.AcquisitionTime, argumentEntities, hmac));

		max_seconds.setArgumentKey("max_seconds");
		max_days.setArgumentKey("test_max_days");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> ShiftRangeDate.shift(dataset, Tag.AcquisitionTime, argumentEntities, hmac));

		max_seconds.setArgumentKey("test_max_seconds");
		max_days.setArgumentKey("test_max_days");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> ShiftRangeDate.shift(dataset, Tag.AcquisitionTime, argumentEntities, hmac));

		List<ArgumentEntity> arguments_2 = new ArrayList<>();
		ArgumentEntity arg_1 = new ArgumentEntity();
		arg_1.setArgumentKey("max_seconds");
		arg_1.setArgumentValue("12");
		arguments_2.add(arg_1);

		Assertions.assertThrows(IllegalArgumentException.class,
				() -> ShiftRangeDate.shift(dataset, Tag.AcquisitionTime, arguments_2, hmac));

		arg_1.setArgumentKey("max_days");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> ShiftRangeDate.shift(dataset, Tag.AcquisitionTime, arguments_2, hmac));

		arg_1.setArgumentKey("min_seconds");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> ShiftRangeDate.shift(dataset, Tag.AcquisitionTime, arguments_2, hmac));

		arg_1.setArgumentKey("min_days");
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> ShiftRangeDate.shift(dataset, Tag.AcquisitionTime, arguments_2, hmac));

		Assertions.assertThrows(IllegalArgumentException.class,
				() -> ShiftRangeDate.shift(dataset, Tag.AcquisitionTime, new ArrayList<>(), hmac));
	}

}
