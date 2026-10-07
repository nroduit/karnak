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

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.karnak.backend.data.entity.TransferStatusEntity;
import org.karnak.backend.enums.DestinationType;
import org.karnak.backend.util.DateFormat;

public class TransferStatusGridItemDetail extends FormLayout {

	// Transfer Status
	// Modality
	private TextField modalityField;

	// Sop Class Uid
	private TextField sopClassUidField;

	// Original
	private TextField patientIdOriginalField;

	private TextField accessionNumberOriginalField;

	private TextField studyDescriptionOriginalField;

	private TextField studyDateOriginalField;

	private TextField studyUidOriginalField;

	private TextField serieDescriptionOriginalField;

	private TextField serieDateOriginalField;

	private TextField serieUidOriginalField;

	private TextField sopInstanceUidOriginalField;

	// To send
	private TextField patientIdToSendField;

	private TextField accessionNumberToSendField;

	private TextField studyDescriptionToSendField;

	private TextField studyDateToSendField;

	private TextField studyUidToSendField;

	private TextField serieDescriptionToSendField;

	private TextField serieDateToSendField;

	private TextField serieUidToSendField;

	private TextField sopInstanceUidToSendField;

	// Destination
	private TextField destinationUrlField;

	private TextField destinationHostNameField;

	private TextField destinationAeTitleField;

	private TextField destinationPortField;

	private TextField destinationDescriptionField;

	// Forward Node
	private TextField forwardNodeAeTitleField;

	private TextField forwardNodeDescriptionField;

	/**
	 * Constructor
	 */
	public TransferStatusGridItemDetail() {
		// Transfer Status
		initTextFieldsTransferStatus();

		// Destination
		initTextFieldsDestination();

		// Forward node
		initTextFieldsForwardNode();
	}

	/**
	 * Init destination text fields
	 */
	private void initTextFieldsDestination() {
		destinationUrlField = new TextField("Destination Url");
		destinationHostNameField = new TextField("Destination Host Name");
		destinationAeTitleField = new TextField("Destination AeTitle");
		destinationPortField = new TextField("Destination Port");
		destinationDescriptionField = new TextField("Destination Description");
	}

	/**
	 * Init forward node text fields
	 */
	private void initTextFieldsForwardNode() {
		forwardNodeAeTitleField = new TextField("Forward Node AeTitle");
		forwardNodeDescriptionField = new TextField("Forward Node Description");
	}

	/**
	 * Init transfer status text fields
	 */
	private void initTextFieldsTransferStatus() {
		// Modality
		modalityField = new TextField("Modality");
		// Sop Class Uid
		sopClassUidField = new TextField("Sop Class Uid");
		// Original
		patientIdOriginalField = new TextField("Patient Id Original");
		accessionNumberOriginalField = new TextField("Accession Number Original");
		studyDescriptionOriginalField = new TextField("Study Description Original");
		studyDateOriginalField = new TextField("Study Date Original");
		studyUidOriginalField = new TextField("Study Uid Original");
		serieDescriptionOriginalField = new TextField("Serie Description Original");
		serieDateOriginalField = new TextField("Serie Date Original");
		serieUidOriginalField = new TextField("Serie Uid Original");
		sopInstanceUidOriginalField = new TextField("Sop Instance Uid Original");
		// To Send
		patientIdToSendField = new TextField("Patient Id Sent");
		accessionNumberToSendField = new TextField("Accession Number Sent");
		studyDescriptionToSendField = new TextField("Study Description Sent");
		studyDateToSendField = new TextField("Study Date Sent");
		studyUidToSendField = new TextField("Study Uid Sent");
		serieDescriptionToSendField = new TextField("Serie Description Sent");
		serieDateToSendField = new TextField("Serie Date Sent");
		serieUidToSendField = new TextField("Serie Uid Sent");
		sopInstanceUidToSendField = new TextField("Sop Instance Uid Sent");
	}

	/**
	 * Add texfields in component
	 * @param textFields Textfields to add
	 */
	private void addTextFields(List<TextField> textFields) {
		textFields.forEach(field -> {
			field.setReadOnly(true);
			add(field);
		});
	}

