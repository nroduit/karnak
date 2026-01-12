/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.extid;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.StringLengthValidator;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.karnak.backend.cache.Patient;
import org.karnak.backend.data.entity.ProjectEntity;

public class ExternalIDForm extends Div {

	private static final String ERROR_MESSAGE_PATIENT = "Length must be between 1 and 50.";

	private final Binder<Patient> binder;

	@Setter
	private transient ProjectEntity projectEntity;

	private TextField externalIdField;

	private TextField patientIdField;

	private TextField patientFirstNameField;

	private TextField patientLastNameField;

	private TextField issuerOfPatientIdField;

	@Getter
	private Button addPatientButton;

	private Button clearFieldsButton;

	private Div addedPatientLabelDiv;

	public ExternalIDForm() {
		setSizeFull();

		binder = new BeanValidationBinder<>(Patient.class);

		setElements();
		setBinder();

		clearFieldsButton.addClickListener(click -> clearPatientFields());

		// enable/disable update button while editing
		binder.addStatusChangeListener(event -> {
			boolean isValid = !event.hasValidationErrors();
			boolean hasChanges = binder.hasChanges();
			addPatientButton.setEnabled(hasChanges && isValid);
		});

		HorizontalLayout horizontalLayout1 = new HorizontalLayout();
		HorizontalLayout horizontalLayout2 = new HorizontalLayout();
		HorizontalLayout horizontalLayout3 = new HorizontalLayout();
		HorizontalLayout horizontalLayout4 = new HorizontalLayout();
		HorizontalLayout horizontalLayout5 = new HorizontalLayout();
		Div addPatientDiv = new Div();

		horizontalLayout1.setSizeFull();
		horizontalLayout2.setSizeFull();
		horizontalLayout3.setSizeFull();
		horizontalLayout4.setSizeFull();

		horizontalLayout3.add(addedPatientLabelDiv);

		horizontalLayout4.add(externalIdField, patientIdField, patientFirstNameField, patientLastNameField,
				issuerOfPatientIdField);
		horizontalLayout5.add(clearFieldsButton, addPatientButton);

		addPatientDiv.add(horizontalLayout4, horizontalLayout5);
		add(horizontalLayout1, horizontalLayout2, horizontalLayout3, addPatientDiv);
	}

	private void setElements() {
		addedPatientLabelDiv = new Div();
		addedPatientLabelDiv.setText("Add a new patient: ");
		addedPatientLabelDiv.getStyle().set("font-size", "large").set("font-weight", "bolder");

		externalIdField = new TextField("External Pseudonym");
		externalIdField.setWidth("20%");
		externalIdField.setRequired(true);
		patientIdField = new TextField("Patient ID");
		patientIdField.setWidth("20%");
		patientIdField.setRequired(true);

		patientFirstNameField = new TextField("Patient first name");
		patientFirstNameField.setWidth("20%");
		patientLastNameField = new TextField("Patient last name");
		patientLastNameField.setWidth("20%");
		issuerOfPatientIdField = new TextField("Issuer of patient ID");
		issuerOfPatientIdField.setWidth("20%");

		clearFieldsButton = new Button("Clear");

		addPatientButton = new Button("Add patient");
		addPatientButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		addPatientButton.setIcon(VaadinIcon.PLUS_CIRCLE.create());
	}

	public void setBinder() {
		binder.forField(externalIdField)
			.withValidator(StringUtils::isNotBlank, "External Pseudonym is empty")
			.withValidator(new StringLengthValidator(ERROR_MESSAGE_PATIENT, 1, 50))
			.bind("pseudonym");

		binder.forField(patientIdField)
			.withValidator(StringUtils::isNotBlank, "Patient ID is empty")
			.withValidator(new StringLengthValidator(ERROR_MESSAGE_PATIENT, 1, 50))
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
			.bind("issuerOfPatientId");
	}

	public Patient getNewPatient() {
		Patient newPatient = new Patient(externalIdField.getValue(), patientIdField.getValue(),
				patientFirstNameField.getValue(), patientLastNameField.getValue(), issuerOfPatientIdField.getValue(),
				projectEntity.getId());
		binder.validate();
		if (binder.isValid()) {
			binder.readBean(null);
			return newPatient;
		}
		return null;
	}

	public void clearPatientFields() {
		externalIdField.clear();
		patientIdField.clear();
		patientFirstNameField.clear();
		patientLastNameField.clear();
		issuerOfPatientIdField.clear();
		binder.readBean(null);
	}

}
