/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.project.component;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.provider.ListDataProvider;
import java.util.Collection;
import java.util.Objects;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.entity.SecretEntity;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.frontend.component.WarningConfirmDialog;

public class ProjectSecret extends Div {

	private static final String WARNING_TEXT = "If you change the project secret, the consistency of previously de-identified DICOM files cannot be guaranteed anymore.";

	private static final String REFER_LINK_TEXT = "For more details on the use of the project secret, please refer to the following link";

	private static final Anchor REFER_LINK = new Anchor(
			"https://osirix-foundation.github.io/karnak-documentation/en/userguide/projects/index.html#4-project-secret",
			"Project Secret Documentation");

	private final Div messageWarningLayout = new Div();

	private final ComboBox<SecretEntity> secretComboBox;

	private final Button generateSecretButton = new Button("Generate Secret");

	private ProjectEntity projectEntity;

	public ProjectSecret(ComboBox<SecretEntity> secretComboBox) {
		this.secretComboBox = secretComboBox;
		setWidthFull();
		addComponents();
		addMessageWarningLayout();
		eventGenerateSecret();
		eventImportSecret();
	}

	private void addComponents() {
		secretComboBox.getStyle().set("width", "80%");
		secretComboBox.setPlaceholder("Project Secret");
		secretComboBox.setAllowCustomValue(true);
		generateSecretButton.getStyle().set("margin-left", "10px");
		add(secretComboBox, generateSecretButton);
	}

	public void addValuesCombobox(ProjectEntity projectEntity) {
		this.projectEntity = projectEntity;
		secretComboBox.setItems(projectEntity.getSecretEntities());
		secretComboBox.setItemLabelGenerator(ProjectEntity::buildLabelSecret);
	}

	private void addMessageWarningLayout() {
		messageWarningLayout.add(new Div(new Text(WARNING_TEXT)));
		messageWarningLayout.add(new Div(new Text(REFER_LINK_TEXT)));
		messageWarningLayout.add(REFER_LINK);
	}

	private void eventGenerateSecret() {
		generateSecretButton.addClickListener(event -> {
			WarningConfirmDialog dialog = new WarningConfirmDialog(messageWarningLayout);
			dialog.addConfirmationListener(componentEvent -> {
				SecretEntity secretEntityToCreate = new SecretEntity(projectEntity, HMAC.generateRandomKey());
				projectEntity.addActiveSecretEntity(secretEntityToCreate);
				secretComboBox.setValue(secretEntityToCreate);
				secretComboBox.getDataProvider().refreshAll();
			});
			dialog.open();
		});
	}

	/**
	 * Manage event when importing a secret
	 */
	private void eventImportSecret() {
		secretComboBox.addCustomValueSetListener(event -> {
			// Retrieve value entered by user
			String customValue = event.getDetail();

			// Check if valid are already existing
			boolean valid = HMAC.validateKey(customValue);
			Collection<SecretEntity> items = ((ListDataProvider) secretComboBox.getDataProvider()).getItems();
			boolean alreadyExisting = valid && items.stream()
				.map(secretEntity -> HMAC.showHexKey(HMAC.byteToHex(secretEntity.getSecretKey())))
				.anyMatch(secretValue -> Objects.equals(secretValue, customValue));

			// Already existing
			if (alreadyExisting) {
				secretComboBox.setInvalid(true);
				secretComboBox.setErrorMessage("Secret is already existing");
			}
			// Not valid
			else if (!valid) {
				secretComboBox.setInvalid(true);
				secretComboBox.setErrorMessage("Secret is not valid");
			}
			else {
				// Ok
				secretComboBox.setInvalid(false);
				WarningConfirmDialog dialog = new WarningConfirmDialog(messageWarningLayout);
				dialog.addConfirmationListener(componentEvent -> {
					SecretEntity secretEntityToCreate = new SecretEntity(projectEntity, HMAC.hexToByte(customValue));
					projectEntity.addActiveSecretEntity(secretEntityToCreate);
					secretComboBox.setValue(secretEntityToCreate);
					secretComboBox.getDataProvider().refreshAll();
				});
				dialog.open();
			}
		});
	}

}
