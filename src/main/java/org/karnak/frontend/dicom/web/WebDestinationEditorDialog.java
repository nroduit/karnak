/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom.web;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.data.entity.WebDestinationConfigEntity;
import org.karnak.backend.enums.DicomWebServiceType;
import org.karnak.backend.service.WebDestinationConfigService;
import org.weasis.core.util.annotations.Generated;

/**
 * Add/edit dialog for a single DICOMweb endpoint. Fires a {@link SaveEndpointEvent} with
 * the entered values (a null id means "create"); persistence is the caller's job.
 */
@Generated()
@NullUnmarked
public class WebDestinationEditorDialog extends Dialog {

	private final Long endpointId;

	private final TextField descriptionFld;

	private final TextField urlFld;

	private final CheckboxGroup<DicomWebServiceType> servicesFld;

	private final ComboBox<String> groupFld;

	public WebDestinationEditorDialog(@Nullable WebDestinationConfigEntity endpoint, List<String> knownGroups) {
		this.endpointId = (endpoint != null) ? endpoint.getId() : null;
		this.descriptionFld = new TextField("Description");
		this.urlFld = createUrlField();
		this.servicesFld = createServicesField();
		this.groupFld = createGroupField(knownGroups);

		setHeaderTitle((endpointId != null) ? "Edit DICOMweb Endpoint" : "Add DICOMweb Endpoint");

		if (endpoint != null) {
			fillFields(endpoint);
		}

		add(createFormLayout());
		addButtons();
	}

	public void addSaveEndpointListener(ComponentEventListener<SaveEndpointEvent> listener) {
		addListener(SaveEndpointEvent.class, listener);
	}

	private static TextField createUrlField() {
		TextField field = new TextField("DICOMweb base URL");
		field.setRequired(true);
		field.setRequiredIndicatorVisible(true);
		field.setPlaceholder("https://host:443/dicom-web");
		field.setValueChangeMode(ValueChangeMode.EAGER);
		return field;
	}

	private static CheckboxGroup<DicomWebServiceType> createServicesField() {
		CheckboxGroup<DicomWebServiceType> group = new CheckboxGroup<>("Services to probe");
		group.setItems(DicomWebServiceType.values());
		group.setItemLabelGenerator(DicomWebServiceType::getDisplayName);
		group.setHelperText("none selected means all services");
		return group;
	}

	private static ComboBox<String> createGroupField(List<String> knownGroups) {
		ComboBox<String> comboBox = new ComboBox<>("Group");
		comboBox.setItems(knownGroups);
		comboBox.setAllowCustomValue(true);
		comboBox.setClearButtonVisible(true);
		comboBox.setHelperText("optional - pick a group or type a new one");
		comboBox.addCustomValueSetListener(event -> comboBox.setValue(event.getDetail()));
		return comboBox;
	}

	private FormLayout createFormLayout() {
		FormLayout formLayout = new FormLayout();
		formLayout.add(descriptionFld, groupFld, urlFld, servicesFld);
		formLayout.setColspan(urlFld, 2);
		formLayout.setColspan(servicesFld, 2);
		return formLayout;
	}

	private void fillFields(WebDestinationConfigEntity endpoint) {
		descriptionFld.setValue((endpoint.getDescription() != null) ? endpoint.getDescription() : "");
		urlFld.setValue((endpoint.getUrl() != null) ? endpoint.getUrl() : "");
		servicesFld.setValue(WebDestinationConfigService.decodeServices(endpoint.getServices()));
		groupFld.setValue((endpoint.getGroupName() != null) ? endpoint.getGroupName() : "");
	}

	private void addButtons() {
		Button cancelBtn = new Button("Cancel", event -> close());

		Button saveBtn = new Button("Save", event -> save());
		saveBtn.addThemeVariants(ButtonVariant.PRIMARY);

		getFooter().add(cancelBtn, saveBtn);
	}

	private void save() {
		if (urlFld.isEmpty()) {
			return;
		}
		Set<DicomWebServiceType> services = EnumSet.noneOf(DicomWebServiceType.class);
		services.addAll(servicesFld.getValue());

		fireEvent(new SaveEndpointEvent(this, false, endpointId, emptyToNull(descriptionFld.getValue()),
				urlFld.getValue(), services, emptyToNull(groupFld.getValue())));
		close();
	}

	private static @Nullable String emptyToNull(@Nullable String value) {
		return (value == null || value.isBlank()) ? null : value;
	}

	@Getter
	public static class SaveEndpointEvent extends ComponentEvent<WebDestinationEditorDialog> {

		private final Long endpointId;

		private final String description;

		private final String url;

		private final transient Set<DicomWebServiceType> services;

		private final String group;

		public SaveEndpointEvent(WebDestinationEditorDialog source, boolean fromClient, @Nullable Long endpointId,
				@Nullable String description, String url, Set<DicomWebServiceType> services, @Nullable String group) {
			super(source, fromClient);
			this.endpointId = endpointId;
			this.description = description;
			this.url = url;
			this.services = services;
			this.group = group;
		}

	}

}
