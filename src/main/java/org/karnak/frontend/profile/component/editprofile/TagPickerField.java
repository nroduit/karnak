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

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.model.profilepipe.TagActionMap;
import org.karnak.backend.model.profilepipe.TagPathPattern;
import org.karnak.backend.model.standard.AttributeDetail;
import org.karnak.backend.service.DicomStandardService;

/**
 * Editable list of DICOM tags used inside the profile element editor. Tags are added by
 * searching / browsing the DICOM dictionary ({@link TagPickerDialog}) or typed directly.
 * Each tag is shown as a removable chip displaying its value and attribute name. In
 * single mode only one tag is kept.
 *
 * <p>
 * A value may also be a tag path naming the sequences the tag is nested in —
 * {@code (0040,0275).(0040,0009)} — in which case the chip names every segment and typed
 * input is validated against {@link TagPathPattern}. How much of the grammar an element
 * accepts is given by its {@link PathMode}: an element matching existing tags takes the
 * wildcards too, one adding an attribute needs a destination it can write to.
 */
@NullUnmarked
public class TagPickerField extends VerticalLayout {

	/** What a profile element accepts as a tag value. */
	public enum PathMode {

		/** A tag of the top-level dataset only: no path. */
		NONE,
		/**
		 * A path naming every sequence exactly, the only form that designates where an
		 * attribute would be added.
		 */
		LITERAL,
		/** Any path, wildcards included, since it only has to match existing tags. */
		ANY

	}

	private static final String PATH_HELPER = "A tag applies at any depth. Prefix with a dot for the root only "
			+ "(.(0010,0010)), or name the enclosing sequences to scope it ((0040,0275).(0040,0009)). "
			+ "Use * for one level and ** for one or more.";

	private static final String LITERAL_PATH_HELPER = "The tag is added to the top-level dataset, or inside the "
			+ "sequences named before it ((0040,0275).(0040,0009)), which are created when the object does not "
			+ "hold them. Wildcards are not accepted here.";

	private final transient DicomStandardService dicomStandardService;

	private final boolean multi;

	private final PathMode pathMode;

	private final List<String> tags = new ArrayList<>();

	private final FlexLayout chips = new FlexLayout();

	private final TextField manualEntry = new TextField();

	public TagPickerField(DicomStandardService dicomStandardService, String label, boolean multi, PathMode pathMode) {
		this.dicomStandardService = dicomStandardService;
		this.multi = multi;
		this.pathMode = pathMode;

		setPadding(false);
		setSpacing(false);

		Span title = new Span(label);
		title.getStyle().set("font-weight", "bold");

		chips.setFlexWrap(FlexLayout.FlexWrap.WRAP);
		chips.getStyle().set("gap", "5px").set("margin", "5px 0");

		add(title, chips, buildEntryBar());
		refreshChips();
	}

	/** The free-text entry of a tag or path, next to the browse button. */
	private HorizontalLayout buildEntryBar() {
		Button browse = new Button("Browse / search", VaadinIcon.SEARCH.create(), event -> openPicker());
		browse.addThemeVariants(ButtonVariant.TERTIARY);

		manualEntry
			.setPlaceholder(allowsPaths() ? "(0010,0010), .(0010,0010) or (0040,0275).(0040,0009)" : "(0010,0010)");
		manualEntry.setClearButtonVisible(true);
		manualEntry.setWidth("340px");
		if (allowsPaths()) {
			manualEntry.setHelperText(pathMode == PathMode.LITERAL ? LITERAL_PATH_HELPER : PATH_HELPER);
		}
		manualEntry.addKeyPressListener(Key.ENTER, event -> submitManualEntry());

		Button addTyped = new Button("Add", VaadinIcon.PLUS.create(), event -> submitManualEntry());
		addTyped.addThemeVariants(ButtonVariant.TERTIARY);

		HorizontalLayout bar = new HorizontalLayout(manualEntry, addTyped, browse);
		bar.setAlignItems(Alignment.BASELINE);
		bar.setPadding(false);
		return bar;
	}

