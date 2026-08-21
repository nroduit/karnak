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
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.dcm4che3.util.TagUtils;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.model.standard.AttributeDetail;
import org.karnak.backend.model.standard.ModuleAttribute;
import org.karnak.backend.service.DicomStandardService;

/**
 * Dialog that helps the user find DICOM tags for a profile element: a free-text search
 * over the whole standard dictionary (keyword / name / tag) and a browse-by-module list
 * that drills into sequences. The selected tags are returned in the canonical
 * {@code (gggg,eeee)} form through the {@code onSelect} callback.
 *
 * <p>
 * When the profile element accepts them, the scope selector turns the selection into a
 * tag path instead — {@code .(gggg,eeee)} to pin the tag to the top-level dataset, or
 * {@code (0040,0275).(0040,0009)} to apply it only inside the sequence it was browsed in.
 * Browsing a module is what makes the enclosing sequences known, so that last scope is
 * only offered there.
 */
@NullUnmarked
public class TagPickerDialog extends Dialog {

	/**
	 * A row of the picker grid, normalised from either source (search or module).
	 *
	 * @param ancestors the sequences enclosing the tag, in {@code (gggg,eeee)} form and
	 * outermost first; always empty for a search result, which carries no hierarchy
	 */
	public record TagRow(String tagValue, String keyword, String name, String vr, int depth, List<String> ancestors) {
	}

	private final transient DicomStandardService dicomStandardService;

	private final transient Consumer<List<String>> onSelect;

	private final Grid<TagRow> grid = new Grid<>(TagRow.class, false);

	private final TextField searchField = new TextField();

	private final ComboBox<String> moduleComboBox = new ComboBox<>("Browse by module");

	private final Checkbox showRetired = new Checkbox("Show retired");

	private final RadioButtonGroup<TagScopes.TagScope> scopeGroup = new RadioButtonGroup<>();

	private final boolean allowPaths;

	public TagPickerDialog(DicomStandardService dicomStandardService, boolean multiSelect, boolean allowPaths,
			Consumer<List<String>> onSelect) {
		this.dicomStandardService = dicomStandardService;
		this.onSelect = onSelect;
		this.allowPaths = allowPaths;

		setHeaderTitle("Add DICOM tags");
		setWidth("760px");
		setHeight(allowPaths ? "700px" : "640px");

		buildSearchControls();
		buildGrid(multiSelect);

		VerticalLayout body = new VerticalLayout(buildSearchBar(), grid);
		if (allowPaths) {
			buildScopeSelector();
			body.add(scopeGroup);
		}
		body.setPadding(false);
		body.setSizeFull();
		body.setFlexGrow(1, grid);
		add(body);

		Button add = new Button("Add", event -> confirmSelection());
		add.getElement().getThemeList().add("primary");
		Button cancel = new Button("Cancel", event -> close());
		getFooter().add(cancel, add);

		// Initial list so the grid is never empty when opened.
		refreshFromSearch();
	}

	/**
	 * Builds the toolbar: a growing search field, a module selector and the retired
	 * toggle.
	 */
	private HorizontalLayout buildSearchBar() {
		HorizontalLayout bar = new HorizontalLayout(searchField, moduleComboBox, showRetired);
		bar.setWidthFull();
		bar.setAlignItems(Alignment.BASELINE);
		bar.setFlexGrow(1, searchField);
		return bar;
	}

	private void buildSearchControls() {
		searchField.setLabel("Search");
		searchField.setPlaceholder("Keyword, name or tag (e.g. PatientName, 0010,0010)");
		searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
		searchField.setClearButtonVisible(true);
		searchField.setValueChangeMode(ValueChangeMode.LAZY);
		searchField.setValueChangeTimeout(250);
		searchField.addValueChangeListener(event -> {
			moduleComboBox.clear();
			refreshFromSearch();
			refreshScopeAvailability();
		});

		moduleComboBox.setItems(dicomStandardService.listModuleIds());
		moduleComboBox.setPlaceholder("Select a module");
		moduleComboBox.setClearButtonVisible(true);
		moduleComboBox.setWidth("260px");
		moduleComboBox.addValueChangeListener(event -> {
			if (event.getValue() != null) {
				searchField.clear();
				refreshFromModule(event.getValue());
			}
			else {
				refreshFromSearch();
			}
			refreshScopeAvailability();
		});

		showRetired.addValueChangeListener(event -> {
			if (moduleComboBox.getValue() == null) {
				refreshFromSearch();
			}
		});
	}

