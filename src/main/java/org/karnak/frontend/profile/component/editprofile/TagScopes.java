/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.profile.component.editprofile;

import java.util.ArrayList;
import java.util.List;
import org.dcm4che3.util.TagUtils;

/**
 * Turns a tag picked from the DICOM dictionary into the value a profile element stores,
 * which may be a plain tag or a tag path constraining where the tag sits.
 *
 * @see org.karnak.backend.model.profilepipe.TagPathPattern
 */
final class TagScopes {

	/**
	 * How a picked tag is scoped: applying wherever it appears, at the top-level dataset
	 * only, or only inside the sequences it was browsed in.
	 */
	enum TagScope {

		ANY_LEVEL("Any level"), ROOT_ONLY("Root only"), IN_SEQUENCE("Inside its sequence");

		private final String label;

		TagScope(String label) {
			this.label = label;
		}

		@Override
		public String toString() {
			return label;
		}

	}

	private TagScopes() {
	}

	/**
	 * The sequences enclosing a module attribute, in {@code (gggg,eeee)} form and
	 * outermost first.
	 *
	 * <p>
	 * The standard gives the location of an attribute inside a module as a
	 * colon-separated path of hexadecimal tags whose last segment is the attribute
	 * itself. A segment that cannot be read as a tag drops the whole hierarchy, so that a
	 * half-resolved path is never offered as a scope.
	 * @param pathSegments the segments of
	 * {@link org.karnak.backend.model.standard.ModuleAttribute#getTagPath()}
	 * @return the enclosing sequences, empty when the attribute is at the root of the
	 * module or the path could not be read
	 */
	static List<String> ancestorsOf(String[] pathSegments) {
		if (pathSegments == null || pathSegments.length < 2) {
			return List.of();
		}
		List<String> ancestors = new ArrayList<>();
		for (int i = 0; i < pathSegments.length - 1; i++) {
			try {
				ancestors.add(TagUtils.toString(TagUtils.intFromHexString(pathSegments[i])));
			}
			catch (RuntimeException e) {
				return List.of();
			}
		}
		return List.copyOf(ancestors);
	}

	/**
	 * The value a picked tag contributes once scoped.
	 *
	 * <p>
	 * {@link TagScope#IN_SEQUENCE} yields a floating path, so the sequences it names may
	 * themselves be nested anywhere. A tag with no known enclosing sequence stays a plain
	 * tag under that scope, there being no path to build.
	 * @param tagValue the picked tag, in {@code (gggg,eeee)} form
	 * @param ancestors the sequences enclosing it, outermost first
	 * @param scope how the tag must be scoped, {@code null} meaning any level
	 * @return the value to store on the profile element
	 */
	static String scopedValue(String tagValue, List<String> ancestors, TagScope scope) {
		if (scope == null) {
			return tagValue;
		}
		return switch (scope) {
			case ROOT_ONLY -> "." + tagValue;
			case IN_SEQUENCE ->
				ancestors == null || ancestors.isEmpty() ? tagValue : String.join(".", ancestors) + "." + tagValue;
			case ANY_LEVEL -> tagValue;
		};
	}

}