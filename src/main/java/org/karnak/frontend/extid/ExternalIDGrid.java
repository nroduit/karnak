/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.extid;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.karnak.backend.cache.Patient;
import org.karnak.backend.cache.PatientClient;
import org.karnak.backend.config.AppConfig;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.util.PatientClientUtil;
import org.karnak.frontend.component.WarningConfirmDialog;
import org.vaadin.klaudeta.PaginatedGrid;

public class ExternalIDGrid extends PaginatedGrid<Patient, PatientFilter> {

	private static final String ERROR_MESSAGE_PATIENT = "Length must be between 1 and 50.";

	private static final String LABEL_SAVE = "Save";

	private static final String LABEL_CANCEL = "Cancel";

	private static final String LABEL_FILTER = "Filter";

	private final Binder<Patient> binder;

	@Getter
	@Setter
	private transient PatientClient externalIDCache;

	@Getter
	@Setter
	private transient ProjectEntity projectEntity;

	private Button deletePatientButton;

	private Button deleteAllSelectedPatientsButton;

	private List<Patient> selectedPatients;

	private Button saveEditPatientButton;

	private Button cancelEditPatientButton;

	private Editor<Patient> editor;

	private Collection<Button> editButtons;

	private TextField externalIdField;

	private TextField patientIdField;

	private TextField patientFirstNameField;

	private TextField patientLastNameField;

	private TextField issuerOfPatientIdField;

	private Grid.Column<Patient> deleteColumn;

	private Grid.Column<Patient> extidColumn;

	private Grid.Column<Patient> patientIdColumn;

	private Grid.Column<Patient> patientFirstNameColumn;

	private Grid.Column<Patient> patientLastNameColumn;

	private Grid.Column<Patient> issuerOfPatientIDColumn;

	private TextField patientIdFilter;

	private TextField extidFilter;

	private TextField patientFirstNameFilter;

	private TextField patientLastNameFilter;

	private TextField issuerOfPatientIDFilter;

	@Getter
	private List<Patient> patientsListInCache = new ArrayList<>();

	@Getter
	@Setter
	private transient Collection<Patient> duplicatePatientsList = new ArrayList<>();

	public ExternalIDGrid() {
		binder = new Binder<>(Patient.class);
		List<Patient> patientList = new ArrayList<>();
		this.externalIDCache = AppConfig.getInstance().getExternalIDCache();
		// TODO: to use instead of the current multiple filters..
		PatientFilter patientFilter = new PatientFilter();

		setSelectionMode(Grid.SelectionMode.MULTI);

		setPageSize(10);
		setPaginatorSize(2);

		setSizeFull();
		getElement().addEventListener("keyup", event -> editor.cancel())
			.setFilter("event.key === 'Escape' || event.key === 'Esc'");

		setItems(patientList);
		setElements();
		setBinder();
		readAllCacheValue();
		editor.addOpenListener(e -> {
			editButtons.forEach(button -> button.setEnabled(!editor.isOpen()));
			deleteColumn.setVisible(false);
		});

		editor.addCloseListener(e -> {
			editButtons.forEach(button -> button.setEnabled(!editor.isOpen()));
			deleteColumn.setVisible(true);
		});

		saveEditPatientButton.addClickListener(e -> {
			final Patient patientEdit = new Patient(externalIdField.getValue(), patientIdField.getValue(),
					patientFirstNameField.getValue(), patientLastNameField.getValue(),
					issuerOfPatientIdField.getValue(), projectEntity.getId());
			externalIDCache.remove(PatientClientUtil.generateKey(editor.getItem(), projectEntity.getId()));
			externalIDCache.put(PatientClientUtil.generateKey(patientEdit, projectEntity.getId()), patientEdit);
			editor.save();
		});
		saveEditPatientButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		cancelEditPatientButton.addClickListener(e -> editor.cancel());
	}

