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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

import com.vaadin.flow.component.UI;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.standard.AttributeDetail;
import org.karnak.backend.service.DicomStandardService;
import org.mockito.Mockito;

/**
 * The dictionary is stubbed here: what matters is that the field and its dialog build and
 * round-trip values, not what the standard holds — {@link TagScopesTest} covers that.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class TagPickerFieldTest {

	private final DicomStandardService dicomStandardService = Mockito.mock(DicomStandardService.class);

	// Vaadin keeps the current UI in a WeakReference, so a strong reference is held here
	// to keep it alive for the whole test.
	private UI ui;

	@BeforeEach
	void setUp() {
		ui = new UI();
		UI.setCurrent(ui);
		Mockito.when(dicomStandardService.listModuleIds()).thenReturn(List.of("patient", "sr-document-content"));
		Mockito.when(dicomStandardService.searchAttributes(anyString(), anyBoolean())).thenReturn(List.of());
		Mockito.when(dicomStandardService.attributeDetail(anyString())).thenReturn(null);
	}

	@AfterEach
	void tearDown() {
		UI.setCurrent(null);
		ui = null;
	}

	private TagPickerField field(TagPickerField.PathMode pathMode) {
		return new TagPickerField(dicomStandardService, "Tags", true, pathMode);
	}

	@Test
	void keeps_a_tag_path_as_written() {
		TagPickerField field = field(TagPickerField.PathMode.ANY);

		field.setTags(List.of("(0040,0275).(0020,0010)"));

		assertEquals(List.of("(0040,0275).(0020,0010)"), field.getTags());
	}

	@Test
	void keeps_an_anchored_tag_as_written() {
		TagPickerField field = field(TagPickerField.PathMode.ANY);

		field.setTags(List.of(".(0020,0010)"));

		assertEquals(List.of(".(0020,0010)"), field.getTags());
	}

	@Test
	void shows_an_existing_path_even_where_paths_cannot_be_added() {
		// A path authored through the YAML editor must still be readable in the editor of
		// an element that only accepts a top-level tag
		TagPickerField field = new TagPickerField(dicomStandardService, "Tag", false, TagPickerField.PathMode.NONE);

		field.setTags(List.of("(0040,0275).(0020,0010)"));

		assertEquals(List.of("(0040,0275).(0020,0010)"), field.getTags());
	}

	@Test
	void names_every_segment_of_a_path_on_its_chip() {
		Mockito.when(dicomStandardService.attributeDetail("00400275"))
			.thenReturn(new AttributeDetail("00400275", "RequestAttributesSequence", "Request Attributes Sequence",
					"false", "(0040,0275)", "1", "SQ"));
		Mockito.when(dicomStandardService.attributeDetail("00200010"))
			.thenReturn(new AttributeDetail("00200010", "StudyID", "Study ID", "false", "(0020,0010)", "1", "SH"));
		TagPickerField field = field(TagPickerField.PathMode.ANY);

		field.setTags(List.of("(0040,0275).(0020,0010)"));

		String rendered = field.getElement().getTextRecursively();
		assertTrue(rendered.contains("RequestAttributesSequence › StudyID"),
				() -> "Chip should name both segments, was: " + rendered);
	}

	@Test
	void builds_the_picker_dialog_with_the_scope_selector() {
		assertDoesNotThrow(() -> new TagPickerDialog(dicomStandardService, true, true, selected -> {
		}));
	}

	@Test
	void builds_the_picker_dialog_without_the_scope_selector() {
		assertDoesNotThrow(() -> new TagPickerDialog(dicomStandardService, false, false, selected -> {
		}));
	}

}