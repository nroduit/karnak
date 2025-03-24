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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import org.karnak.frontend.util.NotificationUtil;

/**
 * Dialog to get the input of the user concerning the export settings
 */
public class ExportSettingsDialog extends Dialog {

	// Textfields
	private TextField delimiterTextField;

	private TextField quoteCharacterTextField;

	// Export setting model
	private ExportSettings exportSettings;

	// Binder
	private Binder<ExportSettings> binder;

	// Button
	private Button saveButton;

	private Button cancelButton;

	/**
	 * Constructor
	 */
	public ExportSettingsDialog() {
		setModal(true);

		// Build components
		buildComponents();

		// Binder
		buildBinder();

		// Events
		addEvents();
	}

	/**
	 * Add events
	 */
	private void addEvents() {
		// read defaults values
		binder.readBean(exportSettings);

		// Save button
		saveButton.addClickListener(buttonClickEvent -> {
			try {
				binder.writeBean(exportSettings);
				this.close();
			}
			catch (ValidationException e) {
				NotificationUtil.displayErrorMessage("Error occurred during bean validation", Position.BOTTOM_CENTER);
			}
		});

		// Cancel button
		cancelButton.addClickListener(buttonClickEvent -> {
			binder.readBean(exportSettings);
			this.close();
		});
	}

	/**
	 * Binder on textfields
	 */
	private void buildBinder() {
		binder = new Binder<>(ExportSettings.class);
		// Delimiter
		binder.forField(delimiterTextField)
			.withValidator(separator -> separator.length() == 1, "Delimiter must contain only one character")
			.asRequired("Delimiter is required")
			.bind(ExportSettings::getDelimiter, ExportSettings::setDelimiter);
		// Quote character
		binder.forField(quoteCharacterTextField)
			.withValidator(separator -> separator.length() == 1, "Quote character must contain only one character")
			.asRequired("Quote character is required")
			.bind(ExportSettings::getQuoteCharacter, ExportSettings::setQuoteCharacter);
	}

	/**
	 * Build components
	 */
	private void buildComponents() {
		// Default
		exportSettings = new ExportSettings();

		// Title
		HorizontalLayout titleLayout = new HorizontalLayout();
		H2 dialogTitle = new H2("Export Settings");
		dialogTitle.setWidthFull();
		titleLayout.add(dialogTitle);

		// Textfields
		delimiterTextField = new TextField("Delimiter");
		quoteCharacterTextField = new TextField("Quote character");
		delimiterTextField.setWidthFull();
		quoteCharacterTextField.setWidthFull();
		VerticalLayout fieldsLayout = new VerticalLayout();
		fieldsLayout.add(delimiterTextField, quoteCharacterTextField);

		// Buttons
		cancelButton = new Button("Cancel");
		saveButton = new Button("Save settings");
		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, saveButton);
		buttonLayout.setJustifyContentMode(JustifyContentMode.CENTER);
		buttonLayout.setWidthFull();

		// Final layout
		VerticalLayout dialogLayout = new VerticalLayout(titleLayout, fieldsLayout, buttonLayout);
		add(dialogLayout);
	}

	public ExportSettings getExportSettings() {
		return exportSettings;
	}

	public void setExportSettings(ExportSettings exportSettings) {
		this.exportSettings = exportSettings;
	}

}
