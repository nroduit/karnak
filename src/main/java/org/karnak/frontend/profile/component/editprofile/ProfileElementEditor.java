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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.ExcludedTagEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.enums.DeidActionType;
import org.karnak.backend.enums.ProfileItemType;
import org.karnak.backend.service.DicomStandardService;
import org.karnak.backend.service.profilepipe.ProfilePipeService;
import org.karnak.frontend.profile.component.errorprofile.ProfileError;

/**
 * Dialog used to add a new profile element or edit an existing one. The form adapts to
 * the selected {@link ProfileItemType} (the supported "core" set) and validates the
 * element through {@link ProfilePipeService#validateElement} before persisting it. Tags
 * are filled with the {@link TagPickerField} (search / browse the DICOM dictionary).
 */
@NullUnmarked
public class ProfileElementEditor extends Dialog {

	/** The element types that can be created / edited from the UI. */
	private static final List<ProfileItemType> SUPPORTED_TYPES = List.of(ProfileItemType.ACTION_TAGS,
			ProfileItemType.ACTION_DATES, ProfileItemType.REPLACE_UID, ProfileItemType.BASIC_DICOM,
			ProfileItemType.CLEAN_PIXEL_DATA, ProfileItemType.DEFACING, ProfileItemType.ADD_TAG,
			ProfileItemType.ACTION_PRIVATETAGS);

	private static final List<String> DATE_OPTIONS = List.of("shift", "shift_range", "shift_by_tag", "date_format");

	private final transient ProfilePipeService profilePipeService;

	private final transient DicomStandardService dicomStandardService;

	private final Long profileId;

	private final Long elementId;

	private final transient Runnable onSaved;

	private final TextField nameField = new TextField("Name");

	private final ComboBox<ProfileItemType> typeComboBox = new ComboBox<>("Type");

	private final TextField conditionField = new TextField("Condition (optional)");

	private final VerticalLayout dynamicSection = new VerticalLayout();

	private final Span errorLabel = new Span();

	// Per-type inputs, rebuilt when the type/option changes.
	private ComboBox<DeidActionType> actionComboBox;

	private ComboBox<String> dateOptionComboBox;

	private final VerticalLayout dateArgsSection = new VerticalLayout();

	private TagPickerField includedTags;

	private TagPickerField excludedTags;

	private TagPickerField singleTag;

	private final Map<String, Supplier<String>> argValueSuppliers = new LinkedHashMap<>();

	public ProfileElementEditor(ProfilePipeService profilePipeService, DicomStandardService dicomStandardService,
			Long profileId, ProfileElementEntity existing, List<ProfileElementEntity> siblings, Runnable onSaved) {
		this.profilePipeService = profilePipeService;
		this.dicomStandardService = dicomStandardService;
		this.profileId = profileId;
		this.elementId = existing != null ? existing.getId() : null;
		this.onSaved = onSaved;

		setHeaderTitle(existing != null ? "Edit element" : "Add element");
		setWidth("640px");

		nameField.setWidthFull();
		conditionField.setWidthFull();
		// A unique type already present in the profile is not offered again.
		typeComboBox.setItems(SUPPORTED_TYPES.stream()
			.filter(type -> !type.isUnique() || !isUniqueTypePresent(type, siblings))
			.toList());
		typeComboBox.setItemLabelGenerator(ProfileElementEditor::typeLabel);
		typeComboBox.setWidthFull();
		typeComboBox.addValueChangeListener(event -> {
			buildDynamicSection(event.getValue(), null);
			if (event.getValue() != null && event.getValue().isUnique() && nameField.getValue().isBlank()) {
				nameField.setValue(shortLabel(event.getValue()));
			}
		});

		dynamicSection.setPadding(false);
		dynamicSection.setSpacing(true);
		dateArgsSection.setPadding(false);
		dateArgsSection.setSpacing(false);
		errorLabel.getStyle().set("color", "var(--aura-red-text)");

		VerticalLayout body = new VerticalLayout(nameField, typeComboBox, dynamicSection, conditionField, errorLabel);
		body.setPadding(false);
		add(body);

		Button save = new Button("Save", event -> save());
		save.getElement().getThemeList().add("primary");
		Button cancel = new Button("Cancel", event -> close());
		getFooter().add(cancel, save);

		prefill(existing);
	}

	/** Whether the element with this codename can be edited with this dialog. */
	public static boolean supports(String codename) {
		ProfileItemType type = ProfileItemType.getType(codename);
		return type != null && SUPPORTED_TYPES.contains(type);
	}

	private static String typeLabel(ProfileItemType type) {
		return shortLabel(type) + " (" + type.getClassAlias() + ")";
	}

