/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
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
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.frontend.component.BoxShadowComponent;
import org.karnak.frontend.component.converter.HStringToIntegerConverter;
import org.karnak.frontend.forwardnode.edit.component.ButtonSaveDeleteCancel;
import org.karnak.frontend.util.UIS;
import org.weasis.core.util.annotations.Generated;

@Generated()
@NullUnmarked
public class FormDICOM extends VerticalLayout {

	private Binder<DestinationEntity> binder;

	private TextField aeTitle;

	private TextField description;

	private TextField hostname;

	private TextField port;

	private TextField concurrentConnections;

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
	private final ConformanceReportComponent conformanceReportComponent;

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
		this.conformanceReportComponent = new ConformanceReportComponent();
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
		conformanceReportComponent.init(this.binder);
		transferSyntaxComponent.init(this.binder);
		transcodeOnlyUncompressedComponent.init(this.binder);

		setSizeFull();
		buttonSaveDeleteCancel.getStyle().set("padding-bottom", "var(--vaadin-gap-l)");

		aeTitle = new TextField("AETitle");
		description = new TextField("Description");
		hostname = new TextField("Hostname");
		port = new TextField("Port");
		concurrentConnections = new TextField("Concurrent connections");
		useAETitleCheckbox = new Checkbox("Use AETitle destination");
		activate = new Checkbox("Enable destination");

		// Define layout
		VerticalLayout destinationLayout = new VerticalLayout(
				UIS.setWidthFull(new HorizontalLayout(aeTitle, description)), destinationCondition,
				UIS.setWidthFull(new HorizontalLayout(hostname, port, concurrentConnections)));
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
				UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(conformanceReportComponent))),
				UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(tagMorphingComponent))),
				UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(deIdentificationComponent))),
				UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(filterBySOPClassesForm))),
				UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(activateLayout))),
				UIS.setWidthFull(buttonSaveDeleteCancel));

		setElements();
		setBinder();

		// When the destination becomes virtual (report-only) the delivery fields are
		// irrelevant: disable them. Re-validate on user toggle so stale mandatory-field
		// errors clear once the fields no longer apply.
		conformanceReportComponent.getVirtualDestination().addValueChangeListener(event -> {
			updateVirtualState();
			if (event.isFromClient()) {
				binder.validate();
			}
		});
	}

	/**
	 * Enable or disable the delivery-related fields depending on whether the destination
	 * is virtual (report-only). Kept package-visible so the parent view can re-apply it
	 * after reading a bean.
	 */
	public void updateVirtualState() {
		boolean delivery = !isVirtual();
		aeTitle.setEnabled(delivery);
		hostname.setEnabled(delivery);
		port.setEnabled(delivery);
		concurrentConnections.setEnabled(delivery);
		useAETitleCheckbox.setEnabled(delivery);
		transferSyntaxComponent.setEnabled(delivery);
		notificationComponent.setEnabled(delivery);
		if (!delivery) {
			// Transcode is otherwise driven by the selected transfer syntax; only force
			// it off for a virtual destination.
			transcodeOnlyUncompressedComponent.setEnabled(false);
		}
	}

	private boolean isVirtual() {
		return Boolean.TRUE.equals(conformanceReportComponent.getVirtualDestination().getValue());
	}

	private void setElements() {
		aeTitle.setWidth("30%");

		description.setWidth("70%");

		hostname.setWidth("70%");
		hostname.setRequired(true);

		port.setWidth("30%");
		port.addThemeVariants(TextFieldVariant.ALIGN_RIGHT);

		concurrentConnections.setWidth("30%");
		concurrentConnections.addThemeVariants(TextFieldVariant.ALIGN_RIGHT);
		UIS.setTooltip(concurrentConnections,
				"Number of parallel DICOM associations to this destination (1 = single connection). "
						+ "Increase to speed up heavy multi-source forwarding, but keep it within the "
						+ "destination PACS's concurrent-association limit.");

		UIS.setTooltip(useAETitleCheckbox,
				"if \"true\" then use the destination AETitle as the calling AETitle instead of the Forward Node AETitle");
	}

	private void setBinder() {
		// A virtual (report-only) destination forwards nothing, so the delivery fields
		// are
		// not mandatory: every delivery validator is bypassed while "virtual" is checked.
		binder.forField(aeTitle)
			.withValidator(value -> isVirtual() || StringUtils.isNotBlank(value), "AETitle is mandatory")
			.withValidator(value -> isVirtual() || value.length() <= 16, "AETitle has more than 16 characters")
			.withValidator(value -> isVirtual() || UIS.containsNoWhitespace(value), "AETitle contains white spaces")
			.bind(DestinationEntity::getAeTitle, DestinationEntity::setAeTitle);

		binder.forField(description).bind(DestinationEntity::getDescription, DestinationEntity::setDescription);
		binder.forField(hostname)
			.withValidator(value -> isVirtual() || StringUtils.isNotBlank(value), "Hostname is mandatory")
			.bind(DestinationEntity::getHostname, DestinationEntity::setHostname);
		binder.forField(port)
			.withConverter(new HStringToIntegerConverter())
			.withValidator(value -> isVirtual() || Objects.nonNull(value), "Port is mandatory")
			.withValidator(value -> isVirtual() || (1 <= value && value <= 65535), "Port should be between 1 and 65535")
			.bind(DestinationEntity::getPort, DestinationEntity::setPort);

		binder.forField(concurrentConnections)
			.withConverter(new HStringToIntegerConverter())
			.withValidator(value -> isVirtual() || Objects.nonNull(value), "Concurrent connections is mandatory")
			.withValidator(value -> isVirtual() || (1 <= value && value <= 50),
					"Concurrent connections should be between 1 and 50")
			.bind(DestinationEntity::getConcurrentConnections, DestinationEntity::setConcurrentConnections);

		binder.forField(useAETitleCheckbox).bind(DestinationEntity::getUseaetdest, DestinationEntity::setUseaetdest);

		binder.forField(activate).bind(DestinationEntity::isActivate, DestinationEntity::setActivate);
		binder.bindInstanceFields(this);
	}

}