	private void setElements() {
		extidColumn = addColumn(Patient::getPseudonym).setHeader("External Pseudonym").setSortable(true);
		patientIdColumn = addColumn(Patient::getPatientId).setHeader("Patient ID").setSortable(true);
		patientFirstNameColumn = addColumn(Patient::getPatientFirstName).setHeader("Patient first name")
			.setSortable(true);
		patientLastNameColumn = addColumn(Patient::getPatientLastName).setHeader("Patient last name").setSortable(true);
		issuerOfPatientIDColumn = addColumn(Patient::getIssuerOfPatientId).setHeader("Issuer of patient ID")
			.setSortable(true);
		Grid.Column<Patient> editorColumn = addComponentColumn(patient -> {
			Button edit = new Button("Edit");
			edit.addClassName("edit");
			edit.addClickListener(e -> {
				editor.editItem(patient);
				externalIdField.focus();
			});
			edit.setEnabled(!editor.isOpen());
			editButtons.add(edit);
			return edit;
		});

		addFilterElements();

		editButtons = Collections.newSetFromMap(new WeakHashMap<>());
		editor = getEditor();
		editor.setBinder(binder);
		editor.setBuffered(true);

		externalIdField = new TextField();
		patientIdField = new TextField();
		patientFirstNameField = new TextField();
		patientLastNameField = new TextField();
		issuerOfPatientIdField = new TextField();

		extidColumn.setEditorComponent(externalIdField);
		patientIdColumn.setEditorComponent(patientIdField);
		patientFirstNameColumn.setEditorComponent(patientFirstNameField);
		patientLastNameColumn.setEditorComponent(patientLastNameField);
		issuerOfPatientIDColumn.setEditorComponent(issuerOfPatientIdField);

		deleteAllSelectedPatientsButton = new Button("Delete selected patients");
		deleteAllSelectedPatientsButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
		deleteAllSelectedPatientsButton.addClickListener(e -> {
			if (selectedPatients != null && !selectedPatients.isEmpty()) {
				Div dialogContent = new Div();
				dialogContent.add(new Text(
						"Do you confirm the deletion of the " + selectedPatients.size() + " selected patients ?"));
				WarningConfirmDialog dialog = new WarningConfirmDialog(dialogContent);
				dialog.addConfirmationListener(componentEvent -> {
					for (Patient p : selectedPatients) {
						externalIDCache.remove(PatientClientUtil.generateKey(p, projectEntity.getId()));
					}
					readAllCacheValue();
				});
				dialog.open();
			}
		});

		addSelectionListener(selection -> {
			selectedPatients = new ArrayList<>();
			selectedPatients = selection.getAllSelectedItems().stream().toList();
		});

		deleteColumn = addComponentColumn(patient -> {
			deletePatientButton = new Button("Delete");
			deletePatientButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
			deletePatientButton.addClickListener(e -> {
				externalIDCache.remove(PatientClientUtil.generateKey(patient, projectEntity.getId()));
				readAllCacheValue();
			});
			return deletePatientButton;
		}).setHeader(deleteAllSelectedPatientsButton);

		saveEditPatientButton = new Button(LABEL_SAVE);
		cancelEditPatientButton = new Button(LABEL_CANCEL);
		cancelEditPatientButton.getStyle().set("margin-left", "10px");

		Div buttons = new Div(saveEditPatientButton, cancelEditPatientButton);
		editorColumn.setEditorComponent(buttons);
	}

	public void addFilterElements() {
		HeaderRow filterRow = appendHeaderRow();
		extidFilter = createAndConfigureFilterTextField(filterRow, extidColumn, true);
		patientIdFilter = createAndConfigureFilterTextField(filterRow, patientIdColumn, true);
		patientFirstNameFilter = createAndConfigureFilterTextField(filterRow, patientFirstNameColumn, false);
		patientLastNameFilter = createAndConfigureFilterTextField(filterRow, patientLastNameColumn, false);
		issuerOfPatientIDFilter = createAndConfigureFilterTextField(filterRow, issuerOfPatientIDColumn, false);
	}

	private TextField createAndConfigureFilterTextField(HeaderRow filterRow, Grid.Column<Patient> column,
			boolean required) {
		TextField filterField = new TextField();
		filterField.setRequired(required);
		filterField.addValueChangeListener(event -> checkAndUpdateAllFilters());
		filterField.setValueChangeMode(ValueChangeMode.EAGER);
		filterField.setSizeFull();
		filterField.setPlaceholder(LABEL_FILTER);
		filterRow.getCell(column).setComponent(filterField);
		return filterField;
	}

