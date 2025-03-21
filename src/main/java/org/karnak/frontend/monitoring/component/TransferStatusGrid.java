/*
 * Copyright (c) 2022 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.monitoring.component;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.Objects;
import org.karnak.backend.data.entity.TransferStatusEntity;
import org.karnak.backend.enums.TransferStatusType;
import org.karnak.backend.util.DateFormat;
import org.karnak.frontend.monitoring.TransferStatusDataProvider;
import org.karnak.frontend.util.UIS;
import org.vaadin.klaudeta.PaginatedGrid;

/**
 * Grid for the monitoring view
 */
public class TransferStatusGrid extends PaginatedGrid<TransferStatusEntity, TransferStatusFilter> {

	// Tooltips
	public static final String TOOLTIP_FILTER_BY_ORIGINAL_OR_DEIDENTIFIED_VALUE = "Filter by original or deidentified value";

	public static final String TOOLTIP_FORMAT_DD_MM_YYYY = "Format: DD/MM/YYYY";

	public static final String TOOLTIP_FORMAT_HH_MM = "Format: HH:MM";

	// Filter grid rows
	private TransferStatusFilter transferStatusFilter;

	// DataProvider
	private final TransferStatusDataProvider<TransferStatusEntity> transferStatusDataProvider;

	/**
	 * Constructor
	 * @param transferStatusDataProvider Data provider for transfer status
	 */
	public TransferStatusGrid(TransferStatusDataProvider<TransferStatusEntity> transferStatusDataProvider) {
		super();
		this.transferStatusDataProvider = transferStatusDataProvider;

		// Set size for the grid
		setWidthFull();
		setHeight(90, Unit.PERCENTAGE);

		// Themes of the grid
		addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);

		// Build columns
		// Transfer date
		Column<TransferStatusEntity> transferDateColumn = addColumnTransferDate();
		// Study uid
		Column<TransferStatusEntity> studyUidColumn = addColumnStudyUid();
		// Serie uid
		Column<TransferStatusEntity> serieUidColumn = addColumnSerieUid();
		// Sop Instance uid
		Column<TransferStatusEntity> sopInstanceUidColumn = addColumnSopInstanceUid();
		// Status
		Column<TransferStatusEntity> statusColumn = addColumnStatus();

		// Item Details
		setItemDetailsRenderer(createTransferStatusDetails());

		// Create filters on rows
		createFiltersOnRows(transferDateColumn, studyUidColumn, serieUidColumn, sopInstanceUidColumn, statusColumn);

