/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.extid;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import jakarta.annotation.security.RolesAllowed;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.cache.Patient;
import org.karnak.backend.util.PatientClientUtil;
import org.karnak.frontend.MainLayout;
import org.karnak.frontend.component.ButtonFactory;
import org.karnak.frontend.component.NewItemDialog;
import org.karnak.frontend.component.ProjectDropDown;
import org.karnak.frontend.component.WarningConfirmDialog;
import org.springframework.beans.factory.annotation.Autowired;
import org.weasis.core.util.annotations.Generated;

@Route(value = ExternalIDView.ROUTE, layout = MainLayout.class)
@PageTitle("Karnak - External ID")
@Tag("extid-view")
@RolesAllowed({ "user", "admin" })
@Generated()
@NullUnmarked
public class ExternalIDView extends HorizontalLayout {

	public static final String VIEW_NAME = "External pseudonym";

	public static final String ROUTE = "extid";

	private static final String LABEL_CHOOSE_PROJECT = "Choose a project:";

	private static final String LABEL_DISCLAIMER_EXTID = "WARNING: The data that is added to this grid will be stored"
			+ " temporally for a short period of time. If the application restarts, the data will be deleted.";

	private final ProjectDropDown projectDropDown;

	private final ExternalIDGrid externalIDGrid;

	private final ExternalIDForm externalIDForm;

	private UI ui;

	private Upload uploadCsvButton;

	private final Button addPatientButton;

	private final Button deleteAllButton;

	private Div uploadCsvLabelDiv;

	@Autowired
	public ExternalIDView(final ExternalIDLogic externalIDLogic) {
		setSizeFull();
		// Safety net for short viewports: the outer row can scroll if the column below
		// can't
		// shrink enough. It's a separate container from the grid, so it doesn't affect
		// the
		// grid's flex sizing inside the column.
		getStyle().set("overflow-y", "auto");
		externalIDLogic.setExternalIDView(this);

		// Content lives in a full-height column so the result grid can flex-grow into the
		// space left below the controls; the column's own padding provides the page
		// margins.
		VerticalLayout verticalLayout = new VerticalLayout();
		verticalLayout.setSizeFull();

		NativeLabel labelDisclaimer = new NativeLabel(LABEL_DISCLAIMER_EXTID);
		labelDisclaimer.addClassName("karnak-error-text");
		labelDisclaimer.setMinWidth("75%");
		labelDisclaimer.getStyle().set("right", "0px");

		Div labelProject = new Div();
		labelProject.setText(LABEL_CHOOSE_PROJECT);
		labelProject.addClassName("karnak-title");

		setUploadCSVElement();
		projectDropDown = new ProjectDropDown();
		projectDropDown.setWidth("50%");
		projectDropDown.setItems(externalIDLogic.retrieveProject());
		externalIDGrid = new ExternalIDGrid();
		externalIDForm = new ExternalIDForm();
		addPatientButton = ButtonFactory.createAddButton("Add patient");
		deleteAllButton = new Button("Delete all patients");

		// The patient fields live in a popup (like the project / profile "new item"
		// flows);
		// the toolbar only shows the "Add patient" button that opens it.
		NewItemDialog addPatientDialog = new NewItemDialog("Add patient", "Add", externalIDForm);
		addPatientDialog.setOnConfirm(() -> {
			final Patient newPatient = externalIDForm.getNewPatient();
			if (newPatient == null) {
				return false;
			}
			externalIDGrid.addPatient(newPatient);
			checkDuplicatePatient();
			externalIDGrid.readAllCacheValue();
			return true;
		});
		addPatientButton.addClickListener(click -> {
			externalIDForm.clearPatientFields();
			addPatientDialog.open();
			externalIDForm.getExternalIdField().focus();
		});

		projectDropDown.addValueChangeListener(event -> {
			setEnableAddPatient(!projectDropDown.isEmpty());
			setEnableDeleteButtons(!projectDropDown.isEmpty());
			externalIDForm.setProjectEntity(event.getValue());
			externalIDGrid.setProjectEntity(event.getValue());
			externalIDGrid.readAllCacheValue();
		});
		setEnableAddPatient(!projectDropDown.isEmpty());
		setEnableDeleteButtons(!projectDropDown.isEmpty());

		deleteAllButton.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.PRIMARY);
		deleteAllButton.addClickListener(e -> {
			Div dialogContent = new Div();
			dialogContent.add(new Text("You are about to delete all the patients below. Are you sure ?"));
			WarningConfirmDialog dialog = new WarningConfirmDialog(dialogContent);
			dialog.addConfirmationListener(componentEvent -> {
				Long projectId = projectDropDown.getValue().getId();
				for (Patient p : externalIDGrid.getPatientsListInCache()) {
					externalIDGrid.getExternalIDCache().remove(PatientClientUtil.generateKey(p, projectId));
				}
				externalIDGrid.readAllCacheValue();
			});
			dialog.open();
		});

