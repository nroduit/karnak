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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.frontend.component.ProfileDropDown;

public class NewProject extends HorizontalLayout {

	@Getter
	private final Binder<ProjectEntity> binder;

	@Getter
	private final Button buttonAdd;

	private final TextField textResearchName;

	@Getter
	private final ProfileDropDown profileDropDown;

	public NewProject() {

		TextFieldsBindProject textFieldsBindProject = new TextFieldsBindProject();
		setWidthFull();
		this.binder = textFieldsBindProject.getBinder();
		this.buttonAdd = new Button("Add");
		this.textResearchName = textFieldsBindProject.getTextResearchName();
		this.profileDropDown = textFieldsBindProject.getProfileDropDown();
		setElements();
		add(this.textResearchName, this.profileDropDown, this.buttonAdd);
		this.binder.removeBinding(textFieldsBindProject.getSecretComboBox());
		this.binder.bindInstanceFields(this);
	}

	private void setElements() {

		textResearchName.setWidth("20%");
		textResearchName.getStyle().set("padding-right", "10px");
		textResearchName.setPlaceholder("Enter Name");
	}

	public void clear() {
		binder.readBean(new ProjectEntity());
	}

}