		// Pagination
		// Sets the max number of items to be rendered on the grid for each page
		setPageSize(33);
		// Sets how many pages should be visible on the pagination before and/or after the
		// current
		// selected page
		setPaginatorSize(2);
	}

	/**
	 * Create the renderer of the transfer status details
	 * @return the renderer created
	 */
	private ComponentRenderer<TransferStatusGridItemDetail, TransferStatusEntity> createTransferStatusDetails() {
		return new ComponentRenderer<>(TransferStatusGridItemDetail::new,
				TransferStatusGridItemDetail::buildDetailsToDisplay);
	}

	/**
	 * Create filters
	 * @param transferDateColumn Transfer Date column
	 * @param studyUidColumn Study Uid Column
	 * @param serieUidColumn Serie Uid Column
	 * @param sopInstanceUidColumn Sop Instance Uid Column
	 * @param statusColumn Status Column
	 */
	private void createFiltersOnRows(Column<TransferStatusEntity> transferDateColumn,
			Column<TransferStatusEntity> studyUidColumn, Column<TransferStatusEntity> serieUidColumn,
			Column<TransferStatusEntity> sopInstanceUidColumn, Column<TransferStatusEntity> statusColumn) {
		// Filters
		HeaderRow filterRow = appendHeaderRow();
		transferStatusFilter = new TransferStatusFilter();

		// Transfer date filter
		createTransferDateFilter(transferDateColumn, filterRow);
		// Study uid filter
		createStudyUidFilter(studyUidColumn, filterRow);
		// Serie uid filter
		createSerieUidFilter(serieUidColumn, filterRow);
		// Sop Instance uid filter
		createSopInstanceUidFilter(sopInstanceUidColumn, filterRow);
		// Sent status
		createHasBeenSentFilter(statusColumn, filterRow);
	}

	/**
	 * Creation of the filter for transfer date
	 * @param transferDateColumn Column
	 * @param filterRow Row filter
	 */
	private void createTransferDateFilter(Column<TransferStatusEntity> transferDateColumn, HeaderRow filterRow) {
		// Date
		DatePicker datePicker = new DatePicker();
		datePicker.setLocale(Locale.FRANCE);
		UIS.setTooltip(datePicker, TOOLTIP_FORMAT_DD_MM_YYYY);
		datePicker.setPlaceholder("Date");
		datePicker.setMinWidth(30, Unit.PERCENTAGE);
		datePicker.setMaxWidth(40, Unit.PERCENTAGE);
		datePicker.setClearButtonVisible(true);

		// Time Start
		TimePicker startTimePicker = new TimePicker();
		startTimePicker.setPlaceholder("Start");
		UIS.setTooltip(startTimePicker, TOOLTIP_FORMAT_HH_MM);
		startTimePicker.setMinWidth(25, Unit.PERCENTAGE);
		startTimePicker.setMaxWidth(30, Unit.PERCENTAGE);
		startTimePicker.setClearButtonVisible(true);

		// Time End
		TimePicker endTimePicker = new TimePicker();
		endTimePicker.setPlaceholder("End");
		UIS.setTooltip(endTimePicker, TOOLTIP_FORMAT_HH_MM);
		endTimePicker.setMinWidth(25, Unit.PERCENTAGE);
		endTimePicker.setMaxWidth(30, Unit.PERCENTAGE);
		endTimePicker.setClearButtonVisible(true);

		// Listeners
		buildListenerDate(datePicker, startTimePicker, endTimePicker);
		buildListenerStartTime(datePicker, startTimePicker, endTimePicker);
		buildListenerEndTime(datePicker, startTimePicker, endTimePicker);

		// Add components
		HorizontalLayout horizontalLayout = new HorizontalLayout();
		horizontalLayout.add(datePicker, startTimePicker, endTimePicker);
		horizontalLayout.setSizeFull();
		filterRow.getCell(transferDateColumn).setComponent(horizontalLayout);
	}

	/**
	 * Listener end time
	 * @param datePicker Date
	 * @param startTimePicker Start time
	 * @param endTimePicker End time
	 */
	private void buildListenerEndTime(DatePicker datePicker, TimePicker startTimePicker, TimePicker endTimePicker) {
		endTimePicker.addValueChangeListener(event -> handleEventDateTime(datePicker, startTimePicker, endTimePicker));
	}

	/**
	 * Listener start time
	 * @param datePicker Date
	 * @param startTimePicker Start time
	 * @param endTimePicker End time
	 */
	private void buildListenerStartTime(DatePicker datePicker, TimePicker startTimePicker, TimePicker endTimePicker) {
		startTimePicker
			.addValueChangeListener(event -> handleEventDateTime(datePicker, startTimePicker, endTimePicker));
	}

	/**
	 * Listener date
	 * @param datePicker Date
	 * @param startTimePicker Start time
	 * @param endTimePicker End time
	 */
	private void buildListenerDate(DatePicker datePicker, TimePicker startTimePicker, TimePicker endTimePicker) {
		datePicker.addValueChangeListener(event -> handleEventDateTime(datePicker, startTimePicker, endTimePicker));
	}

	/**
	 * Handle event on date time pickers
	 * @param datePicker Date
	 * @param startTimePicker Start time
	 * @param endTimePicker End time
	 */
	private void handleEventDateTime(DatePicker datePicker, TimePicker startTimePicker, TimePicker endTimePicker) {
		// Check invalid combinations
		checkDateTimePickerInvalid(startTimePicker, endTimePicker);

		// Launch filter if pickers are valid
		if (!datePicker.isInvalid() && !startTimePicker.isInvalid() && !endTimePicker.isInvalid()) {
			// Add filter values
			addFilterDateTimeValues(datePicker, startTimePicker, endTimePicker);
			// Filter
			transferStatusDataProvider.refreshAll();
		}
	}

	/**
	 * Add date time values in filter
	 * @param datePicker Date
	 * @param startTimePicker Start time
	 * @param endTimePicker End time
	 */
	private void addFilterDateTimeValues(DatePicker datePicker, TimePicker startTimePicker, TimePicker endTimePicker) {
		if (datePicker.getValue() != null) {
			// Start time
			transferStatusFilter
				.setStart(startTimePicker.getValue() == null ? LocalDateTime.of(datePicker.getValue(), LocalTime.MIN)
						: LocalDateTime.of(datePicker.getValue(), startTimePicker.getValue()));

			// End time
			transferStatusFilter
				.setEnd(endTimePicker.getValue() == null ? LocalDateTime.of(datePicker.getValue(), LocalTime.MAX)
						: LocalDateTime.of(datePicker.getValue(), endTimePicker.getValue()));

		}
		else {
			transferStatusFilter.setStart(null);
			transferStatusFilter.setEnd(null);
			startTimePicker.setValue(null);
			endTimePicker.setValue(null);
		}
	}

	/**
	 * Check invalid combinations
	 * @param startTimePicker StartTimePicker
	 * @param endTimePicker EndTimePicker
	 */
	private void checkDateTimePickerInvalid(TimePicker startTimePicker, TimePicker endTimePicker) {
		if (startTimePicker.getValue() != null && endTimePicker.getValue() != null
				&& (Objects.equals(startTimePicker.getValue(), endTimePicker.getValue())
						|| startTimePicker.getValue().isAfter(endTimePicker.getValue()))) {
			startTimePicker.setInvalid(true);
			endTimePicker.setInvalid(true);
		}
		else {
			startTimePicker.setInvalid(false);
			endTimePicker.setInvalid(false);
		}
	}

	/**
	 * Creation of the filter for study uid
	 * @param studyUidColumn Column
	 * @param filterRow Row filter
	 */
	private void createStudyUidFilter(Column<TransferStatusEntity> studyUidColumn, HeaderRow filterRow) {
		TextField studyUidField = new TextField();
		studyUidField.addValueChangeListener(event -> {
			transferStatusFilter.setStudyUid(event.getValue());
			transferStatusDataProvider.refreshAll();
		});
		studyUidField.setValueChangeMode(ValueChangeMode.EAGER);
		filterRow.getCell(studyUidColumn).setComponent(studyUidField);
		studyUidField.setSizeFull();
		studyUidField.setPlaceholder("Filter Study Uid");
		UIS.setTooltip(studyUidField, TOOLTIP_FILTER_BY_ORIGINAL_OR_DEIDENTIFIED_VALUE);
	}

	/**
	 * Creation of the filter for serie uid
	 * @param serieUidColumn Column
	 * @param filterRow Row filter
	 */
	private void createSerieUidFilter(Column<TransferStatusEntity> serieUidColumn, HeaderRow filterRow) {
		TextField serieUidField = new TextField();
		serieUidField.addValueChangeListener(event -> {
			transferStatusFilter.setSerieUid(event.getValue());
			transferStatusDataProvider.refreshAll();
		});
		serieUidField.setValueChangeMode(ValueChangeMode.EAGER);
		filterRow.getCell(serieUidColumn).setComponent(serieUidField);
		serieUidField.setSizeFull();
		serieUidField.setPlaceholder("Filter Serie Uid");
		UIS.setTooltip(serieUidField, TOOLTIP_FILTER_BY_ORIGINAL_OR_DEIDENTIFIED_VALUE);
	}

	/**
	 * Creation of the filter for sop instance uid
	 * @param sopInstanceUidColumn Column
	 * @param filterRow Row filter
	 */
	private void createSopInstanceUidFilter(Column<TransferStatusEntity> sopInstanceUidColumn, HeaderRow filterRow) {
		TextField sopInstanceUidField = new TextField();
		sopInstanceUidField.addValueChangeListener(event -> {
			transferStatusFilter.setSopInstanceUid(event.getValue());
			transferStatusDataProvider.refreshAll();
		});
		sopInstanceUidField.setValueChangeMode(ValueChangeMode.EAGER);
		filterRow.getCell(sopInstanceUidColumn).setComponent(sopInstanceUidField);
		sopInstanceUidField.setSizeFull();
		sopInstanceUidField.setPlaceholder("Filter Sop Instance Uid");
		UIS.setTooltip(sopInstanceUidField, TOOLTIP_FILTER_BY_ORIGINAL_OR_DEIDENTIFIED_VALUE);
	}

	/**
	 * Creation of the filter for status
	 * @param statusColumn Column
	 * @param filterRow Row filter
	 */
	private void createHasBeenSentFilter(Column<TransferStatusEntity> statusColumn, HeaderRow filterRow) {
		ComboBox<TransferStatusType> statusComboBox = new ComboBox<>();
		statusComboBox.setItemLabelGenerator(TransferStatusType::getLabel);
		statusComboBox.setPlaceholder("Filter Status");
		statusComboBox.setItems(TransferStatusType.values());
		statusComboBox.addValueChangeListener(event -> {
			transferStatusFilter.setTransferStatusType(event.getValue());
			transferStatusDataProvider.refreshAll();
		});
		filterRow.getCell(statusColumn).setComponent(statusComboBox);
		statusComboBox.setSizeFull();
	}

	/**
	 * Add column transfer date
	 * @return column built
	 */
	private Column<TransferStatusEntity> addColumnTransferDate() {
		return addColumn(transferStatusEntity -> DateFormat.format(transferStatusEntity.getTransferDate(),
				DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS_SSSSSS_POINT))
			.setHeader("Transfer date")
			.setWidth("20%")
			.setSortable(false)
			.setKey("transferDateColumn");
	}

	/**
	 * Add column Study uid
	 * @return column built
	 */
	private Column<TransferStatusEntity> addColumnStudyUid() {
		return addColumn(transferStatusEntity -> transferStatusEntity.isSent()
				? transferStatusEntity.getStudyUidToSend() : transferStatusEntity.getStudyUidOriginal())
			.setHeader("Study UID")
			.setWidth("20%")
			.setSortable(false)
			.setKey("studyUidColumn");
	}

	/**
	 * Add column Serie uid
	 * @return column built
	 */
	private Column<TransferStatusEntity> addColumnSerieUid() {
		return addColumn(transferStatusEntity -> transferStatusEntity.isSent()
				? transferStatusEntity.getSerieUidToSend() : transferStatusEntity.getSerieUidOriginal())
			.setHeader("Serie UID")
			.setWidth("20%")
			.setSortable(false)
			.setKey("serieUidColumn");
	}

	/**
	 * Add column Sop Instance uid
	 * @return column built
	 */
	private Column<TransferStatusEntity> addColumnSopInstanceUid() {
		return addColumn(transferStatusEntity -> transferStatusEntity.isSent()
				? transferStatusEntity.getSopInstanceUidToSend() : transferStatusEntity.getSopInstanceUidOriginal())
			.setHeader("Sop Instance UID")
			.setWidth("20%")
			.setSortable(false)
			.setKey("sopInstanceUidColumn");
	}

	/**
	 * Add column Status
	 * @return column built
	 */
	private Column<TransferStatusEntity> addColumnStatus() {
		return addColumn(createColumnStatusComponentRenderer()).setHeader("Status")
			.setWidth("20%")
			.setSortable(false)
			.setKey("statusColumn");
	}

	/**
	 * Create the badge to display depending on the success of the transfer
	 * @return badge created
	 */
	private ComponentRenderer<Span, TransferStatusEntity> createColumnStatusComponentRenderer() {
		return new ComponentRenderer<>(Span::new, (span, transferStatusEntity) -> {
			StringBuilder pill = new StringBuilder();
			pill.append("badge primary pill ");
			if (transferStatusEntity.isSent()) {
				pill.append("success");
			}
			else if (transferStatusEntity.isError()) {
				pill.append("error");
			}
			else {
				span.getStyle().setBackgroundColor("#d38900");
			}
			span.getElement().getThemeList().add(pill.toString());
			span.setText(transferStatusEntity.isSent() ? "Sent" : transferStatusEntity.getReason());
		});
	}

	public TransferStatusFilter getTransferStatusFilter() {
		return transferStatusFilter;
	}

	public void setTransferStatusFilter(TransferStatusFilter transferStatusFilter) {
		this.transferStatusFilter = transferStatusFilter;
	}

}
