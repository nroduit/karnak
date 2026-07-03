/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.List;
import lombok.Getter;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.data.entity.DicomNodeConfigEntity;
import org.weasis.core.util.annotations.Generated;

/**
 * Add/edit dialog for a single {@link DicomNodeConfigEntity}. Fires a
 * {@link SaveNodeEvent} with the entered values (a null id means "create"); persistence
 * is the caller's job.
 */
@Generated()
@NullUnmarked
public class DicomNodeEditorDialog extends Dialog {

	private final Long nodeId;

	private final TextField descriptionFld;

	private final TextField aetFld;

	private final TextField hostnameFld;

	private final PortField portFld;

	private final ComboBox<String> nodeTypeFld;

	private final ComboBox<String> nodeGroupFld;

	public DicomNodeEditorDialog(@Nullable DicomNodeConfigEntity node, List<String> nodeTypes, List<String> knownGroups,
			String defaultNodeType) {
		this.nodeId = (node != null) ? node.getId() : null;
		this.descriptionFld = new TextField("Description");
		this.aetFld = createMandatoryAetField();
		this.hostnameFld = createMandatoryField("Hostname");
		this.portFld = createPortField();
		this.nodeTypeFld = createNodeTypeField(nodeTypes);
		this.nodeGroupFld = createNodeGroupField(knownGroups);

		setHeaderTitle((node != null) ? "Edit DICOM Node" : "Add DICOM Node");

		if (node != null) {
			fillFields(node);
		}
		else {
			nodeTypeFld.setValue(defaultNodeType);
		}

		add(createFormLayout());
		addButtons();
	}

	public void addSaveNodeListener(ComponentEventListener<SaveNodeEvent> listener) {
		addListener(SaveNodeEvent.class, listener);
	}

	private static TextField createMandatoryField(String label) {
		TextField field = new TextField(label);
		field.setRequired(true);
		field.setRequiredIndicatorVisible(true);
		field.setValueChangeMode(ValueChangeMode.EAGER);
		return field;
	}

	private static AETField createMandatoryAetField() {
		AETField field = new AETField("AE Title");
		field.setRequired(true);
		field.setRequiredIndicatorVisible(true);
		field.setValueChangeMode(ValueChangeMode.EAGER);
		return field;
	}

	private static PortField createPortField() {
		PortField field = new PortField();
		field.setLabel("Port");
		field.setRequiredIndicatorVisible(true);
		field.setValueChangeMode(ValueChangeMode.EAGER);
		return field;
	}

	private static ComboBox<String> createNodeTypeField(List<String> nodeTypes) {
		ComboBox<String> comboBox = new ComboBox<>("Node Type");
		comboBox.setItems(nodeTypes);
		comboBox.setAllowCustomValue(true);
		comboBox.setRequiredIndicatorVisible(true);
		comboBox.setHelperText("what the node is used for");
		comboBox.addCustomValueSetListener(event -> comboBox.setValue(event.getDetail()));
		return comboBox;
	}

	private static ComboBox<String> createNodeGroupField(List<String> knownGroups) {
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
		formLayout.add(descriptionFld, aetFld, hostnameFld, portFld, nodeTypeFld, nodeGroupFld);
		return formLayout;
	}

	private void fillFields(DicomNodeConfigEntity node) {
		descriptionFld.setValue((node.getDescription() != null) ? node.getDescription() : "");
		aetFld.setValue((node.getAeTitle() != null) ? node.getAeTitle() : "");
		hostnameFld.setValue((node.getHostname() != null) ? node.getHostname() : "");
		portFld.setValue(node.getPort());
		nodeTypeFld.setValue((node.getNodeType() != null) ? node.getNodeType() : "");
		nodeGroupFld.setValue((node.getNodeGroup() != null) ? node.getNodeGroup() : "");
	}

	private void addButtons() {
		Button cancelBtn = new Button("Cancel", event -> close());

		Button saveBtn = new Button("Save", event -> save());
		saveBtn.addThemeVariants(ButtonVariant.PRIMARY);

		getFooter().add(cancelBtn, saveBtn);
	}

	private void save() {
		if (aetFld.isEmpty() || hostnameFld.isEmpty() || portFld.isEmpty() || nodeTypeFld.isEmpty()) {
			return;
		}

		fireEvent(new SaveNodeEvent(this, false, nodeId, emptyToNull(descriptionFld.getValue()), aetFld.getValue(),
				hostnameFld.getValue(), portFld.getValue(), nodeTypeFld.getValue(),
				emptyToNull(nodeGroupFld.getValue())));
		close();
	}

	private static @Nullable String emptyToNull(@Nullable String value) {
		return (value == null || value.isBlank()) ? null : value;
	}

	@Getter
	public static class SaveNodeEvent extends ComponentEvent<DicomNodeEditorDialog> {

		private final Long nodeId;

		private final String description;

		private final String aeTitle;

		private final String hostname;

		private final Integer port;

		private final String nodeType;

		private final String nodeGroup;

		public SaveNodeEvent(DicomNodeEditorDialog source, boolean fromClient, @Nullable Long nodeId,
				@Nullable String description, String aeTitle, String hostname, Integer port, String nodeType,
				@Nullable String nodeGroup) {
			super(source, fromClient);
			this.nodeId = nodeId;
			this.description = description;
			this.aeTitle = aeTitle;
			this.hostname = hostname;
			this.port = port;
			this.nodeType = nodeType;
			this.nodeGroup = nodeGroup;
		}

	}

}
