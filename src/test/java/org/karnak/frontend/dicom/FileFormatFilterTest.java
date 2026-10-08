/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayNameGeneration(ReplaceUnderscores.class)
class FileFormatFilterTest {

	@Nested
	class Accept {

		@Test
		void accepts_directories(@TempDir File tempDir) {
			var filter = new FileFormatFilter("dcm", "DICOM files");
			assertTrue(filter.accept(tempDir));
		}

		@Test
		void accepts_matching_extension() {
			var filter = new FileFormatFilter("dcm", "DICOM files");
			assertTrue(filter.accept(new File("study.dcm")));
		}

		@Test
		void rejects_non_matching_extension() {
			var filter = new FileFormatFilter("dcm", "DICOM files");
			assertFalse(filter.accept(new File("image.png")));
		}

		@Test
		void rejects_null_file() {
			var filter = new FileFormatFilter("dcm", "DICOM files");
			assertFalse(filter.accept(null));
		}

		@Test
		void accepts_any_registered_extension() {
			var filter = new FileFormatFilter(new String[] { "dcm", "dicom", "ima" }, "DICOM files");
			assertTrue(filter.accept(new File("study.dcm")));
			assertTrue(filter.accept(new File("study.dicom")));
			assertTrue(filter.accept(new File("study.ima")));
			assertFalse(filter.accept(new File("study.jpg")));
		}

		@Test
		void extension_matching_is_case_insensitive() {
			var filter = new FileFormatFilter("DCM", "DICOM");
			assertTrue(filter.accept(new File("study.dcm")));
		}

	}

	@Nested
	class GetExtension {

		@Test
		void extracts_lowercase_extension() {
			var filter = new FileFormatFilter("dcm", null);
			assertEquals("png", filter.getExtension(new File("image.PNG")));
		}

		@Test
		void returns_null_for_file_without_extension() {
			var filter = new FileFormatFilter("dcm", null);
			assertNull(filter.getExtension(new File("README")));
		}

		@Test
		void returns_null_for_null_file() {
			var filter = new FileFormatFilter("dcm", null);
			assertNull(filter.getExtension(null));
		}

		@Test
		void returns_null_for_dot_only_file() {
			var filter = new FileFormatFilter("dcm", null);
			assertNull(filter.getExtension(new File(".")));
		}

	}

	@Nested
	class AddExtension {

		@Test
		void strips_star_and_dot_prefix() {
			var filter = new FileFormatFilter("*.dcm", "DICOM");
			assertTrue(filter.accept(new File("study.dcm")));
		}

		@Test
		void strips_dot_prefix() {
			var filter = new FileFormatFilter(".dcm", "DICOM");
			assertTrue(filter.accept(new File("study.dcm")));
		}

		@Test
		void first_extension_becomes_default() {
			var filter = new FileFormatFilter("dcm", "DICOM");
			assertEquals("dcm", filter.getDefaultExtension());
		}

	}

	@Nested
	class Description {

		@Test
		void includes_extension_list_by_default() {
			var filter = new FileFormatFilter("dcm", "DICOM");
			assertTrue(filter.getDescription().contains("*.dcm"));
			assertTrue(filter.getDescription().contains("DICOM"));
		}

		@Test
		void shows_only_description_when_extension_list_disabled() {
			var filter = new FileFormatFilter("dcm", "DICOM");
			filter.setExtensionListInDescription(false);
			assertEquals("DICOM", filter.getDescription());
		}

		@Test
		void shows_multiple_extensions() {
			var filter = new FileFormatFilter(new String[] { "dcm", "ima" }, "DICOM");
			var desc = filter.getDescription();
			assertTrue(desc.contains("*.dcm"));
			assertTrue(desc.contains("*.ima"));
		}

		@Test
		void shows_only_extensions_when_no_description_set() {
			var filter = new FileFormatFilter(new String[] { "dcm" });
			var desc = filter.getDescription();
			assertTrue(desc.startsWith("("));
			assertTrue(desc.contains("*.dcm"));
		}

	}

}
