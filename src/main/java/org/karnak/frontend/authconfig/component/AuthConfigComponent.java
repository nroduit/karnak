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
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.karnak.backend.data.entity.AuthConfigEntity;

@Uses(PasswordField.class)
public class AuthConfigComponent extends VerticalLayout {

	private TextField url;

	private TextField scope;

	private PasswordField clientSecret;

	private PasswordField clientId;

	private H2 title;

	@Getter
	private Button saveBtn;

	@Getter
	private Button cancelBtn;

	@Getter
	private Button deleteBtn;

	private Binder<AuthConfigEntity> binder;

	public AuthConfigComponent() {
		buildLayout();
	}

	private void buildLayout() {
		removeAll();
		initDeleteButton();
		title = new H2();
		add(new HorizontalLayout(title, deleteBtn));

		VerticalLayout layout = new VerticalLayout();
		layout.setWidthFull();

		url = new TextField();
		url.setLabel("Access Token URL");
		url.setWidthFull();
		layout.add(url);
		scope = new TextField();
		scope.setLabel("Scope");
		scope.setWidthFull();
		layout.add(scope);
		clientSecret = new PasswordField();
		clientSecret.setLabel("Client Secret");
		clientSecret.setWidthFull();
		layout.add(clientSecret);
		clientId = new PasswordField();
		clientId.setLabel("Client ID");
		clientId.setWidthFull();
		layout.add(clientId);

		add(layout);

		HorizontalLayout buttons = new HorizontalLayout();
		saveBtn = new Button("Save");
		buttons.add(saveBtn);
		cancelBtn = new Button("Cancel");
		buttons.add(cancelBtn);

		add(buttons);

		setBinder();

		this.setVisible(false);
	}

	public void setBinder() {
		binder = new BeanValidationBinder<>(AuthConfigEntity.class);
		binder.forField(clientId)
			.withValidator(StringUtils::isNotBlank, "Client ID is required")
			.bind(AuthConfigEntity::getClientId, AuthConfigEntity::setClientId);

		binder.forField(clientSecret)
			.withValidator(StringUtils::isNotBlank, "Client Secret is required")
			.bind(AuthConfigEntity::getClientSecret, AuthConfigEntity::setClientSecret);

		binder.forField(url)
			.withValidator(StringUtils::isNotBlank, "Url is required")
			.bind(AuthConfigEntity::getAccessTokenUrl, AuthConfigEntity::setAccessTokenUrl);

		binder.forField(scope)
			.withValidator(StringUtils::isNotBlank, "Scope is required")
			.bind(AuthConfigEntity::getScope, AuthConfigEntity::setScope);

		binder.bindInstanceFields(this);
	}

	public void displayData(AuthConfigEntity data) {
		this.binder.readBean(data);

		title.setText(data.getCode());
		url.setReadOnly(true);
		scope.setReadOnly(true);
		clientSecret.setReadOnly(true);
		clientId.setReadOnly(true);
		clientSecret.setRevealButtonVisible(false);
		clientId.setRevealButtonVisible(false);

		this.deleteBtn.setVisible(true);
		this.saveBtn.setVisible(false);
		this.cancelBtn.setVisible(false);
		this.setVisible(true);
	}

	public AuthConfigEntity getData() {
		AuthConfigEntity entity = new AuthConfigEntity();
		entity.setCode(title.getText());
		return binder.writeBeanIfValid(entity) ? entity : null;
	}

	public boolean isValid() {
		binder.validate();
		return binder.isValid();
	}

	public void displayEmptyForm(String code) {
		this.title.setText(code);
		this.binder.readBean(new AuthConfigEntity()); // empty instance
		url.setReadOnly(false);
		scope.setReadOnly(false);
		clientSecret.setReadOnly(false);
		clientSecret.setRevealButtonVisible(true);
		clientId.setReadOnly(false);
		clientId.setRevealButtonVisible(true);
		this.deleteBtn.setVisible(false);
		this.saveBtn.setVisible(true);
		this.cancelBtn.setVisible(true);
		this.setVisible(true);
	}

	public void cancel() {
		this.setVisible(false);
	}

	private void initDeleteButton() {
		deleteBtn = new Button((new Icon(VaadinIcon.TRASH)));
		deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
	}

	public String getAuthConfigCode() {
		return this.title.getText();
	}

}
