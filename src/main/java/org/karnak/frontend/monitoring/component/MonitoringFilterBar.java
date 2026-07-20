/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.monitoring.component;

import com.vaadin.componentfactory.DateRange;
import com.vaadin.componentfactory.EnhancedDateRangePicker;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.karnak.backend.enums.TransferStatusType;
import org.weasis.core.util.annotations.Generated;

/**
 * Filter toolbar for the monitoring view: a single-calendar
 * {@link EnhancedDateRangePicker} for custom (day-granular) ranges plus quick presets
 * that keep hour-level precision (Last hour / 24h …), a status selector and the
 * study/series/SOP text filters. All controls feed the same {@link TransferStatusFilter};
 * any change triggers the supplied {@code onChange} callback so the tree and the
 * dashboard reload.
 */
// The EnhancedDateRangePicker add-on ships only a Lumo theme. Under Aura (which
// defines no --lumo-* variables) it renders unstyled, so we supply the --lumo-*
// tokens it needs plus Lumo's icon font. See lumo-compat.css. The picker also
// positions its calendar toggle assuming Lumo's positioned input container, so a
// shadow-DOM fix (themeFor) keeps that toggle inside the field under Aura.
@CssImport("@vaadin/vaadin-lumo-styles/src/props/icons.css")
@CssImport("./themes/common-theme/lumo-compat.css")
@CssImport(value = "./themes/common-theme/components/vcf-date-range-picker.css", themeFor = "vcf-date-range-picker")
@Generated()
public class MonitoringFilterBar extends HorizontalLayout {

	/** Quick date-range presets. */
	public enum RangePreset {

		LAST_5_MIN("Last 5 minutes"), LAST_15_MIN("Last 15 minutes"), LAST_HOUR("Last hour"), LAST_24H("Last 24 hours"),
		TODAY("Today"), LAST_7_DAYS("Last 7 days"), LAST_15_DAYS("Last 15 days"), ALL("All"), CUSTOM("Custom");

		private final String label;

		RangePreset(String label) {
			this.label = label;
		}

		@Override
		public String toString() {
			return label;
		}

	}

	@Getter
	private final transient TransferStatusFilter filter = new TransferStatusFilter();

	private final transient Runnable onChange;

	private final ComboBox<RangePreset> presetComboBox = new ComboBox<>("Range");

	private final EnhancedDateRangePicker rangePicker = new EnhancedDateRangePicker("Custom range");

	private boolean updating;

	public MonitoringFilterBar(Runnable onChange) {
		this.onChange = onChange;

		presetComboBox.setItems(RangePreset.values());
		presetComboBox.addValueChangeListener(event -> {
			if (!updating && event.getValue() != null && event.getValue() != RangePreset.CUSTOM) {
				applyPreset(event.getValue());
				fireChange();
			}
		});

		rangePicker.setClearButtonVisible(true);
		rangePicker.addValueChangeListener(event -> onRangeEdited());

		ComboBox<TransferStatusType> statusComboBox = new ComboBox<>("Status");
		statusComboBox.setItems(TransferStatusType.values());
		statusComboBox.setItemLabelGenerator(TransferStatusType::getLabel);
		statusComboBox.setValue(TransferStatusType.ALL);
		statusComboBox.addValueChangeListener(event -> {
			filter.setTransferStatusType(event.getValue() == null ? TransferStatusType.ALL : event.getValue());
			fireChange();
		});

		TextField studyUidField = new TextField("Study UID");
		configureTextFilter(studyUidField, filter::setStudyUid);
		TextField serieUidField = new TextField("Series UID");
		configureTextFilter(serieUidField, filter::setSerieUid);

		setAlignItems(Alignment.BASELINE);
		setSpacing(true);
		setWidthFull();
		add(presetComboBox, rangePicker, statusComboBox, studyUidField, serieUidField);
		// Let the UID filters take the space freed by the compact range picker
		setFlexGrow(1, studyUidField, serieUidField);

		// Default to recent activity
		updating = true;
		presetComboBox.setValue(RangePreset.LAST_24H);
		updating = false;
		applyPreset(RangePreset.LAST_24H);
	}

	/**
	 * Re-evaluate the current relative preset so its window ends at the present instant.
	 * Relative presets (Last 5 minutes, Last 24 hours, …) freeze their [start, end]
	 * window when first applied, so a plain reload keeps querying the original, now-stale
	 * range and never shows activity that arrived since. The monitoring Refresh button
	 * calls this first so the range advances to "now". Custom ranges are explicit user
	 * input and left untouched.
	 */
	public void refreshRange() {
		RangePreset preset = presetComboBox.getValue();
		if (preset != null && preset != RangePreset.CUSTOM) {
			applyPreset(preset);
		}
	}

	private void configureTextFilter(TextField field, java.util.function.Consumer<String> setter) {
		field.setClearButtonVisible(true);
		field.setMinWidth("16em");
		field.setValueChangeMode(ValueChangeMode.LAZY);
		field.addValueChangeListener(event -> {
			setter.accept(StringUtils.trimToEmpty(event.getValue()));
			fireChange();
		});
	}

	private void onRangeEdited() {
		if (updating) {
			return;
		}
		// Manual edit switches the preset to Custom; the date-only range maps to whole
		// days
		updating = true;
		presetComboBox.setValue(RangePreset.CUSTOM);
		updating = false;
		DateRange range = rangePicker.getValue();
		LocalDate start = range == null ? null : range.getStartDate();
		LocalDate end = range == null ? null : range.getEndDate();
		filter.setStart(start == null ? null : start.atStartOfDay());
		filter.setEnd(end == null ? null : end.atTime(LocalTime.MAX));
		fireChange();
	}

	private void applyPreset(RangePreset preset) {
		LocalDateTime now = LocalDateTime.now(ZoneId.of("CET"));
		LocalDateTime start;
		LocalDateTime end;
		switch (preset) {
			case LAST_5_MIN -> {
				start = now.minusMinutes(5);
				end = now;
			}
			case LAST_15_MIN -> {
				start = now.minusMinutes(15);
				end = now;
			}
			case LAST_HOUR -> {
				start = now.minusHours(1);
				end = now;
			}
			case LAST_24H -> {
				start = now.minusHours(24);
				end = now;
			}
			case TODAY -> {
				start = now.toLocalDate().atStartOfDay();
				end = now;
			}
			case LAST_7_DAYS -> {
				start = now.minusDays(7);
				end = now;
			}
			case LAST_15_DAYS -> {
				start = now.minusDays(15);
				end = now;
			}
			default -> {
				start = null;
				end = null;
			}
		}
		updating = true;
		if (start == null || end == null) {
			rangePicker.clear();
		}
		else {
			rangePicker.setValue(new DateRange(start.toLocalDate(), end.toLocalDate()));
		}
		updating = false;
		filter.setStart(start);
		filter.setEnd(end);
	}

	private void fireChange() {
		if (onChange != null) {
			onChange.run();
		}
	}

}
