/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.project.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import lombok.Setter;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.frontend.component.ProfileDropDown;

public class EditProject extends VerticalLayout {

	@Getter
	@Setter
	private ProfileDropDown profileDropDown;

	@Getter
	private final WarningRemoveProjectUsed dialogWarning;

	@Getter
	@Setter
	private Binder<ProjectEntity> binder;

	private TextField textProjectName;

	private ProjectSecret projectSecret;

	private HorizontalLayout horizontalLayoutButtons;

	@Getter
	@Setter
	private Button buttonUpdate;

	@Getter
	@Setter
	private Button buttonRemove;

	@Getter
	@Setter
	private ProjectEntity projectEntity;

	public EditProject() {
		this.dialogWarning = new WarningRemoveProjectUsed();
		setEnabled(false);
		setElements();

		add(this.textProjectName, this.profileDropDown, this.projectSecret, this.horizontalLayoutButtons);
	}

	public void setProject(ProjectEntity projectEntity) {
		this.projectEntity = projectEntity;
		if (projectEntity != null) {
			projectSecret.addValuesCombobox(projectEntity);
			binder.setBean(projectEntity);
			setEnabled(true);
		}
		else {
			binder.removeBean();
			clear();
			setEnabled(false);
		}
	}

	private void setElements() {
		TextFieldsBindProject textFieldsBindProject = new TextFieldsBindProject();
		binder = textFieldsBindProject.getBinder();
		textProjectName = textFieldsBindProject.getTextResearchName();
		profileDropDown = textFieldsBindProject.getProfileDropDown();
		projectSecret = new ProjectSecret(textFieldsBindProject.getSecretComboBox());
		textProjectName.setLabel("Project Name");
		textProjectName.setWidthFull();
		profileDropDown.setLabel("De-identification Profile");
		profileDropDown.setWidthFull();
		buttonUpdate = new Button("Update");
		buttonRemove = new Button("Remove");
		buttonRemove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
		horizontalLayoutButtons = new HorizontalLayout(buttonUpdate, buttonRemove);
	}

	public void clear() {
		binder.readBean(new ProjectEntity());
	}

}