	/**
	 * Builds the scope selector. {@link TagScopes.TagScope#IN_SEQUENCE} needs the
	 * enclosing sequences, which only the module browse knows, so it stays disabled while
	 * the grid shows search results.
	 */
	private void buildScopeSelector() {
		scopeGroup.setLabel("Apply the selected tags to");
		scopeGroup.setItems(TagScopes.TagScope.values());
		scopeGroup.setValue(TagScopes.TagScope.ANY_LEVEL);
		scopeGroup.setHelperText("Any level matches the tag wherever it appears, including inside sequences. "
				+ "Inside its sequence needs the module browse, which is where the hierarchy is known.");
		scopeGroup.setItemEnabledProvider(scope -> scope != TagScopes.TagScope.IN_SEQUENCE || isBrowsingModule());
		refreshScopeAvailability();
	}

	private boolean isBrowsingModule() {
		return moduleComboBox.getValue() != null;
	}

	/** Falls back to the default scope when the selected one is no longer available. */
	private void refreshScopeAvailability() {
		if (!allowPaths) {
			return;
		}
		if (scopeGroup.getValue() == TagScopes.TagScope.IN_SEQUENCE && !isBrowsingModule()) {
			scopeGroup.setValue(TagScopes.TagScope.ANY_LEVEL);
		}
		// Re-evaluates the enabled provider against the current mode
		scopeGroup.getDataProvider().refreshAll();
	}

	private void buildGrid(boolean multiSelect) {
		grid.setSelectionMode(multiSelect ? SelectionMode.MULTI : SelectionMode.SINGLE);
		grid.addColumn(TagRow::tagValue).setHeader("Tag").setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(this::displayName).setHeader("Attribute").setAutoWidth(true);
		grid.addColumn(TagRow::vr).setHeader("VR").setAutoWidth(true).setFlexGrow(0);
		if (allowPaths) {
			grid.addColumn(this::enclosingSequences).setHeader("In sequence").setAutoWidth(true);
		}
		grid.setSizeFull();
	}

	/**
	 * The keywords of the sequences the row is nested in, empty when it is at the root.
	 */
	private String enclosingSequences(TagRow row) {
		return row.ancestors().stream().map(this::keywordOf).collect(Collectors.joining(" › "));
	}

	private String keywordOf(String tagValue) {
		AttributeDetail detail = dicomStandardService.attributeDetail(hexOf(tagValue));
		if (detail == null) {
			return tagValue;
		}
		return detail.keyword() != null && !detail.keyword().isBlank() ? detail.keyword() : detail.name();
	}

	private static String hexOf(String tagValue) {
		return tagValue.replaceAll("[(),\\s]", "").toLowerCase();
	}

	/** Indents nested (sequence) attributes so the hierarchy is readable. */
	private String displayName(TagRow row) {
		String label = row.keyword() != null && !row.keyword().isBlank() ? row.keyword() : row.name();
		return row.depth() > 0 ? " ".repeat(row.depth() * 3) + "└ " + label : label;
	}

	private void refreshFromSearch() {
		List<TagRow> rows = dicomStandardService.searchAttributes(searchField.getValue(), showRetired.getValue())
			.stream()
			.map(this::toRow)
			.toList();
		grid.setItems(rows);
	}

	private void refreshFromModule(String moduleId) {
		List<TagRow> rows = new ArrayList<>();
		for (ModuleAttribute attribute : dicomStandardService.moduleAttributes(moduleId)) {
			String[] segments = attribute.getTagPath().split(":");
			String leafHex = segments[segments.length - 1];
			TagRow row = toRow(leafHex, TagScopes.ancestorsOf(segments));
			if (row != null) {
				rows.add(row);
			}
		}
		grid.setItems(rows);
	}

	private TagRow toRow(AttributeDetail detail) {
		return new TagRow(detail.tag(), detail.keyword(), detail.name(), detail.valueRepresentation(), 0, List.of());
	}

	private TagRow toRow(String tagHex, List<String> ancestors) {
		try {
			String tagValue = TagUtils.toString(TagUtils.intFromHexString(tagHex));
			AttributeDetail detail = dicomStandardService.attributeDetail(tagHex);
			int depth = ancestors.size();
			if (detail != null) {
				return new TagRow(tagValue, detail.keyword(), detail.name(), detail.valueRepresentation(), depth,
						ancestors);
			}
			return new TagRow(tagValue, null, tagValue, null, depth, ancestors);
		}
		catch (RuntimeException e) {
			return null;
		}
	}

	/** The value the selected row contributes, in the form the profile element stores. */
	private String scopedValue(TagRow row) {
		return TagScopes.scopedValue(row.tagValue(), row.ancestors(), allowPaths ? scopeGroup.getValue() : null);
	}

	private void confirmSelection() {
		List<String> selected = grid.getSelectedItems().stream().map(this::scopedValue).distinct().toList();
		if (!selected.isEmpty()) {
			onSelect.accept(selected);
		}
		close();
	}

}
