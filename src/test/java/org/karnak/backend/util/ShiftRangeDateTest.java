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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.HashContext;

/**
 * {@link ShiftRangeDate} derives a deterministic shift inside a configured range from the
 * patient id via the {@link HMAC}; the same patient always yields the same shifted date.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class ShiftRangeDateTest {

	private static HMAC hmac() {
		return new HMAC(new HashContext(HMAC.hexToByte("0123456789abcdef0123456789abcdef"), "PATIENT-1"));
	}

	private static List<ArgumentEntity> rangeArgs() {
		return List.of(new ArgumentEntity("max_days", "30"), new ArgumentEntity("max_seconds", "3600"));
	}

	@Test
	void shifts_a_date_deterministically_within_the_range() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.StudyDate, VR.DA, "20200615");

		String first = ShiftRangeDate.shift(dcm, Tag.StudyDate, rangeArgs(), hmac());
		String second = ShiftRangeDate.shift(dcm, Tag.StudyDate, rangeArgs(), hmac());

		assertEquals(first, second);
		assertTrue(first.matches("\\d{8}"), first);
	}

	@Test
	void verify_shift_arguments_rejects_a_missing_max_argument() {
		List<ArgumentEntity> args = List.of(new ArgumentEntity("max_days", "30"));

		assertThrows(IllegalArgumentException.class, () -> ShiftRangeDate.verifyShiftArguments(args));
	}

}