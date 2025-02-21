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

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.dom.Style;
import org.apache.commons.lang3.StringUtils;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.frontend.component.BoxShadowComponent;
import org.karnak.frontend.extid.WarningDialog;
import org.karnak.frontend.forwardnode.edit.component.ButtonSaveDeleteCancel;
import org.karnak.frontend.kheops.SwitchingAlbumsView;
import org.karnak.frontend.util.UIS;

public class FormSTOW extends VerticalLayout {

	private final DeIdentificationComponent deIdentificationComponent;

	private final TagMorphingComponent tagMorphingComponent;

	private Binder<DestinationEntity> binder;

	private TextField description;

	private TextField url;

	private Button generateAuthorizationHeaderButton;

	private TextArea headers;

	private final FilterBySOPClassesForm filterBySOPClassesForm;

	private SwitchingAlbumsView switchingAlbumsView;

	private Checkbox activate;

	private final DestinationCondition destinationCondition;

	private final NotificationComponent notificationComponent;

	private final TransferSyntaxComponent transferSyntaxComponent;

	private final TranscodeOnlyUncompressedComponent transcodeOnlyUncompressedComponent;

	public FormSTOW() {
		this.deIdentificationComponent = new DeIdentificationComponent();
		this.tagMorphingComponent = new TagMorphingComponent();
		this.filterBySOPClassesForm = new FilterBySOPClassesForm();
		this.destinationCondition = new DestinationCondition();
		this.notificationComponent = new NotificationComponent();
		this.transferSyntaxComponent = new TransferSyntaxComponent();
		this.transcodeOnlyUncompressedComponent = new TranscodeOnlyUncompressedComponent();
	}

	public void init(Binder<DestinationEntity> binder, ButtonSaveDeleteCancel buttonSaveDeleteCancel) {
		setSizeFull();
		this.binder = binder;
		this.deIdentificationComponent.init(this.binder);
		this.tagMorphingComponent.init(this.binder);
		this.filterBySOPClassesForm.init(this.binder);
		this.destinationCondition.init(binder);
		this.notificationComponent.init(binder);
		this.transferSyntaxComponent.init(this.binder);
		this.transcodeOnlyUncompressedComponent.init(this.binder);

		this.description = new TextField("Description");
		this.url = new TextField("URL");
		this.generateAuthorizationHeaderButton = new Button(AuthHeadersGenerationDialog.TITLE);
		this.headers = new TextArea("Headers");
		this.switchingAlbumsView = new SwitchingAlbumsView();
		this.activate = new Checkbox("Enable destination");

		// Define layout
		VerticalLayout destinationLayout = new VerticalLayout(UIS.setWidthFull(new HorizontalLayout(description)),
				destinationCondition, UIS.setWidthFull(new HorizontalLayout(url, generateAuthorizationHeaderButton)),
				UIS.setWidthFull(headers));
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
		add(UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(tagMorphingComponent))));
		add(UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(deIdentificationComponent))));
		add(UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(filterBySOPClassesForm))));
		add(UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(switchingLayout))));
		add(UIS.setWidthFull(new BoxShadowComponent(UIS.setWidthFull(activateLayout))));
		add(UIS.setWidthFull(buttonSaveDeleteCancel));

		setElements();
		setBinder();
		configureGenerateHeadersButton();
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
	}

	private void setBinder() {
		binder.forField(url)
			.withValidator(StringUtils::isNotBlank, "URL is mandatory")
			.bind(DestinationEntity::getUrl, DestinationEntity::setUrl);

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

	public DeIdentificationComponent getDeIdentificationComponent() {
		return deIdentificationComponent;
	}

	public TagMorphingComponent getTagMorphingComponent() {
		return tagMorphingComponent;
	}

	public FilterBySOPClassesForm getFilterBySOPClassesForm() {
		return filterBySOPClassesForm;
	}

	public NotificationComponent getNotificationComponent() {
		return notificationComponent;
	}

	public TransferSyntaxComponent getTransferSyntaxComponent() {
		return transferSyntaxComponent;
	}

	public TranscodeOnlyUncompressedComponent getTranscodeOnlyUncompressedComponent() {
		return transcodeOnlyUncompressedComponent;
	}

}