	/**
	 * Determine textfields to display and set values
	 * @param transferStatusEntity values to evaluate
	 */
	public void buildDetailsToDisplay(TransferStatusEntity transferStatusEntity) {
		determineTextFieldsToDisplay(transferStatusEntity);
		setValues(transferStatusEntity);
	}

	/**
	 * Determine texfields to display
	 * @param transferStatusEntity Values to evaluate
	 */
	private void determineTextFieldsToDisplay(TransferStatusEntity transferStatusEntity) {
		// Forward node
		determineTextFieldsToDisplayForwardNode(transferStatusEntity);

		// Destination
		determineTextFieldsToDisplayDestination(transferStatusEntity);

		// Transfer Status
		determineTextFieldsToDisplayTransferStatus(transferStatusEntity);
	}

	/**
	 * Determine texfields to display for destiforward ndoe
	 * @param transferStatusEntity values to evaluate
	 */
	private void determineTextFieldsToDisplayForwardNode(TransferStatusEntity transferStatusEntity) {
		addTextFields(Arrays.asList(forwardNodeAeTitleField, forwardNodeDescriptionField));
	}

	/**
	 * Determine texfields to display for destination
	 * @param transferStatusEntity values to evaluate
	 */
	private void determineTextFieldsToDisplayDestination(TransferStatusEntity transferStatusEntity) {
		addTextFields(
				Objects.equals(transferStatusEntity.getDestinationEntity().getDestinationType(), DestinationType.dicom)
						? Arrays.asList(destinationHostNameField, destinationAeTitleField, destinationPortField,
								destinationDescriptionField)
						: Arrays.asList(destinationUrlField, destinationDescriptionField));
	}

	/**
	 * Determine texfields to display for transfer status
	 * @param transferStatusEntity values to evaluate
	 */
	private void determineTextFieldsToDisplayTransferStatus(TransferStatusEntity transferStatusEntity) {

		// Transfer Status
		// Original
		List<TextField> originalTransferStatusTextFields = Arrays.asList(patientIdOriginalField,
				accessionNumberOriginalField, studyDescriptionOriginalField, studyDateOriginalField,
				studyUidOriginalField, serieDescriptionOriginalField, serieDateOriginalField, serieUidOriginalField,
				sopInstanceUidOriginalField);
		// To Send
		List<TextField> toSendTransferStatusTextFields = Arrays.asList(patientIdToSendField, accessionNumberToSendField,
				studyDescriptionToSendField, studyDateToSendField, studyUidToSendField, serieDescriptionToSendField,
				serieDateToSendField, serieUidToSendField, sopInstanceUidToSendField);

		// Transfer Status
		// Modality and sop class uid
		addTextFields(Arrays.asList(modalityField, sopClassUidField));
		// Sop Class Uid
		// If not sent only original values
		if (!transferStatusEntity.isSent()) {
			addTextFields(originalTransferStatusTextFields);
		}
		// If sent: display original and sent values
		else {
			addTextFields(originalTransferStatusTextFields);
			addTextFields(toSendTransferStatusTextFields);
		}
	}

	/**
	 * Populate values in textfields
	 * @param transferStatusEntity values to populate
	 */
	private void setValues(TransferStatusEntity transferStatusEntity) {
		// Transfer Status
		setValuesTransferStatus(transferStatusEntity);

		// Destination
		setValuesDestination(transferStatusEntity);

		// Forward Node
		setValuesForwardNode(transferStatusEntity);
	}

