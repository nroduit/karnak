/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.destination.component;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.frontend.component.ProjectDropDown;
import org.karnak.frontend.project.ProjectView;
import org.weasis.core.util.annotations.Generated;

@Generated()
public class DestinationComponentUtil extends VerticalLayout {

	/**
	 * Build project drop down
	 */
	public ProjectDropDown buildProjectDropDown() {
		ProjectDropDown projectDropDown = new ProjectDropDown();
		projectDropDown.setItemLabelGenerator(ProjectEntity::getName);
		projectDropDown.setLabel("Choose a project");
		// Keep the project selector compact rather than spanning the whole card; a
		// project name never needs the full destination-panel width.
		projectDropDown.setWidth("300px");
		return projectDropDown;
	}

	/**
	 * Warning No Project Defined
	 */
	public WarningNoProjectsDefined buildWarningNoProjectDefined() {
		WarningNoProjectsDefined warningNoProjectsDefined = new WarningNoProjectsDefined();
		warningNoProjectsDefined.setTextBtnCancel("Continue");
		warningNoProjectsDefined.setTextBtnValidate("Create a project");
		return warningNoProjectsDefined;
	}

	/**
	 * Build Checkbox activate
	 */
	public Checkbox buildActivateCheckbox(final String label) {
		// Checkbox activate
		Checkbox checkbox = new Checkbox(label);
		checkbox.setValue(true);
		checkbox.setMinWidth("20%");
		checkbox.getElement().getStyle().set("margin-block-end", "auto");
		return checkbox;
	}

	/**
	 * Build div which is visible or not depending on the activated checkbox
	 */
	public Div buildActivateDiv() {
		Div div = new Div();
		div.setWidth("100%");
		return div;
	}

	/**
	 * Listener on popup warning no project defined: => navigate to view project or
	 * uncheck activated checkbox
	 */
	public void buildWarningNoProjectDefinedListener(WarningNoProjectsDefined warningNoProjectsDefined,
			Checkbox checkbox) {
		warningNoProjectsDefined.getBtnCancel().addClickListener(btnEvent -> {
			checkbox.setValue(false);
			warningNoProjectsDefined.close();
		});
		warningNoProjectsDefined.getBtnValidate().addClickListener(btnEvent -> {
			warningNoProjectsDefined.close();
			navigateToProject();
		});
	}

	/**
	 * Build listener on projectDropDown
	 */
	public void buildProjectDropDownListener(ProjectDropDown projectDropDown, ProfileLabel profileLabel) {
		projectDropDown.addValueChangeListener(event -> setTextOnSelectionProject(event.getValue(), profileLabel));
	}

	public void navigateToProject() {
		UI.getCurrent().navigate(ProjectView.class, "");
	}

	public void setTextOnSelectionProject(ProjectEntity projectEntity, ProfileLabel profileLabel) {
		if (projectEntity != null && projectEntity.getProfileEntity() != null) {
			profileLabel.setShowValue(String.format("The profile %s [version %s] will be used",
					projectEntity.getProfileEntity().getName(), projectEntity.getProfileEntity().getVersion()));
		}
		else if (projectEntity != null && projectEntity.getProfileEntity() == null) {
			profileLabel.setShowValue("No profiles defined in the project");
		}
		else {
			profileLabel.removeAll();
		}
	}

}
