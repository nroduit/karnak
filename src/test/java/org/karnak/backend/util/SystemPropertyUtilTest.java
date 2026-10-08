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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class SystemPropertyUtilTest {

	private static final String KEY = "karnak.test.systemproperty";

	private static final String INT_KEY = "karnak.test.systemproperty.int";

	@AfterEach
	void clearProperties() {
		System.clearProperty(KEY);
		System.clearProperty(INT_KEY);
	}

	@Test
	void retrieves_a_defined_system_property() {
		System.setProperty(KEY, "value");

		assertEquals("value", SystemPropertyUtil.retrieveSystemProperty(KEY, "default"));
	}

	@Test
	void falls_back_to_the_default_when_property_and_env_are_absent() {
		assertEquals("default", SystemPropertyUtil.retrieveSystemProperty(KEY, "default"));
	}

	@Test
	void parses_an_integer_system_property() {
		System.setProperty(INT_KEY, "123");

		assertEquals(123, SystemPropertyUtil.retrieveIntegerSystemProperty(INT_KEY, 0));
	}

	@Test
	void returns_the_default_integer_when_property_is_absent() {
		assertEquals(7, SystemPropertyUtil.retrieveIntegerSystemProperty(INT_KEY, 7));
	}

	@Test
	void returns_the_default_integer_when_value_is_not_a_number() {
		System.setProperty(INT_KEY, "not-a-number");

		assertEquals(9, SystemPropertyUtil.retrieveIntegerSystemProperty(INT_KEY, 9));
	}

}