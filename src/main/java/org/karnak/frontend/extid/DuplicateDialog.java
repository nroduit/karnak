/*
 * Copyright (c) 2021 Karnak Team and other contributors.
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
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import java.util.Collection;
import org.karnak.backend.cache.Patient;

public class DuplicateDialog extends Dialog {

	private final Collection<Patient> duplicateList;

	private Grid<Patient> grid;

	public DuplicateDialog(String title, String text, Collection<Patient> duplicateList, String buttonText) {
		removeAll();
		this.duplicateList = duplicateList;

		Div divTitle = new Div();
		divTitle.setText(title);
		divTitle.getStyle().set("font-size", "large").set("font-weight", "bolder").set("padding-bottom", "10px");

		Div divContent = new Div();
		Div divIntro = new Div();
		divIntro.setText(text);
		divIntro.getStyle().set("padding-bottom", "10px");

		divContent.add(divIntro);

		setGridElement();

		Button cancelButton = new Button(buttonText, event -> close());

		cancelButton.getStyle().set("margin-left", "50%");
		add(divTitle, divContent, grid, cancelButton);
	}

	public void setGridElement() {
		grid = new Grid<>();
		grid.addColumn(Patient::getPseudonym).setHeader("External pseudonym").setSortable(true);
		grid.addColumn(Patient::getPatientId).setHeader("Patient ID").setSortable(true);
		grid.addColumn(Patient::getPatientFirstName).setHeader("Patient first name").setSortable(true);
		grid.addColumn(Patient::getPatientLastName).setHeader("Patient last name").setSortable(true);
		grid.addColumn(Patient::getIssuerOfPatientId).setHeader("Issuer of patient ID").setSortable(true);
		grid.setItems(duplicateList);
	}

}
