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

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.frontend.component.ProjectDropDown;
import org.weasis.core.util.annotations.Generated;

@Getter
@Generated()
@NullUnmarked
public class TagMorphingComponent extends VerticalLayout {

	// Labels
	private static final String LABEL_CHECKBOX_TAG_MORPHING = "Activate tag morphing";

	// Components
	private Checkbox tagMorphingCheckbox;

	private ProjectDropDown projectDropDown;

	private WarningNoProjectsDefined warningNoProjectsDefined;

	@Setter
	private Binder<DestinationEntity> destinationBinder;

	private Div tagMorphingDiv;

	private final DestinationComponentUtil destinationComponentUtil;

	private ProfileLabel profileLabel;

	/**
	 * Constructor
	 */
	public TagMorphingComponent() {
		this.destinationComponentUtil = new DestinationComponentUtil();
	}

	/**
	 * Init deidentification component
	 * @param binder Binder for checks
	 */
	public void init(final Binder<DestinationEntity> binder) {
		// Init destination binder
		setDestinationBinder(binder);

		// Build deidentification components
		buildComponents();

		// Init destination binder
		initDestinationBinder();

		// Build Listeners
		buildListeners();

		// Add components
		addComponents();
	}

	/**
	 * Build deidentification components
	 */
	private void buildComponents() {
		profileLabel = new ProfileLabel();
		projectDropDown = destinationComponentUtil.buildProjectDropDown();
		warningNoProjectsDefined = destinationComponentUtil.buildWarningNoProjectDefined();
		tagMorphingCheckbox = destinationComponentUtil.buildActivateCheckbox(LABEL_CHECKBOX_TAG_MORPHING);
		tagMorphingDiv = destinationComponentUtil.buildActivateDiv();
	}

	private void initDestinationBinder() {
		destinationBinder.forField(tagMorphingCheckbox)
			.bind(DestinationEntity::isActivateTagMorphing, DestinationEntity::setActivateTagMorphing);
		destinationBinder.forField(projectDropDown)
			.withValidator(project -> project != null || !tagMorphingCheckbox.getValue(), "Choose a project")
			.bind(DestinationEntity::getTagMorphingProjectEntity, DestinationEntity::setTagMorphingProjectEntity);
	}

	/**
	 * Build listeners
	 */
	private void buildListeners() {
		destinationComponentUtil.buildWarningNoProjectDefinedListener(warningNoProjectsDefined, tagMorphingCheckbox);
		destinationComponentUtil.buildProjectDropDownListener(projectDropDown, profileLabel);
	}

	/**
	 * Add components
	 */
	private void addComponents() {
		// Padding
		setPadding(true);

		// Keep the resolved-profile label tight under the project field
		VerticalLayout content = new VerticalLayout();
		content.setPadding(false);
		content.setSpacing(false);
		content.setWidthFull();
		content.getStyle().set("gap", "0.25rem");
		content.add(projectDropDown, profileLabel);
		tagMorphingDiv.add(content);

		// If checkbox is checked set div visible, invisible otherwise
		tagMorphingDiv.setVisible(tagMorphingCheckbox.getValue());

		// Checkbox as a full-width header row, the form fields stacked below it
		add(tagMorphingCheckbox, tagMorphingDiv);
	}

}
