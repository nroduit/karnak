/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.pseudonym.mapping.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Style;
import lombok.Getter;
import lombok.Setter;
import org.karnak.frontend.component.BoxShadowComponent;

/**
 * Input pseudonym that the user wants to look for
 */
@Setter
@Getter
public class MappingInputComponent extends VerticalLayout {

	// Components
	private TextField pseudonymTextField;

	private Button findButton;

	/**
	 * Constructor
	 */
	public MappingInputComponent() {

		// TextField input for pseudonym
		pseudonymTextField = new TextField();
		pseudonymTextField.setPlaceholder("Pseudonym to look for...");
		pseudonymTextField.setWidth("30vw");
		pseudonymTextField.setMinWidth("200px");

		// Find Button
		findButton = new Button("Find...");
		findButton.setAutofocus(true);

		// Pseudonym layout
		HorizontalLayout pseudonymLayout = new HorizontalLayout();
		pseudonymLayout.getStyle().setJustifyContent(Style.JustifyContent.CENTER);
		pseudonymLayout.add(pseudonymTextField, findButton);
		BoxShadowComponent pseudonymBoxShadowComponent = new BoxShadowComponent(pseudonymLayout);
		pseudonymBoxShadowComponent.getElement().getStyle().set("padding", "10px");
		pseudonymBoxShadowComponent.getElement().getStyle().set("margin", "auto");
		add(pseudonymBoxShadowComponent);
	}

}
