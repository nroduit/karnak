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

import com.vaadin.flow.component.ModalityMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import java.io.ByteArrayInputStream;
import lombok.Getter;
import org.weasis.core.util.annotations.Generated;

/**
 * Export popup that only asks the user for the CSV separator before downloading the
 * export.
 */
@Generated()
public class ExportDialog extends Dialog {

	// Export settings: only the separator is configurable, the quote character keeps its
	// default value
	@Getter
	private final ExportSettings exportSettings = new ExportSettings();

	private TextField separatorTextField;

	private Button downloadButton;

	private Button cancelButton;

	/**
	 * Constructor.
	 * @param csvSupplier supplies the CSV bytes built with the current
	 * {@link #exportSettings}
	 */
	public ExportDialog(SerializableSupplier<byte[]> csvSupplier) {
		setModality(ModalityMode.STRICT);
		setHeaderTitle("Export CSV");

		buildComponents(csvSupplier);
	}

	private void buildComponents(SerializableSupplier<byte[]> csvSupplier) {
		// Separator field
		separatorTextField = new TextField("Separator");
		separatorTextField.setValue(exportSettings.getDelimiter());
		separatorTextField.setWidthFull();
		separatorTextField.setValueChangeMode(ValueChangeMode.EAGER);
		separatorTextField.addValueChangeListener(event -> {
			String value = event.getValue();
			boolean valid = value != null && value.length() == 1;
			if (valid) {
				exportSettings.setDelimiter(value);
				separatorTextField.setInvalid(false);
			}
			else {
				separatorTextField.setInvalid(true);
				separatorTextField.setErrorMessage("Separator must contain exactly one character");
			}
			downloadButton.setEnabled(valid);
		});
		VerticalLayout fieldsLayout = new VerticalLayout(separatorTextField);
		fieldsLayout.setPadding(false);

		// Buttons
		cancelButton = new Button("Cancel");
		cancelButton.addClickListener(event -> close());

		downloadButton = new Button("Download", new Icon(VaadinIcon.DOWNLOAD_ALT));
		downloadButton.addThemeVariants(ButtonVariant.PRIMARY);
		downloadButton.addClickListener(event -> close());

		Anchor downloadAnchor = new Anchor();
		downloadAnchor.setHref(DownloadHandler
			.fromInputStream(event -> new DownloadResponse(new ByteArrayInputStream(csvSupplier.get()), "export.csv",
					"text/csv", -1)));
		downloadAnchor.getElement().setAttribute("download", true);
		downloadAnchor.add(downloadButton);

		HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, downloadAnchor);
		buttonLayout.setJustifyContentMode(JustifyContentMode.CENTER);
		buttonLayout.setWidthFull();

		add(new VerticalLayout(fieldsLayout, buttonLayout));
	}

}