/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.extid;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.StringLengthValidator;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.cache.Patient;
import org.karnak.backend.data.entity.ProjectEntity;
import org.weasis.core.util.annotations.Generated;

/**
 * The set of fields describing a new patient/pseudonym mapping. It carries only the
 * inputs and their validation; it is shown as the body of the "Add patient" popup, which
 * supplies its own Add / Cancel buttons.
 */
@Generated()
@NullUnmarked
public class ExternalIDForm extends VerticalLayout {

	private static final String ERROR_MESSAGE_PATIENT = "Length must be between 1 and 50.";

	private final Binder<Patient> binder;

	@Setter
	private transient ProjectEntity projectEntity;

	@Getter
	private TextField externalIdField;

	private TextField patientIdField;

	private TextField patientFirstNameField;

	private TextField patientLastNameField;

	private TextField issuerOfPatientIdField;

	public ExternalIDForm() {
		setPadding(false);
		setSpacing(true);

		binder = new BeanValidationBinder<>(Patient.class);

		setElements();
		setBinder();

		add(externalIdField, patientIdField, patientFirstNameField, patientLastNameField, issuerOfPatientIdField);
	}

	private void setElements() {
		externalIdField = new TextField("External Pseudonym");
		externalIdField.setWidthFull();
		externalIdField.setRequired(true);

		patientIdField = new TextField("Patient ID");
		patientIdField.setWidthFull();
		patientIdField.setRequired(true);

		patientFirstNameField = new TextField("Patient first name");
		patientFirstNameField.setWidthFull();
		patientLastNameField = new TextField("Patient last name");
		patientLastNameField.setWidthFull();
		issuerOfPatientIdField = new TextField("Issuer of patient ID");
		issuerOfPatientIdField.setWidthFull();
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