	/** Validates the typed value, adding it as a chip or reporting why it was refused. */
	private void submitManualEntry() {
		String value = manualEntry.getValue();
		if (value == null || value.isBlank()) {
			return;
		}
		String trimmed = value.trim();
		String error = validationError(trimmed);
		if (error != null) {
			manualEntry.setInvalid(true);
			manualEntry.setErrorMessage(error);
			return;
		}
		manualEntry.setInvalid(false);
		manualEntry.clear();
		addTag(trimmed);
	}

	/**
	 * Why {@code value} cannot be used, or {@code null} when it is valid: a
	 * {@code (gggg,eeee)} tag, an {@code X} wildcard pattern, or — where they are allowed
	 * — a tag path.
	 */
	private String validationError(String value) {
		if (TagPathPattern.isPath(value)) {
			if (!allowsPaths()) {
				return "This profile element only accepts a tag of the top-level dataset, not a path";
			}
			if (!TagPathPattern.isValid(value)) {
				return "Invalid tag path";
			}
			if (pathMode == PathMode.LITERAL && TagPathPattern.parse(value).literalTags().isEmpty()) {
				return "This profile element needs every sequence named exactly, a wildcard does not say "
						+ "where the tag would be added";
			}
			return null;
		}
		String hex = hexOf(value);
		if (hex.matches("[0-9A-Fa-f]{8}") || TagActionMap.isValidPattern(hex)) {
			return null;
		}
		return "Expected a tag such as (0010,0010) or a pattern such as 0010XXXX";
	}

	private boolean allowsPaths() {
		return pathMode != PathMode.NONE;
	}

	private void openPicker() {
		new TagPickerDialog(dicomStandardService, multi, allowsPaths(), selected -> {
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
			.set("background-color", "color-mix(in srgb, var(--vaadin-text-color) 10%, transparent)")
			.set("border-radius", "var(--vaadin-radius-m)")
			.set("padding", "2px 4px 2px 8px")
			.set("display", "inline-flex")
			.set("align-items", "center");
		chip.add(new Span(tag));

		String name = resolveName(tag);
		if (name != null) {
			Span nameSpan = new Span(name);
			nameSpan.getStyle().set("color", "var(--vaadin-text-color-secondary)").set("margin-left", "6px");
			chip.add(nameSpan);
		}

		Button remove = new Button(VaadinIcon.CLOSE_SMALL.create(), event -> {
			tags.remove(tag);
			refreshChips();
		});
		remove.addThemeVariants(ButtonVariant.TERTIARY, ButtonVariant.SMALL);
		chip.add(remove);
		return chip;
	}

	/**
	 * The readable label of a value: the attribute name of a concrete tag, or, for a
	 * path, the name of each of its segments. {@code null} when nothing can be resolved,
	 * so that the chip shows the raw value alone.
	 */
	private String resolveName(String tagValue) {
		if (!TagPathPattern.isPath(tagValue)) {
			return attributeName(tagValue);
		}
		StringBuilder label = new StringBuilder();
		boolean resolvedAny = false;
		// A leading dot yields an empty first segment: the top-level dataset
		for (String segment : tagValue.trim().split("\\.", -1)) {
			if (!label.isEmpty()) {
				label.append(" › ");
			}
			if (segment.isEmpty()) {
				label.append("root");
				continue;
			}
			String name = attributeName(segment);
			resolvedAny |= name != null;
			label.append(name != null ? name : segment.trim());
		}
		return resolvedAny ? label.toString() : null;
	}

	/**
	 * The attribute name (keyword) of a concrete tag value, or {@code null} otherwise.
	 */
	private String attributeName(String tagValue) {
		String hex = hexOf(tagValue);
		if (hex.matches("[0-9A-Fa-f]{8}")) {
			AttributeDetail detail = dicomStandardService.attributeDetail(hex.toLowerCase());
			if (detail != null) {
				return detail.keyword() != null && !detail.keyword().isBlank() ? detail.keyword() : detail.name();
			}
		}
		return null;
	}

	private static String hexOf(String tagValue) {
		return tagValue.replaceAll("[(),\\s]", "");
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
