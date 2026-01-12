/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.util.SystemPropertyUtil;
import org.karnak.frontend.forwardnode.ForwardNodeLogic;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.weasis.core.util.StringUtil;
import org.weasis.dicom.op.CStore;
import org.weasis.dicom.param.DicomNode;

public class EditAETitleDescription extends HorizontalLayout {

	private final TextField textFieldAETitle;

	private final TextField textFieldDescription;

	private final Button selectFolderButton;

	private final Binder<ForwardNodeEntity> binder;

	public EditAETitleDescription(Binder<ForwardNodeEntity> binder, ForwardNodeLogic forwardNodeLogic,
			Environment environment) {
		this.binder = binder;
		this.textFieldAETitle = new TextField("Forward AETitle");
		this.textFieldDescription = new TextField("Description");
		this.selectFolderButton = new Button("Upload local folder", VaadinIcon.FOLDER_OPEN.create());
		selectFolderButton.addClickListener(event -> {
			ForwardNodeEntity forwardNode = binder.getBean();

			if (forwardNode != null) {
				forwardNode = forwardNodeLogic.retrieveForwardNodeById(forwardNode.getId());
			}

			if (forwardNode != null && forwardNode.getDestinationEntities() != null
					&& !forwardNode.getDestinationEntities().isEmpty()
					&& forwardNode.getDestinationEntities().stream().anyMatch(DestinationEntity::isActivate)) {
				selectFolder();
			}
			else {
				Notification.show("No active destination configured for this forward node", 3000,
						Notification.Position.MIDDLE);
			}
		});

		setVerticalComponentAlignment(Alignment.BASELINE, textFieldAETitle, textFieldDescription);

		textFieldAETitle.setWidth("30%");
		textFieldDescription.setWidth("70%");
		add(textFieldAETitle, textFieldDescription);

		if (environment.acceptsProfiles(Profiles.of("portable"))) {
			setVerticalComponentAlignment(Alignment.END, selectFolderButton);
			add(selectFolderButton);
		}
		setBinder();
	}

	private void selectFolder() {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("Upload DICOM files from a local folder");

		TextField pathField = new TextField("Folder Path");
		pathField.setPlaceholder("Enter the absolute path to the folder (e.g., /home/user/documents)");
		pathField.setWidthFull();

		Span errorMessage = new Span();
		errorMessage.getStyle().set("color", "var(--lumo-error-text-color)");
		errorMessage.setVisible(false);

		Button confirmButton = new Button("Confirm", e -> {
			dicomSend(pathField, dialog, errorMessage);
		});

		dialog.add(pathField, errorMessage);
		dialog.getFooter().add(new Button("Cancel", e -> dialog.close()), confirmButton);
		dialog.setWidth("500px");
		dialog.open();
	}

	private void dicomSend(TextField pathField, Dialog dialog, Span errorMessage) {
		String path = pathField.getValue();
		if (StringUtil.hasText(path)) {
			Path folderPath = Path.of(path);
			if (Files.isDirectory(folderPath)) {
				textFieldDescription.setValue(path);
				DicomNode callingNode = new DicomNode("LOCAL_FOLDER");
				int port = SystemPropertyUtil.retrieveIntegerSystemProperty("DICOM_LISTENER_PORT", 11119);
				DicomNode remoteNode = new DicomNode(textFieldAETitle.getValue(), "localhost", port);
				dialog.close();
				CompletableFuture.runAsync(() -> CStore.process(callingNode, remoteNode, List.of(path), null));
			}
			else {
				errorMessage.setText("The specified path is not a valid folder");
				errorMessage.setVisible(true);
			}
		}
		else {
			errorMessage.setText("Please enter a folder path");
			errorMessage.setVisible(true);
		}
	}

	public void setForwardNode(ForwardNodeEntity forwardNodeEntity) {
		if (forwardNodeEntity != null) {
			binder.readBean(forwardNodeEntity);
			binder.setBean(forwardNodeEntity);
			setEnabled(true);
			selectFolderButton.setEnabled(true);
		}
		else {
			binder.readBean(null);
			binder.setBean(null);
			textFieldDescription.clear();
			textFieldAETitle.clear();
			setEnabled(false);
			selectFolderButton.setEnabled(false);
		}
	}

	@Override
	public void setEnabled(boolean enabled) {
		textFieldAETitle.setEnabled(enabled);
		textFieldDescription.setEnabled(enabled);
	}

	private void setBinder() {
		binder.forField(textFieldAETitle)
			.withValidator(value -> !value.isEmpty(), "Forward AE Title is mandatory")
			.withValidator(value -> value.length() <= 16, "Forward AETitle has more than 16 characters")
			.bind(ForwardNodeEntity::getFwdAeTitle, ForwardNodeEntity::setFwdAeTitle);
		binder.forField(textFieldDescription)
			.bind(ForwardNodeEntity::getFwdDescription, ForwardNodeEntity::setFwdDescription);
	}

}
