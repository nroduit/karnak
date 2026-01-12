/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.profile.component.editprofile;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import lombok.Getter;

public class ProfileMetadata extends VerticalLayout {

	private final Div titleDiv = new Div();

	private final Div valueDiv = new Div();

	private final TextField valueField = new TextField();

	private final Button editButton = new Button(new Icon(VaadinIcon.EDIT));

	@Getter
	private final Button validateEditButton = new Button(new Icon(VaadinIcon.CHECK));

	private final Button disabledEditButton = new Button(new Icon(VaadinIcon.CLOSE));

	private final String title;

	@Getter
	private String value;

	public ProfileMetadata(String title, String value, Boolean profileByDefault) {
		this.title = title;
		this.value = value;

		setTitleText();
		setValueText();
		setElements();
		addEvents();

		if (!profileByDefault) {
			titleDiv.add(editButton);
		}

		add(titleDiv, valueDiv);
	}

	private void addEvents() {
		editButton.addClickListener(event -> editOnClick());

		disabledEditButton.addClickListener(event -> disabledEditButton());

		validateEditButton.addClickListener(event -> validateEditButton());
	}

	private void setElements() {
		titleDiv.getStyle()
			.set("font-weight", "bold")
			.set("margin-top", "0px")
			.set("padding-left", "5px")
			.set("line-height", "31.5px")
			.setHeight("38.5px");
		valueDiv.getStyle()
			.set("color", "grey")
			.set("padding-left", "10px")
			.set("line-height", "31.5px")
			.setHeight("38.5px");
		editButton.getStyle().set("margin-left", "10px");
		validateEditButton.getStyle().set("margin-left", "5px");
		disabledEditButton.getStyle().set("margin-left", "5px");
	}

	private void setTitleText() {
		titleDiv.setText(this.title);
	}

	private void setValueText() {
		String text = "Not defined";
		if (this.value != null) {
			text = this.value;
		}
		Text valueText = new Text(text);
		valueDiv.add(valueText);
	}

	private void setValueTextField() {
		valueField.setValue("");
		if (this.value != null) {
			valueField.setValue(this.value);
		}
		valueDiv.add(valueField);
		valueDiv.add(validateEditButton);
		valueDiv.add(disabledEditButton);
	}

	private void editOnClick() {
		editButton.getStyle().set("visibility", "hidden");
		valueDiv.removeAll();
		setValueTextField();
	}

	private void disabledEditButton() {
		editButton.getStyle().set("visibility", "visible");
		valueDiv.removeAll();
		setValueText();
	}

	private void validateEditButton() {
		editButton.getStyle().set("visibility", "visible");
		value = valueField.getValue();
		valueDiv.removeAll();
		setValueText();
	}

}
