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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.ArrayList;
import java.util.List;
import org.karnak.backend.model.standard.AttributeDetail;
import org.karnak.backend.service.DicomStandardService;

/**
 * Editable list of DICOM tags used inside the profile element editor. Tags are added by
 * searching / browsing the DICOM dictionary ({@link TagPickerDialog}). Each tag is shown
 * as a removable chip displaying its value and attribute name. In single mode only one
 * tag is kept.
 */
public class TagPickerField extends VerticalLayout {

	private final transient DicomStandardService dicomStandardService;

	private final boolean multi;

	private final List<String> tags = new ArrayList<>();

	private final FlexLayout chips = new FlexLayout();

	public TagPickerField(DicomStandardService dicomStandardService, String label, boolean multi) {
		this.dicomStandardService = dicomStandardService;
		this.multi = multi;

		setPadding(false);
		setSpacing(false);

		Span title = new Span(label);
		title.getStyle().set("font-weight", "bold");

		chips.setFlexWrap(FlexLayout.FlexWrap.WRAP);
		chips.getStyle().set("gap", "5px").set("margin", "5px 0");

		Button browse = new Button("Browse / search", VaadinIcon.SEARCH.create(), event -> openPicker());
		browse.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

		add(title, chips, browse);
		refreshChips();
	}

	private void openPicker() {
		new TagPickerDialog(dicomStandardService, multi, selected -> {
			selected.forEach(this::addTag);
			refreshChips();
		}).open();
	}

	private void addTag(String tag) {
		if (tag == null || tag.isBlank()) {
			return;
		}
		String normalized = tag.trim();
		if (!multi) {
			tags.clear();
		}
		if (!tags.contains(normalized)) {
			tags.add(normalized);
		}
		refreshChips();
	}

	private void refreshChips() {
		chips.removeAll();
		for (String tag : tags) {
			chips.add(buildChip(tag));
		}
	}

	private Span buildChip(String tag) {
		Span chip = new Span();
		chip.getStyle()
			.set("background-color", "var(--lumo-contrast-10pct)")
			.set("border-radius", "var(--lumo-border-radius-m)")
			.set("padding", "2px 4px 2px 8px")
			.set("display", "inline-flex")
			.set("align-items", "center");
		chip.add(new Span(tag));

		String name = resolveName(tag);
		if (name != null) {
			Span nameSpan = new Span(name);
			nameSpan.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-left", "6px");
			chip.add(nameSpan);
		}

		Button remove = new Button(VaadinIcon.CLOSE_SMALL.create(), event -> {
			tags.remove(tag);
			refreshChips();
		});
		remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
		chip.add(remove);
		return chip;
	}

	/**
	 * The attribute name (keyword) of a concrete tag value, or {@code null} otherwise.
	 */
	private String resolveName(String tagValue) {
		String hex = tagValue.replaceAll("[(),\\s]", "");
		if (hex.matches("[0-9A-Fa-f]{8}")) {
			AttributeDetail detail = dicomStandardService.attributeDetail(hex.toLowerCase());
			if (detail != null) {
				return detail.keyword() != null && !detail.keyword().isBlank() ? detail.keyword() : detail.name();
			}
		}
		return null;
	}

	/** The currently selected tag values, in display order. */
	public List<String> getTags() {
		return new ArrayList<>(tags);
	}

	/** Replace the current selection. */
	public void setTags(List<String> values) {
		tags.clear();
		if (values != null) {
			values.forEach(this::addTag);
		}
		refreshChips();
	}

}
