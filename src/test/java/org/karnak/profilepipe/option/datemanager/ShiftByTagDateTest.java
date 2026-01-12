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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.util.ShiftByTagDate;

public class ShiftByTagDateTest {

	private static final Attributes dataset = new Attributes();

	private static final List<ArgumentEntity> argumentEntities = new ArrayList<>();

	private static final ArgumentEntity seconds_tag = new ArgumentEntity();

	private static final ArgumentEntity days_tag = new ArgumentEntity();

	private static final HMAC hmac = new HMAC(HMAC.generateRandomKey());

	@BeforeEach
	protected void setUpBeforeTest() {
		argumentEntities.clear();

		dataset.clear();

		dataset.setString(Tag.StudyDate, VR.DA, "20180209");
		dataset.setString(Tag.StudyTime, VR.TM, "120843");
		dataset.setString(Tag.PatientAge, VR.AS, "043Y");
		dataset.setString(Tag.AcquisitionDateTime, VR.DT, "20180209120854.354");
		dataset.setString(Tag.AcquisitionTime, VR.TM, "010134");
		dataset.setString(0x00150010, VR.LT, "ADIS");
		dataset.setString(0x00151010, VR.LT, "10");
		dataset.setString(0x00151011, VR.LT, "500");
		dataset.setString(0x00151012, VR.LT, "AAA");
		dataset.setString(0x00151013, VR.LT, "BBB");
	}

	@Test
	void shiftNoop() {
		assertEquals("20180209", ShiftByTagDate.shift(dataset, Tag.StudyDate, argumentEntities, hmac));
		assertEquals("120843.000000", ShiftByTagDate.shift(dataset, Tag.StudyTime, argumentEntities, hmac));
		assertEquals("043Y", ShiftByTagDate.shift(dataset, Tag.PatientAge, argumentEntities, hmac));
		assertEquals("20180209120854.354000",
				ShiftByTagDate.shift(dataset, Tag.AcquisitionDateTime, argumentEntities, hmac));
		assertEquals("010134.000000", ShiftByTagDate.shift(dataset, Tag.AcquisitionTime, argumentEntities, hmac));
	}

	@Test
	void shiftByTag() {
		days_tag.setArgumentKey("days_tag");
		days_tag.setArgumentValue("(0015,1010)");
		seconds_tag.setArgumentKey("seconds_tag");
		seconds_tag.setArgumentValue("(0015,1011)");
		argumentEntities.add(seconds_tag);
		argumentEntities.add(days_tag);

		assertEquals("20180130", ShiftByTagDate.shift(dataset, Tag.StudyDate, argumentEntities, hmac));
		assertEquals("120023.000000", ShiftByTagDate.shift(dataset, Tag.StudyTime, argumentEntities, hmac));
		assertEquals("043Y", ShiftByTagDate.shift(dataset, Tag.PatientAge, argumentEntities, hmac));
		assertEquals("20180130120034.354000",
				ShiftByTagDate.shift(dataset, Tag.AcquisitionDateTime, argumentEntities, hmac));
		assertEquals("005314.000000", ShiftByTagDate.shift(dataset, Tag.AcquisitionTime, argumentEntities, hmac));
	}

	@Test
	void shiftByBadTag() {
		days_tag.setArgumentKey("days_tag");
		days_tag.setArgumentValue("(0017,1010)");
		seconds_tag.setArgumentKey("seconds_tag");
		seconds_tag.setArgumentValue("(0017,1011)");
		argumentEntities.add(seconds_tag);
		argumentEntities.add(days_tag);

		assertEquals("20180209", ShiftByTagDate.shift(dataset, Tag.StudyDate, argumentEntities, hmac));
		assertEquals("120843.000000", ShiftByTagDate.shift(dataset, Tag.StudyTime, argumentEntities, hmac));
		assertEquals("043Y", ShiftByTagDate.shift(dataset, Tag.PatientAge, argumentEntities, hmac));
		assertEquals("20180209120854.354000",
				ShiftByTagDate.shift(dataset, Tag.AcquisitionDateTime, argumentEntities, hmac));
		assertEquals("010134.000000", ShiftByTagDate.shift(dataset, Tag.AcquisitionTime, argumentEntities, hmac));
	}

	@Test
	void shiftByBadTag2() {
		days_tag.setArgumentKey("days_tag");
		days_tag.setArgumentValue("(0015,1012)");
		seconds_tag.setArgumentKey("seconds_tag");
		seconds_tag.setArgumentValue("(0015,1013)");
		argumentEntities.add(seconds_tag);
		argumentEntities.add(days_tag);

		assertEquals("20180209", ShiftByTagDate.shift(dataset, Tag.StudyDate, argumentEntities, hmac));
		assertEquals("120843.000000", ShiftByTagDate.shift(dataset, Tag.StudyTime, argumentEntities, hmac));
		assertEquals("043Y", ShiftByTagDate.shift(dataset, Tag.PatientAge, argumentEntities, hmac));
		assertEquals("20180209120854.354000",
				ShiftByTagDate.shift(dataset, Tag.AcquisitionDateTime, argumentEntities, hmac));
		assertEquals("010134.000000", ShiftByTagDate.shift(dataset, Tag.AcquisitionTime, argumentEntities, hmac));
	}

}