		externalIDGrid.getEditor().addOpenListener(editorOpenEvent -> {
			addPatientButton.setEnabled(false);
			uploadCsvButton.setMaxFiles(0);
		});

		externalIDGrid.getEditor().addCloseListener(editorOpenEvent -> {
			addPatientButton.setEnabled(true);
			uploadCsvButton.setMaxFiles(1);
		});

		Div validationStatus = externalIDGrid.setBinder();

		// Toolbar sitting right above the result grid: create a patient (popup) or clear
		// all.
		HorizontalLayout gridToolbar = new HorizontalLayout(addPatientButton, deleteAllButton);
		gridToolbar.setPadding(false);

		verticalLayout.add(new H2("External Pseudonym"), labelDisclaimer, labelProject, projectDropDown,
				uploadCsvLabelDiv, uploadCsvButton, gridToolbar, validationStatus, externalIDGrid);

		// The grid (with its filters) takes all vertical space left below the controls
		// and
		// scrolls its own rows; the floor keeps it usable when the controls leave little
		// room.
		verticalLayout.setFlexGrow(1, externalIDGrid);
		externalIDGrid.setMinHeight("20rem");

		add(verticalLayout);

		addAttachListener(event -> this.ui = event.getUI());
	}

	public void setUploadCSVElement() {
		uploadCsvLabelDiv = new Div();
		uploadCsvLabelDiv.setText("Upload the CSV file containing the external ID associated with patient(s): ");
		uploadCsvLabelDiv.addClassName("karnak-title");

		// Buffer the whole upload in memory: the request-bound input stream is only valid
		// during the upload handler, but the CSV is parsed later (after the user picks a
		// separator), so we keep the bytes and re-wrap them in a fresh stream each time.
		uploadCsvButton = new Upload(UploadHandler.inMemory((metadata, data) -> ui.access(() -> {
			Dialog chooseSeparatorDialog = new Dialog();
			TextField separatorCSVField = new TextField("Choose the separator for reading the CSV file");
			separatorCSVField.setWidthFull();
			separatorCSVField.setMaxLength(1);
			separatorCSVField.setValue(",");
			Button openCSVButton = new Button("Open CSV");

			openCSVButton.addClickListener(buttonClickEvent -> {
				chooseSeparatorDialog.close();
				char separator = ',';
				if (!separatorCSVField.getValue().isEmpty()) {
					separator = separatorCSVField.getValue().charAt(0);
				}
				CSVDialog csvDialog = new CSVDialog(new ByteArrayInputStream(data), separator,
						projectDropDown.getValue());
				csvDialog.setWidth("80%");
				csvDialog.open();

				csvDialog.getReadCSVButton().addClickListener(buttonClickEvent1 -> {
					externalIDGrid.addPatientList(csvDialog.getPatientsList());
					checkDuplicatePatient();
					csvDialog.resetPatientsList();
				});
			});

			chooseSeparatorDialog.add(separatorCSVField, openCSVButton);
			chooseSeparatorDialog.open();
			separatorCSVField.focus();
		})));
		uploadCsvButton.setDropLabel(new Span("Drag and drop your CSV file here"));
	}

	public void checkDuplicatePatient() {
		if (!externalIDGrid.getDuplicatePatientsList().isEmpty()) {
			DuplicateDialog duplicateDialog = new DuplicateDialog("WARNING Duplicate data",
					"You are trying to insert two equivalent patients. Here is the list of duplicate patients.",
					externalIDGrid.getDuplicatePatientsList(), "Close");
			duplicateDialog.setWidth("80%");
			duplicateDialog.open();
			externalIDGrid.setDuplicatePatientsList(new ArrayList<>());
		}
	}

	public void setEnableAddPatient(boolean value) {
		addPatientButton.setEnabled(value);
		if (value) {
			uploadCsvButton.setMaxFiles(1);
		}
		else {
			uploadCsvButton.setMaxFiles(0);
		}
	}

	public void setEnableDeleteButtons(boolean value) {
		deleteAllButton.setEnabled(value);
		externalIDGrid.setEnabledDeleteSelectedPatientsButton(value);
	}

}
