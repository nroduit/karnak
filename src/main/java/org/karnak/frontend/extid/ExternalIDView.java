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

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
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
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import java.io.InputStream;
import java.util.ArrayList;
import org.karnak.backend.cache.Patient;
import org.karnak.backend.util.PatientClientUtil;
import org.karnak.frontend.MainLayout;
import org.karnak.frontend.component.ProjectDropDown;
import org.karnak.frontend.component.WarningConfirmDialog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;

@Route(value = ExternalIDView.ROUTE, layout = MainLayout.class)
@PageTitle("KARNAK - External ID")
@Tag("extid-view")
@Secured({ "ROLE_user", "ROLE_admin" })
@SuppressWarnings("serial")
public class ExternalIDView extends HorizontalLayout {

	public static final String VIEW_NAME = "External pseudonym";

	public static final String ROUTE = "extid";

	private static final String LABEL_CHOOSE_PROJECT = "Choose a project:";

	private static final String LABEL_DISCLAIMER_EXTID = "WARNING: The data that is added to this grid will be stored"
			+ " temporally for a short period of time. If the machine restarts, the data will be deleted.";

	private final ProjectDropDown projectDropDown;

	private final ExternalIDGrid externalIDGrid;

	private final Div validationStatus;

	private final ExternalIDForm externalIDForm;

	private transient InputStream inputStream;

	private Upload uploadCsvButton;

	private final Button deleteAllButton;

	private Div uploadCsvLabelDiv;

	private final transient ExternalIDLogic externalIDLogic;

	@Autowired
	public ExternalIDView(final ExternalIDLogic externalIDLogic) {
		setSizeFull();
		this.externalIDLogic = externalIDLogic;
		this.externalIDLogic.setExternalIDView(this);
		getStyle().set("overflow-y", "auto");
		VerticalLayout verticalLayout = new VerticalLayout();

		NativeLabel labelDisclaimer = new NativeLabel(LABEL_DISCLAIMER_EXTID);
		labelDisclaimer.getStyle().set("color", "red");
		labelDisclaimer.setMinWidth("75%");
		labelDisclaimer.getStyle().set("right", "0px");

		Div labelProject = new Div();
		labelProject.setText(LABEL_CHOOSE_PROJECT);
		labelProject.getStyle().set("font-size", "large").set("font-weight", "bolder");

		setUploadCSVElement();
		projectDropDown = new ProjectDropDown();
		projectDropDown.setWidth("50%");
		projectDropDown.setItems(externalIDLogic.retrieveProject());
		externalIDGrid = new ExternalIDGrid();
		externalIDForm = new ExternalIDForm();
		deleteAllButton = new Button("Delete all patients");

		projectDropDown.addValueChangeListener(event -> {
			setEnableAddPatient(!projectDropDown.isEmpty());
			setEnableDeleteButtons(!projectDropDown.isEmpty());
			externalIDForm.setProjectEntity(event.getValue());
			externalIDGrid.setProjectEntity(event.getValue());
			externalIDGrid.readAllCacheValue();
		});
		setEnableAddPatient(!projectDropDown.isEmpty());
		setEnableDeleteButtons(!projectDropDown.isEmpty());

		deleteAllButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
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

		externalIDForm.getAddPatientButton().addClickListener(click -> {
			final Patient newPatient = externalIDForm.getNewPatient();
			if (newPatient != null) {
				externalIDGrid.addPatient(newPatient);
				checkDuplicatePatient();
				externalIDGrid.readAllCacheValue();
			}
		});

		externalIDGrid.getEditor().addOpenListener(editorOpenEvent -> {
			externalIDForm.setEnabled(false);
			uploadCsvButton.setMaxFiles(0);
		});

		externalIDGrid.getEditor().addCloseListener(editorOpenEvent -> {
			externalIDForm.setEnabled(true);
			uploadCsvButton.setMaxFiles(1);
		});

		validationStatus = externalIDGrid.setBinder();

		verticalLayout.add(new H2("External Pseudonym"), labelDisclaimer, labelProject, projectDropDown,
				uploadCsvLabelDiv, uploadCsvButton, externalIDForm, deleteAllButton, validationStatus, externalIDGrid);

		add(verticalLayout);
	}

	public void setUploadCSVElement() {
		uploadCsvLabelDiv = new Div();
		uploadCsvLabelDiv.setText("Upload the CSV file containing the external ID associated with patient(s): ");
		uploadCsvLabelDiv.getStyle().set("font-size", "large").set("font-weight", "bolder");
		MemoryBuffer memoryBuffer = new MemoryBuffer();
		uploadCsvButton = new Upload(memoryBuffer);
		uploadCsvButton.setDropLabel(new Span("Drag and drop your CSV file here"));
		uploadCsvButton.addSucceededListener(event -> {
			inputStream = memoryBuffer.getInputStream();

			Dialog chooseSeparatorDialog = new Dialog();
			TextField separatorCSVField = new TextField("Choose the separator for reading the CSV file");
			separatorCSVField.setWidthFull();
			separatorCSVField.setMaxLength(1);
			separatorCSVField.setValue(",");
			Button openCSVButton = new Button("Open CSV");

			openCSVButton.addClickListener(buttonClickEvent -> {
				chooseSeparatorDialog.close();
				char separator = ',';
				if (!separatorCSVField.getValue().equals("")) {
					separator = separatorCSVField.getValue().charAt(0);
				}
				CSVDialog csvDialog = new CSVDialog(inputStream, separator, projectDropDown.getValue());
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
		});
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
		externalIDForm.setEnabled(value);
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
