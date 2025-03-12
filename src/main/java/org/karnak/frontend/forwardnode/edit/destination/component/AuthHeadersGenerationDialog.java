/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.destination.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import java.util.Base64;

public class AuthHeadersGenerationDialog extends Dialog {

	public static final String AUTHORIZATION_TAG = "<key>Authorization</key>";

	public static final String TITLE = "Generate Authorization Header";

	private static final String BASIC_AUTH = "Basic Auth";

	private static final String OAUTH2 = "OAuth 2";

	private static final String FIELD_WIDTH = "400px";

	private static final String BUTTON_WIDTH = "200px";

	private Select<String> authTypeSelect;

	private final String[] authTypeSelectValues = { BASIC_AUTH, OAUTH2 };

	private Button cancelButton;

	private Button generateButton;

	private Div divContent;

	private Div divTitle;

	private Div divSelectBox;

	private FormLayout basicForm;

	private TextField basicUsername;

	private TextField basicPassword;

	private FormLayout oauthForm;

	private TextField oauthToken;

	private final FormSTOW parentForm;

	private static final String REQUIRED_MESSAGE = "This field is required";

	public AuthHeadersGenerationDialog(FormSTOW parentForm) {
		this.parentForm = parentForm;

		removeAll();
		setWidth("50%");

		setElement();
		HorizontalLayout horizontalLayout = new HorizontalLayout(cancelButton, generateButton);
		horizontalLayout.setWidthFull();
		horizontalLayout.getStyle().set("justify-content", "flex-end").set("margin-top", "20px");
		add(divTitle, divSelectBox, divContent, horizontalLayout);
	}

	private void setElement() {
		divTitle = new Div();
		divTitle.setText(TITLE);
		divTitle.getStyle().set("font-size", "large").set("font-weight", "bolder").set("padding-bottom", "10px");

		divContent = new Div();
		divSelectBox = new Div();
		authTypeSelect = new Select<>();
		authTypeSelect.setItems(authTypeSelectValues);
		authTypeSelect.setLabel("Authorization Type");
		authTypeSelect.setErrorMessage("This field is mandatory");
		authTypeSelect.setWidth(FIELD_WIDTH);
		authTypeSelect.setEmptySelectionAllowed(false);
		authTypeSelect.addValueChangeListener(value -> displayAuthTypeForm(value.getValue()));

		divSelectBox.add(authTypeSelect);

		generateButton = new Button("Generate Headers", event -> {
			if (validateFields(authTypeSelect.getValue())) {
				generateAuthHeaders(authTypeSelect.getValue());
			}
		});
		generateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		generateButton.setWidth(BUTTON_WIDTH);
		cancelButton = new Button("Cancel", event -> close());
		cancelButton.setWidth(BUTTON_WIDTH);

		buildForms();
	}

	/**
	 * Display the form according to the Authentication Type selected in the select box
	 * @param value : String corresponding to the Authentication Type chosen in the select
	 * box
	 */
	private void displayAuthTypeForm(String value) {
		divContent.removeAll();
		switch (value) {
			case BASIC_AUTH:
				divContent.add(basicForm);
				break;
			case OAUTH2:
				divContent.add(oauthForm);
				break;
			default:
				break;
		}
	}

	/**
	 * Validation method for the authorization type select box, and the fields in the
	 * corresponding form
	 * @param authType : String corresponding to the Authorization Type chosen in the
	 * select box
	 * @return true if the form is valid, false otherwise
	 */
	private boolean validateFields(String authType) {
		// Mark the authorization type select box as invalid if a value is not selected
		if (authTypeSelect.isEmpty()) {
			authTypeSelect.setInvalid(true);
			return false; // set the validation as failed
		}
		// Once the authorization type is chosen, ensure that the proper fields are filled
		// for the generation
		switch (authType) {
			case BASIC_AUTH:
				basicUsername.setInvalid(basicUsername.isEmpty());
				basicPassword.setInvalid(basicPassword.isEmpty());
				return !(basicUsername.isEmpty() || basicPassword.isEmpty());
			case OAUTH2:
				oauthToken.setInvalid(oauthToken.isEmpty());
				return !(oauthToken.isEmpty());
			default:
				break;
		}
		return false;
	}

	private void buildForms() {
		buildBasicAuthForm();
		buildOAuth2Form();
	}

	/**
	 * Create the elements necessary to render the Basic Auth form using the basicForm
	 * instance
	 */
	private void buildBasicAuthForm() {
		basicForm = new FormLayout();
		basicForm.setWidthFull();

		basicUsername = new TextField();
		basicUsername.setWidth(FIELD_WIDTH);
		basicUsername.setRequiredIndicatorVisible(true);
		basicUsername.setErrorMessage(REQUIRED_MESSAGE);
		basicUsername.setRequired(true);
		basicUsername.setLabel("Username");

		basicPassword = new TextField();
		basicPassword.setWidth(FIELD_WIDTH);
		basicPassword.setRequiredIndicatorVisible(true);
		basicPassword.setErrorMessage(REQUIRED_MESSAGE);
		basicPassword.setRequired(true);
		basicPassword.setLabel("Password");

		basicForm.add(basicUsername);
		basicForm.add(basicPassword);
	}

	/**
	 * Create the elements necessary to render the OAuth 2 form using the oauthForm
	 * instance
	 */
	private void buildOAuth2Form() {
		oauthForm = new FormLayout();
		oauthForm.setWidthFull();

		oauthToken = new TextField();
		oauthToken.setWidth(FIELD_WIDTH);
		oauthToken.setRequiredIndicatorVisible(true);
		oauthToken.setErrorMessage(REQUIRED_MESSAGE);
		oauthToken.setLabel("OAuth 2 Token");

		oauthForm.add(oauthToken);
	}

	/**
	 * Generate the Authorization Header based on the Authorization type and the data
	 * entered. The headers are appended to the parent form headers field.
	 * @param authType : String corresponding to the Authorization Type chosen in the
	 * select box
	 */
	private void generateAuthHeaders(String authType) {
		StringBuilder sb = new StringBuilder();
		sb.append(AUTHORIZATION_TAG);
		sb.append("\n");
		sb.append("<value>");
		switch (authType) {
			case BASIC_AUTH:
				String credentials = basicUsername.getValue() + ":" + basicPassword.getValue();
				sb.append("Basic ");
				sb.append(Base64.getEncoder().encodeToString(credentials.getBytes()));
				break;
			case OAUTH2:
				sb.append("Bearer ");
				sb.append(oauthToken.getValue());
				break;
			default:
				break;
		}
		sb.append("</value>");

		this.parentForm.appendToHeaders(sb.toString());
		close();
	}

}
