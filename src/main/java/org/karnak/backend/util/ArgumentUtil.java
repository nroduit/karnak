/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.ArgumentEntity;

/** Helpers to read values out of a profile item's {@link ArgumentEntity} list. */
@Slf4j
@NullUnmarked
public final class ArgumentUtil {

	private ArgumentUtil() {
	}

	/**
	 * Returns the value of the first argument matching the key, or the default value.
	 * @param arguments arguments of the profile item
	 * @param key argument key to look up
	 * @param defaultValue value returned when the key is absent
	 * @return the matching argument value or the default
	 */
	public static String stringValue(List<ArgumentEntity> arguments, String key, String defaultValue) {
		return arguments.stream()
			.filter(argument -> key.equals(argument.getArgumentKey()))
			.map(ArgumentEntity::getArgumentValue)
			.findFirst()
			.orElse(defaultValue);
	}

	/**
	 * Parses the value of the first argument matching the key as an int.
	 * @param arguments arguments of the profile item
	 * @param key argument key to look up
	 * @param defaultValue value returned when the key is absent or not a number
	 * @return the parsed int or the default
	 */
	public static int intValue(List<ArgumentEntity> arguments, String key, int defaultValue) {
		return parseInt(stringValue(arguments, key, null), defaultValue);
	}

	/**
	 * Parses a string as an int, logging and falling back to the default on failure.
	 * @param value value to parse (may be {@code null})
	 * @param defaultValue value returned when {@code value} is {@code null} or not a
	 * number
	 * @return the parsed int or the default
	 */
	public static int parseInt(String value, int defaultValue) {
		if (value == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		}
		catch (NumberFormatException e) {
			log.error("args {} is not correct", value, e);
			return defaultValue;
		}
	}

}
