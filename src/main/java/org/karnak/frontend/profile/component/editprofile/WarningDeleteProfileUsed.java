/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.profile.component.editprofile;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.data.entity.ProjectEntity;

public class WarningDeleteProfileUsed extends Dialog {

	public void setText(ProfileEntity profileEntity) {
		removeAll();
		Div divTitle = new Div();
		divTitle.setText(String.format("The profile %s can't be remove", profileEntity.getName()));
		divTitle.getStyle()
			.set("font-size", "large")
			.set("font-weight", "bolder")
			.set("padding-bottom", "10px")
			.set("color", "red");

		Div divContent = new Div();
		Div divIntro = new Div();
		divIntro.setText("The profile is used in the following project(s)");
		divIntro.getStyle().set("padding-bottom", "10px");

		divContent.add(divIntro);
		if (profileEntity.getProjectEntities() != null) {
			for (ProjectEntity projectEntity : profileEntity.getProjectEntities()) {
				Div divProject = new Div();
				divProject.setText(String.format("Project: %s", projectEntity.getName()));
				divProject.getStyle().set("padding-left", "20px").set("padding-bottom", "5px");
				divContent.add(divProject);
			}
		}

		Button cancelButton = new Button("Cancel", event -> close());

		cancelButton.getStyle().set("margin-left", "75%");
		add(divTitle, divContent, cancelButton);
	}

}
