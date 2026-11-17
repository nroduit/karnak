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

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A single conformance finding. Record equality is the natural deduplication key when the
 * same issue repeats across the instances of a study.
 *
 * @param tagPath display form of the attribute location, e.g. {@code (0010,0010)} or
 * {@code (0040,0275) > (0008,0050)} for an attribute inside a sequence item
 * @param attributeName attribute (or module) name from the DICOM standard
 * @param moduleId identifier of the IOD module(s) defining the attribute, comma-separated
 * when several modules define it, may be null
 * @param expected the expected Type/VR/VM/value as text
 * @param found a short summary of what was actually found
 */
public record ConformanceFinding(String tagPath, String attributeName, String moduleId, Severity severity,
		CheckKind kind, String expected, String found) {

	/** Module id words displayed in upper case. */
	private static final Set<String> ACRONYMS = Set.of("2d", "3d", "cr", "ct", "dvh", "dx", "icc", "ivus", "lut", "mpr",
			"mr", "nm", "oct", "pet", "roi", "rt", "sc", "sop", "sr", "us", "uv", "vl", "voi", "xa", "xrf");

	/** Module id words kept in lower case, except as first word. */
	private static final Set<String> MINOR_WORDS = Set.of("and", "for", "in", "of", "on", "or", "the", "to");

	/**
	 * @return the readable name of the module(s), e.g. {@code General Image} for
	 * {@code general-image}, or null when no module is known
	 */
	public String moduleName() {
		return moduleId == null ? null : toModuleName(moduleId);
	}

	/**
	 * @param moduleIds module id(s) such as {@code ct-image} or
	 * {@code general-image, ct-image}
	 * @return the readable module name(s), e.g. {@code CT Image}
	 */
	public static String toModuleName(String moduleIds) {
		return Arrays.stream(moduleIds.split(",\\s*"))
			.map(ConformanceFinding::toSingleModuleName)
			.collect(Collectors.joining(", "));
	}

	private static String toSingleModuleName(String moduleId) {
		String id = moduleId.replace("x-ray", "x_ray").replace("multi-frame", "multi_frame");
		String[] words = id.split("-");
		StringBuilder name = new StringBuilder();
		for (String word : words) {
			if (word.isEmpty()) {
				continue;
			}
			if (!name.isEmpty()) {
				name.append(' ');
			}
			if (ACRONYMS.contains(word)) {
				name.append(word.toUpperCase(Locale.ROOT));
			}
			else if (!name.isEmpty() && MINOR_WORDS.contains(word)) {
				name.append(word);
			}
			else {
				name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
			}
		}
		return name.toString().replace("_ray", "-Ray").replace("_frame", "-frame");
	}

}
