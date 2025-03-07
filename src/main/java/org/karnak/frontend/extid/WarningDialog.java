/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.extid;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;

public class WarningDialog extends Dialog {

	public WarningDialog(String title, String text, String buttonText) {
		removeAll();
		Div divTitle = new Div();
		divTitle.setText(title);
		divTitle.getStyle().set("font-size", "large").set("font-weight", "bolder").set("padding-bottom", "10px");

		Div divContent = new Div();
		Div divIntro = new Div();
		divIntro.setText(text);
		divIntro.getStyle().set("padding-bottom", "10px");

		divContent.add(divIntro);

		Button cancelButton = new Button(buttonText, event -> close());

		cancelButton.getStyle().set("margin-left", "75%");
		add(divTitle, divContent, cancelButton);
	}

}
