/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom.mwl;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import org.dcm4che3.data.Attributes;

public class DicomPane extends Composite<Dialog> {

	// CONTROLLER
	private final DicomPaneLogic logic = new DicomPaneLogic();

	// DATA
	private final Attributes dcm;

	// UI COMPONENTS
	private Dialog currentDialog;

	private VerticalLayout mainLayout;

	private Div titleBar;

	private TextArea contentFld;

	private HorizontalLayout buttonBar;

	private Button cancelButton;

	private Anchor downloadDicomAnchor;

	private Anchor downloadTextAnchor;

	public DicomPane(Attributes dcm) {
		this.dcm = dcm;

		init();

		buildMainLayout();

		currentDialog.add(mainLayout);
	}

	public void open() {
		currentDialog.open();
	}

	private void init() {
		currentDialog = getContent();
		currentDialog.setWidth("50%");
	}

	private void buildMainLayout() {
		mainLayout = new VerticalLayout();
		mainLayout.setSizeFull();
		mainLayout.setPadding(false);

		buildTitleBar();
		buildContentField();
		buildButtonBar();

		mainLayout.add(titleBar, contentFld, buttonBar);

		mainLayout.setFlexGrow(1, contentFld);
	}

	private void buildTitleBar() {
		titleBar = new Div();
		titleBar.setText("Worklist Entry");
	}

	private void buildContentField() {
		contentFld = new TextArea();

		contentFld.setReadOnly(true);
		contentFld.setHeight("600px");
		contentFld.setWidth("600px");

		contentFld.setValue(dcm.toString(1500, 300));
	}

	private void buildButtonBar() {
		buttonBar = new HorizontalLayout();

		buildDownloadTextAnchor();
		buildDownloadDicomAnchor();
		buildCancelButton();

		buttonBar.add(cancelButton, downloadDicomAnchor, downloadTextAnchor);
	}

	private void buildDownloadTextAnchor() {
		Button downloadTextBtn = new Button();
		downloadTextBtn.setText("Download Text");
		downloadTextBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		downloadTextAnchor = new Anchor();
		downloadTextAnchor.getElement().setAttribute("download", true);
		downloadTextAnchor.add(downloadTextBtn);
		downloadTextAnchor.setHref(
				DownloadHandler.fromInputStream(event -> new DownloadResponse(logic.getWorklistItemInputStreamText(dcm),
						"worklistItem.txt", "text/plain", -1)));
	}

	private void buildDownloadDicomAnchor() {
		Button downloadDicomBtn = new Button();
		downloadDicomBtn.setText("Download DICOM");
		downloadDicomBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		downloadDicomAnchor = new Anchor();
		downloadDicomAnchor.getElement().setAttribute("download", true);
		downloadDicomAnchor.add(downloadDicomBtn);
		downloadDicomAnchor.setHref(DownloadHandler
			.fromInputStream(event -> new DownloadResponse(logic.getWorklistItemInputStreamInDicom(dcm),
					"worklistItem.dcm", "application/dicom", -1)));
	}

	private void buildCancelButton() {
		cancelButton = new Button("Cancel", event -> currentDialog.close());
	}

}
