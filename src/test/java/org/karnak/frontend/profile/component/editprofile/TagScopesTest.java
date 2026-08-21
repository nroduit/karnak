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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.dcm4che3.util.TagUtils;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.profilepipe.TagPathPattern;
import org.karnak.backend.model.standard.ModuleAttribute;
import org.karnak.backend.model.standard.StandardDICOM;
import org.karnak.frontend.profile.component.editprofile.TagScopes.TagScope;

@DisplayNameGeneration(ReplaceUnderscores.class)
class TagScopesTest {

	private static final String STUDY_ID = "(0020,0010)";

	private static final List<String> IN_REQUEST_ATTRIBUTES = List.of("(0040,0275)");

	@Nested
	class AncestorsOf {

		@Test
		void returns_nothing_for_an_attribute_at_the_root_of_the_module() {
			assertTrue(TagScopes.ancestorsOf(new String[] { "00200010" }).isEmpty());
		}

		@Test
		void keeps_every_segment_but_the_attribute_itself() {
			assertEquals(List.of("(0040,0275)"), TagScopes.ancestorsOf(new String[] { "00400275", "00200010" }));
		}

		@Test
		void keeps_the_order_of_a_deeply_nested_attribute() {
			assertEquals(List.of("(0040,0555)", "(0040,08EA)"),
					TagScopes.ancestorsOf(new String[] { "00400555", "004008ea", "00080121" }));
		}

		@Test
		void drops_the_hierarchy_when_a_segment_is_not_a_tag() {
			assertTrue(TagScopes.ancestorsOf(new String[] { "not-a-tag", "00200010" }).isEmpty());
		}

		@Test
		void tolerates_a_missing_path() {
			assertTrue(TagScopes.ancestorsOf(null).isEmpty());
			assertTrue(TagScopes.ancestorsOf(new String[0]).isEmpty());
		}

	}

	@Nested
	class ScopedValue {

		@Test
		void leaves_the_tag_alone_at_any_level() {
			assertEquals(STUDY_ID, TagScopes.scopedValue(STUDY_ID, IN_REQUEST_ATTRIBUTES, TagScope.ANY_LEVEL));
		}

		@Test
		void anchors_the_tag_for_the_root_only() {
			assertEquals(".(0020,0010)", TagScopes.scopedValue(STUDY_ID, List.of(), TagScope.ROOT_ONLY));
		}

		@Test
		void builds_a_floating_path_inside_the_sequence() {
			assertEquals("(0040,0275).(0020,0010)",
					TagScopes.scopedValue(STUDY_ID, IN_REQUEST_ATTRIBUTES, TagScope.IN_SEQUENCE));
		}

		@Test
		void keeps_a_plain_tag_when_no_sequence_encloses_it() {
			assertEquals(STUDY_ID, TagScopes.scopedValue(STUDY_ID, List.of(), TagScope.IN_SEQUENCE));
		}

		@Test
		void keeps_a_plain_tag_when_scoping_is_disabled() {
			assertEquals(STUDY_ID, TagScopes.scopedValue(STUDY_ID, IN_REQUEST_ATTRIBUTES, null));
		}

	}

	/**
	 * The picker feeds the paths of the DICOM standard straight into the profile grammar,
	 * so what it produces has to be parseable by {@link TagPathPattern}.
	 */
	@Nested
	class AgainstTheRealStandard {

		private static final StandardDICOM STANDARD = new StandardDICOM();

		@Test
		void every_nested_module_attribute_yields_a_valid_path() {
			int nested = 0;
			for (String moduleId : STANDARD.getModuleIds()) {
				for (ModuleAttribute attribute : STANDARD.getAttributeListByModule(moduleId)) {
					String[] segments = attribute.getTagPath().split(":");
					List<String> ancestors = TagScopes.ancestorsOf(segments);
					String tag = tagOf(segments[segments.length - 1]);
					if (ancestors.isEmpty() || tag == null) {
						continue;
					}
					nested++;
					String value = TagScopes.scopedValue(tag, ancestors, TagScope.IN_SEQUENCE);
					assertTrue(TagPathPattern.isValid(value),
							() -> "Not a valid tag path: " + value + " (module " + moduleId + ")");
				}
			}
			assertTrue(nested > 0, "The standard should hold nested module attributes");
		}

		@Test
		void a_nested_attribute_is_not_scoped_to_the_root() {
			String value = TagScopes.scopedValue(STUDY_ID, IN_REQUEST_ATTRIBUTES, TagScope.IN_SEQUENCE);

			// Floating, so the sequence itself may be nested anywhere
			assertFalse(TagPathPattern.parse(value).anchored());
			assertEquals(2, TagPathPattern.parse(value).segments().size());
		}

		/**
		 * The leaf of a module path in {@code (gggg,eeee)} form, {@code null} if
		 * unreadable.
		 */
		private static String tagOf(String leafHex) {
			try {
				return TagUtils.toString(TagUtils.intFromHexString(leafHex));
			}
			catch (RuntimeException e) {
				return null;
			}
		}

	}

}