	private static String shortLabel(ProfileItemType type) {
		return switch (type) {
			case ACTION_TAGS -> "Apply action to specific tags";
			case ACTION_DATES -> "Shift / format dates";
			case REPLACE_UID -> "Replace UIDs";
			case BASIC_DICOM -> "Basic DICOM confidentiality profile";
			case CLEAN_PIXEL_DATA -> "Clean pixel data";
			case DEFACING -> "Defacing (clean recognizable visual features)";
			case ADD_TAG -> "Add a tag";
			case ACTION_PRIVATETAGS -> "Apply action to private tags";
			default -> type.getClassAlias();
		};
	}

	/** Whether a unique type is already used by another element of the profile. */
	private boolean isUniqueTypePresent(ProfileItemType type, List<ProfileElementEntity> siblings) {
		if (siblings == null) {
			return false;
		}
		return siblings.stream()
			.anyMatch(e -> type.getClassAlias().equals(e.getCodename())
					&& !java.util.Objects.equals(e.getId(), elementId));
	}

	private void prefill(ProfileElementEntity existing) {
		if (existing == null) {
			typeComboBox.setValue(ProfileItemType.ACTION_TAGS);
			return;
		}
		nameField.setValue(existing.getName() != null ? existing.getName() : "");
		conditionField.setValue(existing.getCondition() != null ? existing.getCondition() : "");
		ProfileItemType type = ProfileItemType.getType(existing.getCodename());
		if (type != null && SUPPORTED_TYPES.contains(type)) {
			typeComboBox.setValue(type);
			buildDynamicSection(type, existing);
		}
	}

	private void buildDynamicSection(ProfileItemType type, ProfileElementEntity existing) {
		dynamicSection.removeAll();
		argValueSuppliers.clear();
		actionComboBox = null;
		dateOptionComboBox = null;
		includedTags = null;
		excludedTags = null;
		singleTag = null;
		if (type == null) {
			return;
		}
		switch (type) {
			case ACTION_TAGS, ACTION_PRIVATETAGS -> {
				dynamicSection.add(buildActionComboBox(DeidActionType.values(), existing));
				includedTags = new TagPickerField(dicomStandardService, "Tags", true);
				excludedTags = new TagPickerField(dicomStandardService, "Excluded tags", true);
				prefillTags(existing);
				dynamicSection.add(includedTags, excludedTags);
			}
			case REPLACE_UID -> {
				dynamicSection.add(buildActionComboBox(new DeidActionType[] { DeidActionType.NEW_UID,
						DeidActionType.REMOVE, DeidActionType.REPLACE_NULL }, existing));
				includedTags = new TagPickerField(dicomStandardService, "Tags", true);
				prefillTags(existing);
				dynamicSection.add(includedTags);
			}
			case ACTION_DATES -> {
				dateOptionComboBox = new ComboBox<>("Option", DATE_OPTIONS);
				dateOptionComboBox.setWidthFull();
				dateOptionComboBox.addValueChangeListener(event -> buildDateArgs(event.getValue(), existing));
				includedTags = new TagPickerField(dicomStandardService, "Tags (optional)", true);
				prefillTags(existing);
				dynamicSection.add(dateOptionComboBox, dateArgsSection, includedTags);
				dateOptionComboBox
					.setValue(existing != null && existing.getOption() != null ? existing.getOption() : "shift");
			}
			case ADD_TAG -> {
				singleTag = new TagPickerField(dicomStandardService, "Tag", false);
				if (existing != null && !existing.getIncludedTagEntities().isEmpty()) {
					singleTag.setTags(List.of(existing.getIncludedTagEntities().getFirst().getTagValue()));
				}
				TextField valueField = new TextField("Value");
				valueField.setWidthFull();
				valueField.setValue(argValue(existing, "value"));
				argValueSuppliers.put("value", valueField::getValue);
				dynamicSection.add(singleTag, valueField);
			}
			default -> {
				// BASIC_DICOM / CLEAN_PIXEL_DATA / DEFACING: no extra configuration
			}
		}
	}

	private ComboBox<DeidActionType> buildActionComboBox(DeidActionType[] options, ProfileElementEntity existing) {
		actionComboBox = new ComboBox<>("Action");
		actionComboBox.setItems(options);
		actionComboBox.setWidthFull();
		if (existing != null) {
			actionComboBox.setValue(DeidActionType.fromSymbol(existing.getAction()));
		}
		return actionComboBox;
	}

	private void prefillTags(ProfileElementEntity existing) {
		if (existing == null) {
			return;
		}
		if (includedTags != null) {
			includedTags
				.setTags(existing.getIncludedTagEntities().stream().map(IncludedTagEntity::getTagValue).toList());
		}
		if (excludedTags != null) {
			excludedTags
				.setTags(existing.getExcludedTagEntities().stream().map(ExcludedTagEntity::getTagValue).toList());
		}
	}

