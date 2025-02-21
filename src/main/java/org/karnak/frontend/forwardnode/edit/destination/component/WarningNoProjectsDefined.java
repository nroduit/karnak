/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.destination.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

public class WarningNoProjectsDefined extends Dialog {

	private final Button btnValidate;

	private final Button btnCancel;

	public WarningNoProjectsDefined() {
		btnValidate = new Button();
		btnCancel = new Button();

		setContent();
	}

	public void setTextBtnValidate(String text) {
		btnValidate.setText(text);
	}

	public void setTextBtnCancel(String text) {
		btnCancel.setText(text);
	}

	private void setContent() {
		removeAll();
		Div divTitle = new Div();
		divTitle.setText("No projects created");
		divTitle.getStyle()
			.set("font-size", "large")
			.set("font-weight", "bolder")
			.set("padding-bottom", "10px")
			.set("color", "red");

		Div divContent = new Div();
		Div divIntro = new Div();
		divIntro.setText(
				"No projects are defined. You can't use the tag morphing or de-identification until you have created a project.");
		divIntro.getStyle().set("padding-bottom", "10px");
		divContent.add(divIntro);
		btnValidate.setWidth("150px");
		btnCancel.setWidth("150px");

		HorizontalLayout btnLayout = new HorizontalLayout();
		btnLayout.setWidthFull();
		btnLayout.add(btnValidate, btnCancel);
		btnValidate.getElement().getStyle().set("margin-left", "auto"); // https://vaadin.com/forum/thread/17198105/button-alignment-in-horizontal-layout
		add(divTitle, divContent, btnLayout);
	}

	public Button getBtnValidate() {
		return btnValidate;
	}

	public Button getBtnCancel() {
		return btnCancel;
	}

}
