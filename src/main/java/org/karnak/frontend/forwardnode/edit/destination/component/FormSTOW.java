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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.dom.Style;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.frontend.component.BoxShadowComponent;
import org.karnak.frontend.extid.WarningDialog;
import org.karnak.frontend.forwardnode.edit.component.ButtonSaveDeleteCancel;
import org.karnak.frontend.kheops.SwitchingAlbumsView;
import org.karnak.frontend.util.UIS;
import org.weasis.core.util.annotations.Generated;

@Generated()
@NullUnmarked
public class FormSTOW extends VerticalLayout {

	private Binder<DestinationEntity> binder;

	private TextField description;

	private TextField url;

	private Checkbox activate;

	private Checkbox http2;

	private final DestinationCondition destinationCondition;

	@Getter
	private final FilterBySOPClassesForm filterBySOPClassesForm;

	@Getter
	private final DeIdentificationComponent deIdentificationComponent;

	@Getter
	private final TagMorphingComponent tagMorphingComponent;

	@Getter
	private final NotificationComponent notificationComponent;

	@Getter
	private final ConformanceReportComponent conformanceReportComponent;

	@Getter
	private final TransferSyntaxComponent transferSyntaxComponent;

	@Getter
	private final TranscodeOnlyUncompressedComponent transcodeOnlyUncompressedComponent;

	private SwitchingAlbumsView switchingAlbumsView;

	private Button generateAuthorizationHeaderButton;

	private TextArea headers;

	public FormSTOW() {
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
		setSizeFull();
		buttonSaveDeleteCancel.getStyle().set("padding-bottom", "var(--vaadin-gap-l)");
		this.binder = binder;
		this.deIdentificationComponent.init(this.binder);
		this.tagMorphingComponent.init(this.binder);
		this.filterBySOPClassesForm.init(this.binder);
		this.destinationCondition.init(binder);
		this.notificationComponent.init(binder);
		this.conformanceReportComponent.init(this.binder);
		this.transferSyntaxComponent.init(this.binder);
		this.transcodeOnlyUncompressedComponent.init(this.binder);

		this.description = new TextField("Description");
		this.url = new TextField("URL");
		this.generateAuthorizationHeaderButton = new Button(AuthHeadersGenerationDialog.TITLE);
		this.headers = new TextArea("Headers");
		this.switchingAlbumsView = new SwitchingAlbumsView();
		this.activate = new Checkbox("Enable destination");
		this.http2 = new Checkbox("Use HTTP/2");

		// Define layout
		VerticalLayout destinationLayout = new VerticalLayout(UIS.setWidthFull(new HorizontalLayout(description)),
				destinationCondition, UIS.setWidthFull(new HorizontalLayout(url, generateAuthorizationHeaderButton)),
				UIS.setWidthFull(headers), http2);
		VerticalLayout transferLayout = new VerticalLayout(
				new HorizontalLayout(transferSyntaxComponent, transcodeOnlyUncompressedComponent));
		VerticalLayout activateLayout = new VerticalLayout(activate);
		VerticalLayout switchingLayout = new VerticalLayout(switchingAlbumsView);

		// Set padding
		transferLayout.setPadding(true);
		destinationLayout.setPadding(true);
		activateLayout.setPadding(true);
		activateLayout.setPadding(true);

		// Add components
		add(UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(destinationLayout))));
		add(UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(transferLayout))));
		add(UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(notificationComponent))));
		add(UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(conformanceReportComponent))));
		add(UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(tagMorphingComponent))));
		add(UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(deIdentificationComponent))));
		add(UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(filterBySOPClassesForm))));
		add(UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(switchingLayout))));
		add(UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(activateLayout))));
		add(UIS.setWidthFull(buttonSaveDeleteCancel));

		setElements();
		setBinder();
		configureGenerateHeadersButton();

		// When the destination becomes virtual (report-only) the delivery fields are
		// irrelevant: disable them. Re-validate on user toggle so a stale mandatory-URL
		// error clears once the field no longer applies.
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
		url.setEnabled(delivery);
		headers.setEnabled(delivery);
		generateAuthorizationHeaderButton.setEnabled(delivery);
		http2.setEnabled(delivery);
		switchingAlbumsView.setEnabled(delivery);
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

	private void configureGenerateHeadersButton() {
		this.generateAuthorizationHeaderButton.addClickListener(e -> {
			if (this.headers.getValue().contains(AuthHeadersGenerationDialog.AUTHORIZATION_TAG)) {
				WarningDialog wd = new WarningDialog("Cannot generate Authorization Header",
						"The Headers already contain an Authorization tag. Please remove it if you want to generate it.",
						"Ok");
				wd.open();
			}
			else {
				AuthHeadersGenerationDialog dialog = new AuthHeadersGenerationDialog(this);
				dialog.open();
			}
		});
	}

	private void setElements() {
		description.setWidth("100%");

		url.setWidth("70%");
		UIS.setTooltip(url, "The destination STOW-RS URL");

		generateAuthorizationHeaderButton.setWidth("30%");
		generateAuthorizationHeaderButton.getStyle().setAlignSelf(Style.AlignSelf.FLEX_END);

		headers.setMinHeight("10em");
		headers.setWidth("100%");
		headers.getStyle().set("padding", "0px");
		UIS.setTooltip(headers,
				"Headers for HTTP request. Example of format:\n<key>Authorization</key>\n<value>Bearer 1v1pwxT4Ww4DCFzyaMt0NP</value>");

		UIS.setTooltip(http2,
				"Use HTTP/2 for STOW-RS uploads. Leave unchecked (HTTP/1.1) when the archive is behind a reverse proxy that caps HTTP/2 requests per connection (e.g. KHEOPS / nginx http2_max_requests=1000), which silently drops instances beyond the limit.");
	}

	private void setBinder() {
		binder.forField(description).bind(DestinationEntity::getDescription, DestinationEntity::setDescription);
		// A virtual (report-only) destination forwards nothing, so the URL is not
		// mandatory while "virtual" is checked.
		binder.forField(url)
			.withValidator(value -> isVirtual() || StringUtils.isNotBlank(value), "URL is mandatory")
			.bind(DestinationEntity::getUrl, DestinationEntity::setUrl);

		binder.forField(headers).bind(DestinationEntity::getHeaders, DestinationEntity::setHeaders);

		binder.forField(activate).bind(DestinationEntity::isActivate, DestinationEntity::setActivate);
		binder.forField(http2).bind(DestinationEntity::isHttp2, DestinationEntity::setHttp2);
		binder.forField(switchingAlbumsView)
			.bind(DestinationEntity::getKheopsAlbumEntities, DestinationEntity::setKheopsAlbumEntities);

		binder.bindInstanceFields(this);
	}

	public void appendToHeaders(String value) {
		String existingHeaders = this.headers.getValue();
		if (!existingHeaders.isEmpty()) {
			existingHeaders += "\n";
		}
		this.headers.setValue(existingHeaders + value);
	}

}
