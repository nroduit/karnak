/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullUnmarked;

/**
 * Parsed DICOM Value Multiplicity specification, e.g. {@code 1}, {@code 1-n},
 * {@code 2-2n}, {@code 1-2}.
 *
 * @param min minimum number of values
 * @param max maximum number of values, {@link Integer#MAX_VALUE} when unbounded
 * @param factor the value count must be a multiple of this factor (e.g. 2 for
 * {@code 2-2n}), 1 when unconstrained
 */
@NullUnmarked
public record VmSpec(int min, int max, int factor) {

	// "1", "16", "1-2", "1-99", "1-n", "2-2n", "3-3n"
	private static final Pattern VM_PATTERN = Pattern.compile("^(\\d+)(?:-(?:(\\d+)|(\\d*)n))?$");

	/**
	 * Parses a VM string from the DICOM data dictionary.
	 * @return the parsed specification, or null when the string is not a valid VM form
	 */
	public static VmSpec parse(String vm) {
		if (vm == null) {
			return null;
		}
		Matcher matcher = VM_PATTERN.matcher(vm.trim());
		if (!matcher.matches()) {
			return null;
		}
		int min = Integer.parseInt(matcher.group(1));
		String fixedMax = matcher.group(2);
		String nFactor = matcher.group(3);
		if (fixedMax != null) {
			// "1-2", "1-99"
			return new VmSpec(min, Integer.parseInt(fixedMax), 1);
		}
		if (nFactor != null) {
			// "1-n", "2-2n", "3-3n"
			int factor = nFactor.isEmpty() ? 1 : Integer.parseInt(nFactor);
			return new VmSpec(min, Integer.MAX_VALUE, factor);
		}
		// "1", "16"
		return new VmSpec(min, min, 1);
	}

	public boolean matches(int count) {
		return count >= min && count <= max && (factor <= 1 || count % factor == 0);
	}

}