	private void buildDateArgs(String option, ProfileElementEntity existing) {
		dateArgsSection.removeAll();
		argValueSuppliers.clear();
		if (option == null) {
			return;
		}
		switch (option) {
			case "shift" -> {
				addIntArg("seconds", "Seconds", existing);
				addIntArg("days", "Days", existing);
			}
			case "shift_range" -> {
				addIntArg("max_seconds", "Max seconds", existing);
				addIntArg("max_days", "Max days", existing);
				addIntArg("min_seconds", "Min seconds (optional)", existing);
				addIntArg("min_days", "Min days (optional)", existing);
			}
			case "shift_by_tag" -> {
				addTextArg("days_tag", "Days tag", existing);
				addTextArg("seconds_tag", "Seconds tag", existing);
			}
			case "date_format" -> {
				ComboBox<String> remove = new ComboBox<>("Remove", List.of("day", "month_day"));
				remove.setWidthFull();
				remove.setValue(argValueOrNull(existing, "remove"));
				argValueSuppliers.put("remove", remove::getValue);
				dateArgsSection.add(remove);
			}
			default -> {
				// no arguments
			}
		}
	}

	private void addIntArg(String key, String label, ProfileElementEntity existing) {
		IntegerField field = new IntegerField(label);
		field.setWidthFull();
		String current = argValue(existing, key);
		if (!current.isBlank()) {
			try {
				field.setValue(Integer.valueOf(current));
			}
			catch (NumberFormatException ignored) {
				// leave empty when the stored value is not an integer
			}
		}
		argValueSuppliers.put(key, () -> field.getValue() != null ? String.valueOf(field.getValue()) : null);
		dateArgsSection.add(field);
	}

	private void addTextArg(String key, String label, ProfileElementEntity existing) {
		TextField field = new TextField(label);
		field.setWidthFull();
		field.setValue(argValue(existing, key));
		argValueSuppliers.put(key, field::getValue);
		dateArgsSection.add(field);
	}

	private void save() {
		errorLabel.setText("");
		ProfileItemType type = typeComboBox.getValue();
		if (type == null) {
			errorLabel.setText("Please choose a type");
			return;
		}
		if (nameField.getValue() == null || nameField.getValue().isBlank()) {
			if (type.isUnique()) {
				// Unique types do not need a specific name; default it.
				nameField.setValue(shortLabel(type));
			}
			else {
				errorLabel.setText("Please give the element a name");
				return;
			}
		}

		ProfileElementEntity element = buildElement(type);
		ProfileError error = profilePipeService.validateElement(element);
		if (error.getError() != null) {
			errorLabel.setText(error.getError());
			return;
		}
		profilePipeService.saveElement(profileId, element);
		if (onSaved != null) {
			onSaved.run();
		}
		close();
	}

	private ProfileElementEntity buildElement(ProfileItemType type) {
		ProfileElementEntity element = new ProfileElementEntity();
		element.setId(elementId);
		element.setName(nameField.getValue().trim());
		element.setCodename(type.getClassAlias());
		element.setCondition(blankToNull(conditionField.getValue()));
		if (actionComboBox != null && actionComboBox.getValue() != null) {
			element.setAction(actionComboBox.getValue().getSymbol());
		}
		if (dateOptionComboBox != null) {
			element.setOption(dateOptionComboBox.getValue());
		}

		List<ArgumentEntity> arguments = new ArrayList<>();
		argValueSuppliers.forEach((key, supplier) -> {
			String value = supplier.get();
			if (value != null && !value.isBlank()) {
				arguments.add(new ArgumentEntity(key, value, element));
			}
		});
		element.setArgumentEntities(arguments);

		element.setIncludedTagEntities(toIncludedTags(includedTags != null ? includedTags : singleTag, element));
		element.setExcludedTagEntities(toExcludedTags(excludedTags, element));
		return element;
	}

	private static List<IncludedTagEntity> toIncludedTags(TagPickerField field, ProfileElementEntity element) {
		if (field == null) {
			return new ArrayList<>();
		}
		return field.getTags().stream().map(tag -> new IncludedTagEntity(tag, element)).collect(toMutableList());
	}

	private static List<ExcludedTagEntity> toExcludedTags(TagPickerField field, ProfileElementEntity element) {
		if (field == null) {
			return new ArrayList<>();
		}
		return field.getTags().stream().map(tag -> new ExcludedTagEntity(tag, element)).collect(toMutableList());
	}

	private static <T> java.util.stream.Collector<T, ?, List<T>> toMutableList() {
		return java.util.stream.Collectors.toCollection(ArrayList::new);
	}

	private static String argValue(ProfileElementEntity element, String key) {
		String value = argValueOrNull(element, key);
		return value != null ? value : "";
	}

	private static String argValueOrNull(ProfileElementEntity element, String key) {
		if (element == null) {
			return null;
		}
		return element.getArgumentEntities()
			.stream()
			.filter(a -> key.equals(a.getArgumentKey()))
			.map(ArgumentEntity::getArgumentValue)
			.findFirst()
			.orElse(null);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

}
