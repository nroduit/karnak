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

import java.util.regex.Pattern;

public class SpecialCharacter {

	private SpecialCharacter() {
	}

	// Response Ferran Maylinch
	// https://stackoverflow.com/questions/10664434/escaping-special-characters-in-java-regular-expressions
	public static String escapeSpecialRegexChars(String str) {
		Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");
		return SPECIAL_REGEX_CHARS.matcher(str).replaceAll("\\\\$0");
	}

}
