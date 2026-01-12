/*
 * Copyright (c) 2025 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.util;

import java.text.Collator;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;
import org.weasis.core.util.StringUtil;

public final class CollatorUtils {

	private CollatorUtils() {
	}

	private static Collator collator() {
		return StringUtil.collator;
	}

	public static String nullSafe(String s) {
		return Optional.ofNullable(s).orElse("");
	}

	public static int compare(String a, String b) {
		return collator().compare(nullSafe(a), nullSafe(b));
	}

	public static Comparator<String> stringComparator() {
		return CollatorUtils::compare;
	}

	public static <T> Comparator<T> comparing(Function<T, String> keyExtractor) {
		return (o1, o2) -> compare(keyExtractor.apply(o1), keyExtractor.apply(o2));
	}

	public static <T> Comparator<T> comparingThen(Function<T, String> primary, Function<T, String> secondary) {
		return (o1, o2) -> {
			int c = compare(primary.apply(o1), primary.apply(o2));
			return c != 0 ? c : compare(secondary.apply(o1), secondary.apply(o2));
		};
	}

}
