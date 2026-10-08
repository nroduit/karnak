/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.component.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DecimalFormat;
import java.util.Locale;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class HStringToIntegerConverterTest {

	private final HStringToIntegerConverter converter = new HStringToIntegerConverter();

	@Test
	void format_has_no_grouping_separator() {
		var format = (DecimalFormat) converter.getFormat(Locale.US);
		assertFalse(format.isGroupingUsed());
	}

	@Test
	void format_parses_integer_only() {
		var format = (DecimalFormat) converter.getFormat(Locale.US);
		assertTrue(format.isParseIntegerOnly());
	}

	@Test
	void format_has_no_fraction_digits() {
		var format = (DecimalFormat) converter.getFormat(Locale.US);
		assertEquals(0, format.getMaximumFractionDigits());
	}

	@Test
	void format_does_not_show_decimal_separator() {
		var format = (DecimalFormat) converter.getFormat(Locale.US);
		assertFalse(format.isDecimalSeparatorAlwaysShown());
	}

	@Test
	void format_produces_no_grouping_for_large_numbers() {
		var format = converter.getFormat(Locale.US);
		assertEquals("1234567", format.format(1234567));
	}

	@Test
	void format_is_consistent_across_locales() {
		var usFormat = (DecimalFormat) converter.getFormat(Locale.US);
		var frFormat = (DecimalFormat) converter.getFormat(Locale.FRANCE);
		assertEquals(usFormat.isGroupingUsed(), frFormat.isGroupingUsed());
		assertEquals(usFormat.getMaximumFractionDigits(), frFormat.getMaximumFractionDigits());
	}

}
