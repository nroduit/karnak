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

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.util.DateFormat;

class DatePatternTest {

	@Test
	void formatDA() {
		assertEquals("20080801", DateFormat.formatDA("20080822", "day"));
		assertEquals("20080101", DateFormat.formatDA("20080822", "month_day"));
	}

	@Test
	void formatDT() {
		assertEquals("20080801131503.000000", DateFormat.formatDT("20080822131503", "day"));
		assertEquals("20080101131503.000000", DateFormat.formatDT("20080822131503", "month_day"));
	}

	@Test
	void verifyPatternArguments() {
		List<ArgumentEntity> argsFalse = new ArrayList<>();
		argsFalse.add(new ArgumentEntity("second", "day", null));

		List<ArgumentEntity> argsFalse2 = new ArrayList<>();
		argsFalse.add(new ArgumentEntity("remove", "daytt", null));

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DateFormat.verifyPatternArguments(argsFalse);
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			DateFormat.verifyPatternArguments(argsFalse2);
		});
	}

}