	/**
	 * Set values for transfer status
	 * @param transferStatusEntity values to populate
	 */
	private void setValuesTransferStatus(TransferStatusEntity transferStatusEntity) {
		// Modality
		modalityField.setValue(checkStringNullValue(transferStatusEntity.getModality()));
		// Sop class Uid
		sopClassUidField.setValue(checkStringNullValue(transferStatusEntity.getSopClassUid()));
		// Original
		patientIdOriginalField.setValue(checkStringNullValue(transferStatusEntity.getPatientIdOriginal()));
		accessionNumberOriginalField.setValue(checkStringNullValue(transferStatusEntity.getAccessionNumberOriginal()));
		studyDescriptionOriginalField
			.setValue(checkStringNullValue(transferStatusEntity.getStudyDescriptionOriginal()));
		studyDateOriginalField
			.setValue(checkStringNullValue(DateFormat.format(transferStatusEntity.getStudyDateOriginal(),
					DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS_SSS_POINT)));
		studyUidOriginalField.setValue(checkStringNullValue(transferStatusEntity.getStudyUidOriginal()));
		serieDescriptionOriginalField
			.setValue(checkStringNullValue(transferStatusEntity.getSerieDescriptionOriginal()));
		serieDateOriginalField
			.setValue(checkStringNullValue(DateFormat.format(transferStatusEntity.getSerieDateOriginal(),
					DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS_SSS_POINT)));
		serieUidOriginalField.setValue(checkStringNullValue(transferStatusEntity.getSerieUidOriginal()));
		sopInstanceUidOriginalField.setValue(checkStringNullValue(transferStatusEntity.getSopInstanceUidOriginal()));
		// Sent
		patientIdToSendField.setValue(checkStringNullValue(transferStatusEntity.getPatientIdToSend()));
		accessionNumberToSendField.setValue(checkStringNullValue(transferStatusEntity.getAccessionNumberToSend()));
		studyDescriptionToSendField.setValue(checkStringNullValue(transferStatusEntity.getStudyDescriptionToSend()));
		studyDateToSendField.setValue(checkStringNullValue(DateFormat.format(transferStatusEntity.getStudyDateToSend(),
				DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS_SSS_POINT)));
		studyUidToSendField.setValue(checkStringNullValue(transferStatusEntity.getStudyUidToSend()));
		serieDescriptionToSendField.setValue(checkStringNullValue(transferStatusEntity.getSerieDescriptionToSend()));
		serieDateToSendField.setValue(checkStringNullValue(DateFormat.format(transferStatusEntity.getSerieDateToSend(),
				DateFormat.FORMAT_DDMMYYYY_SLASH_HHMMSS_2POINTS_SSS_POINT)));
		serieUidToSendField.setValue(checkStringNullValue(transferStatusEntity.getSerieUidToSend()));
		sopInstanceUidToSendField.setValue(checkStringNullValue(transferStatusEntity.getSopInstanceUidToSend()));
	}

	/**
	 * Set values for destination
	 * @param transferStatusEntity values to populate
	 */
	private void setValuesDestination(TransferStatusEntity transferStatusEntity) {
		destinationUrlField.setValue(checkStringNullValue(transferStatusEntity.getDestinationEntity().getUrl()));
		destinationHostNameField
			.setValue(checkStringNullValue(transferStatusEntity.getDestinationEntity().getHostname()));
		destinationAeTitleField
			.setValue(checkStringNullValue(transferStatusEntity.getDestinationEntity().getAeTitle()));
		destinationPortField.setValue(checkIntegerNullValue(transferStatusEntity.getDestinationEntity().getPort()));
		destinationDescriptionField
			.setValue(checkStringNullValue(transferStatusEntity.getDestinationEntity().getDescription()));
	}

	/**
	 * Set values for Forward Node
	 * @param transferStatusEntity values to populate
	 */
	private void setValuesForwardNode(TransferStatusEntity transferStatusEntity) {
		forwardNodeAeTitleField
			.setValue(checkStringNullValue(transferStatusEntity.getForwardNodeEntity().getFwdAeTitle()));
		forwardNodeDescriptionField
			.setValue(checkStringNullValue(transferStatusEntity.getForwardNodeEntity().getFwdDescription()));
	}

	/**
	 * Check value is not null otherwise return empty string
	 * @param valueToEvaluate Value to evaluate
	 * @return if null empty string otherwise value
	 */
	private String checkStringNullValue(String valueToEvaluate) {
		return valueToEvaluate == null ? "" : valueToEvaluate;
	}

	/**
	 * Check value is not null otherwise return empty string
	 * @param valueToEvaluate Value to evaluate
	 * @return if null empty string otherwise value
	 */
	private String checkIntegerNullValue(Integer valueToEvaluate) {
		return valueToEvaluate == null ? "" : valueToEvaluate.toString();
	}

}
