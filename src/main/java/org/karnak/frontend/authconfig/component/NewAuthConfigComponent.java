/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.authconfig.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;

public class NewAuthConfigComponent extends HorizontalLayout {

	private final Button newAuthConfigBtn;

	private final TextField newNameField;

	private final Button saveAuthConfig;

	private final Button cancelAuthConfig;

	public NewAuthConfigComponent() {
		newNameField = new TextField();
		saveAuthConfig = new Button("Create");
		cancelAuthConfig = new Button("Cancel");
		newAuthConfigBtn = new Button("Create Authentication Config");
		initView();
	}

	private void initView() {
		setNewNameField();
		setSaveAuthConfig();
		setCancelAuthConfig();
		setNewAuthConfigBtn();
		add(newAuthConfigBtn);
	}

	public void resetNewAuthConfigComponent() {
		removeAll();
		add(newAuthConfigBtn);
	}

	private void setNewNameField() {
		newNameField.setPlaceholder("Identifier");
	}

	private void setSaveAuthConfig() {
		saveAuthConfig.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		saveAuthConfig.setIcon(VaadinIcon.PLUS_CIRCLE.create());
	}

	private void setCancelAuthConfig() {
		cancelAuthConfig.addClickListener(click -> {
			removeAll();
			add(newAuthConfigBtn);
		});
	}

	private void setNewAuthConfigBtn() {
		newAuthConfigBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		newAuthConfigBtn.setIcon(VaadinIcon.PLUS_CIRCLE.create());
		newAuthConfigBtn.addClickListener(click -> {
			removeAll();
			newNameField.setValue("");
			add(newNameField, saveAuthConfig, cancelAuthConfig);
		});
	}

	public Button getNewAuthConfigBtn() {
		return newAuthConfigBtn;
	}

	public TextField getNewNameField() {
		return newNameField;
	}

	public Button getSaveAuthConfig() {
		return saveAuthConfig;
	}

	public Button getCancelAuthConfig() {
		return cancelAuthConfig;
	}

}
