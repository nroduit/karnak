/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.project.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.frontend.component.ButtonFactory;
import org.karnak.frontend.component.NewItemDialog;
import org.karnak.frontend.component.ProfileDropDown;
import org.weasis.core.util.annotations.Generated;

/**
 * Toolbar entry that creates a new project: a "New project" button which opens a popup
 * ({@link NewItemDialog}) where the project name and de-identification profile are
 * chosen.
 */
@Generated()
public class NewProject extends HorizontalLayout {

	@Getter
	private final Binder<ProjectEntity> binder;

	@Getter
	private final Button buttonNewProject;

	@Getter
	private final NewItemDialog dialog;

	private final TextField textResearchName;

	@Getter
	private final ProfileDropDown profileDropDown;

	public NewProject() {

		TextFieldsBindProject textFieldsBindProject = new TextFieldsBindProject();
		this.binder = textFieldsBindProject.getBinder();
		this.textResearchName = textFieldsBindProject.getTextResearchName();
		this.profileDropDown = textFieldsBindProject.getProfileDropDown();
		setElements();

		this.dialog = new NewItemDialog("New project", "Add", this.textResearchName, this.profileDropDown);

		this.buttonNewProject = ButtonFactory.createAddButton("New project");
		this.buttonNewProject.addClickListener(click -> openDialog());

		setPadding(false);
		add(this.buttonNewProject);

		this.binder.removeBinding(textFieldsBindProject.getSecretComboBox());
		this.binder.bindInstanceFields(this);
	}

	private void setElements() {
		textResearchName.setLabel("Name");
		textResearchName.setPlaceholder("Enter Name");
		textResearchName.setWidthFull();
		profileDropDown.setWidthFull();
	}

	private void openDialog() {
		clear();
		dialog.open();
		textResearchName.focus();
	}

	public void clear() {
		binder.readBean(new ProjectEntity());
	}

}
