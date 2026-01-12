/*
 * Copyright (c) 2022 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import org.weasis.core.util.StringUtil;

public class SystemPropertyUtil {

	private SystemPropertyUtil() {
	}

	/**
	 * Retrieve system property
	 * @param key Key
	 * @param defaultValue default value
	 * @return property found
	 */
	public static String retrieveSystemProperty(String key, String defaultValue) {
		String val = System.getProperty(key);
		if (!StringUtil.hasText(val)) {
			val = System.getenv(key);
			if (!StringUtil.hasText(val)) {
				return defaultValue;
			}
		}
		return val;
	}

	/**
	 * Retrieve integer system property
	 * @param key Key
	 * @param defaultValue default value
	 * @return property found
	 */
	public static Integer retrieveIntegerSystemProperty(String key, Integer defaultValue) {
		String val = System.getProperty(key);
		if (!StringUtil.hasText(val)) {
			val = System.getenv(key);
			if (!StringUtil.hasText(val)) {
				return defaultValue;
			}
		}
		Integer intValue = StringUtil.getInteger(val);
		return intValue != null ? intValue : defaultValue;
	}

}
