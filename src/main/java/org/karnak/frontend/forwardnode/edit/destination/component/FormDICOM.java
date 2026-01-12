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

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.Binder;
import java.util.Objects;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.frontend.component.BoxShadowComponent;
import org.karnak.frontend.component.converter.HStringToIntegerConverter;
import org.karnak.frontend.forwardnode.edit.component.ButtonSaveDeleteCancel;
import org.karnak.frontend.util.UIS;

public class FormDICOM extends VerticalLayout {

	private Binder<DestinationEntity> binder;

	private TextField aeTitle;

	private TextField description;

	private TextField hostname;

	private TextField port;

	private Checkbox useAETitleCheckbox;

	private Checkbox activate;

	@Getter
	private final DeIdentificationComponent deIdentificationComponent;

	@Getter
	private final TagMorphingComponent tagMorphingComponent;

	@Getter
	private final FilterBySOPClassesForm filterBySOPClassesForm;

	private final DestinationCondition destinationCondition;

	@Getter
	private final NotificationComponent notificationComponent;

	@Getter
	private final TransferSyntaxComponent transferSyntaxComponent;

	@Getter
	private final TranscodeOnlyUncompressedComponent transcodeOnlyUncompressedComponent;

	public FormDICOM() {
		this.deIdentificationComponent = new DeIdentificationComponent();
		this.tagMorphingComponent = new TagMorphingComponent();
		this.filterBySOPClassesForm = new FilterBySOPClassesForm();
		this.destinationCondition = new DestinationCondition();
		this.notificationComponent = new NotificationComponent();
		this.transferSyntaxComponent = new TransferSyntaxComponent();
		this.transcodeOnlyUncompressedComponent = new TranscodeOnlyUncompressedComponent();
	}

	public void init(Binder<DestinationEntity> binder, ButtonSaveDeleteCancel buttonSaveDeleteCancel) {
		this.binder = binder;
		deIdentificationComponent.init(this.binder);
		tagMorphingComponent.init(this.binder);
		filterBySOPClassesForm.init(this.binder);
		destinationCondition.init(this.binder);
		notificationComponent.init(this.binder);
		transferSyntaxComponent.init(this.binder);
		transcodeOnlyUncompressedComponent.init(this.binder);

		setSizeFull();

		aeTitle = new TextField("AETitle");
		description = new TextField("Description");
		hostname = new TextField("Hostname");
		port = new TextField("Port");
		useAETitleCheckbox = new Checkbox("Use AETitle destination");
		activate = new Checkbox("Enable destination");

		// Define layout
		VerticalLayout destinationLayout = new VerticalLayout(
				UIS.setWidthFull(new HorizontalLayout(aeTitle, description)), destinationCondition,
				UIS.setWidthFull(new HorizontalLayout(hostname, port)));
		VerticalLayout transferLayout = new VerticalLayout(
				new HorizontalLayout(transferSyntaxComponent, transcodeOnlyUncompressedComponent));
		VerticalLayout useaetdestLayout = new VerticalLayout(new HorizontalLayout(useAETitleCheckbox));
		VerticalLayout activateLayout = new VerticalLayout(activate);

		// Set padding
		destinationLayout.setPadding(true);
		transferLayout.setPadding(true);
		useaetdestLayout.setPadding(true);
		activateLayout.setPadding(true);

		// Add components
		add(UIS.setWidthFull(new BoxShadowComponent(destinationLayout)),
				UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(transferLayout))),
				UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(useaetdestLayout))),
				UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(notificationComponent))),
				UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(tagMorphingComponent))),
				UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(deIdentificationComponent))),
				UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(filterBySOPClassesForm))),
				UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(activateLayout))),
				UIS.setWidthFull(buttonSaveDeleteCancel));

		setElements();
		setBinder();
	}

	private void setElements() {
		aeTitle.setWidth("30%");

		description.setWidth("70%");

		hostname.setWidth("70%");
		hostname.setRequired(true);

		port.setWidth("30%");
		port.addThemeVariants(TextFieldVariant.LUMO_ALIGN_RIGHT);

		UIS.setTooltip(useAETitleCheckbox,
				"if \"true\" then use the destination AETitle as the calling AETitle instead of the Forward Node AETitle");
	}

	private void setBinder() {
		binder.forField(aeTitle)
			.withValidator(StringUtils::isNotBlank, "AETitle is mandatory")
			.withValidator(value -> value.length() <= 16, "AETitle has more than 16 characters")
			.withValidator(UIS::containsNoWhitespace, "AETitle contains white spaces")
			.bind(DestinationEntity::getAeTitle, DestinationEntity::setAeTitle);

		binder.forField(description).bind(DestinationEntity::getDescription, DestinationEntity::setDescription);
		binder.forField(hostname)
			.withValidator(StringUtils::isNotBlank, "Hostname is mandatory")
			.bind(DestinationEntity::getHostname, DestinationEntity::setHostname);
		binder.forField(port)
			.withConverter(new HStringToIntegerConverter())
			.withValidator(Objects::nonNull, "Port is mandatory")
			.withValidator(value -> 1 <= value && value <= 65535, "Port should be between 1 and 65535")
			.bind(DestinationEntity::getPort, DestinationEntity::setPort);

		binder.forField(useAETitleCheckbox).bind(DestinationEntity::getUseaetdest, DestinationEntity::setUseaetdest);

		binder.forField(activate).bind(DestinationEntity::isActivate, DestinationEntity::setActivate);
		binder.bindInstanceFields(this);
	}

}