	public Div setBinder() {
		Div validationStatus = new Div();
		validationStatus.setId("validation");
		validationStatus.getStyle().set("color", "var(--theme-color, red)");
		binder.forField(externalIdField)
			.withValidator(StringUtils::isNotBlank, "External Pseudonym is empty")
			.withValidator(new StringLengthValidator(ERROR_MESSAGE_PATIENT, 1, 50))
			.withStatusLabel(validationStatus)
			.bind("pseudonym");

		binder.forField(patientIdField)
			.withValidator(StringUtils::isNotBlank, "Patient ID is empty")
			.withValidator(new StringLengthValidator(ERROR_MESSAGE_PATIENT, 1, 50))
			.withStatusLabel(validationStatus)
			.bind("patientId");

		String maxLengthMessage = "Length must be between 0 and 50.";
		binder.forField(patientFirstNameField)
			.withValidator(new StringLengthValidator(maxLengthMessage, 0, 50))
			.bind("patientFirstName");

		binder.forField(patientLastNameField)
			.withValidator(new StringLengthValidator(maxLengthMessage, 0, 50))
			.bind("patientLastName");

		binder.forField(issuerOfPatientIdField)
			.withValidator(new StringLengthValidator(maxLengthMessage, 0, 50))
			.withStatusLabel(validationStatus)
			.bind("issuerOfPatientId");

		return validationStatus;
	}

	public void readAllCacheValue() {
		if (externalIDCache != null) {
			Collection<Patient> patients = externalIDCache.getAll();
			patientsListInCache = new ArrayList<>();
			for (final Patient patient : patients) {
				if (projectEntity != null && patient.getProjectID() != null
						&& patient.getProjectID().equals(projectEntity.getId())) {
					patientsListInCache.add(patient);
				}
			}
			setItems(patientsListInCache);
		}
		refreshPaginator();
	}

	public void addPatient(Patient newPatient) {
		if (!patientExist(newPatient)) {
			externalIDCache.put(PatientClientUtil.generateKey(newPatient, projectEntity.getId()), newPatient);
		}
	}

	public void addPatientList(List<Patient> patientList) {
		patientList.forEach(this::addPatient);
		readAllCacheValue();
	}

	public boolean patientExist(Patient patient) {
		final Patient duplicatePatient = externalIDCache
			.get(PatientClientUtil.generateKey(patient, projectEntity.getId()));
		if (duplicatePatient != null) {
			duplicatePatientsList.add(duplicatePatient);
			return true;
		}
		return false;
	}

	public void checkAndUpdateAllFilters() {
		// TODO: replace by PatientFilter which will contains all filters and remove
		// extidFilter, patientIdFilter, patientFirstNameFilter, patientLastNameFilter,
		// issuerOfPatientIDFilter
		List<Patient> filterList = patientsListInCache.stream().toList();

		if (!extidFilter.getValue().isEmpty()) {
			filterList = filterList.stream()
				.filter(cachedPatient -> cachedPatient.getPseudonym().contains(extidFilter.getValue()))
				.toList();
		}

		if (!patientIdFilter.getValue().isEmpty()) {
			filterList = filterList.stream()
				.filter(cachedPatient -> cachedPatient.getPatientId().contains(patientIdFilter.getValue()))
				.toList();
		}

		if (!patientFirstNameFilter.getValue().isEmpty()) {
			filterList = filterList.stream()
				.filter(cachedPatient -> cachedPatient.getPatientFirstName()
					.contains(patientFirstNameFilter.getValue()))
				.toList();
		}

		if (!patientLastNameFilter.getValue().isEmpty()) {
			filterList = filterList.stream()
				.filter(cachedPatient -> cachedPatient.getPatientLastName().contains(patientLastNameFilter.getValue()))
				.toList();
		}

		if (!issuerOfPatientIDFilter.getValue().isEmpty()) {
			filterList = filterList.stream()
				.filter(cachedPatient -> cachedPatient.getIssuerOfPatientId()
					.contains(issuerOfPatientIDFilter.getValue()))
				.toList();
		}

		setItems(filterList);
	}

	public void setEnabledDeleteSelectedPatientsButton(Boolean value) {
		deleteAllSelectedPatientsButton.setEnabled(value);
	}

}
