/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom.echo;

import static org.karnak.frontend.dicom.mwl.DicomWorkListSelectionDialog.getDivConfigNodeComponentRenderer;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.provider.DataProviderListener;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadHandler;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.karnak.backend.enums.MessageFormat;
import org.karnak.backend.enums.MessageLevel;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.DicomNodeList;
import org.karnak.backend.model.dicom.Message;
import org.karnak.backend.util.DicomNodeUtil;
import org.karnak.frontend.component.AbstractDialog;
import org.karnak.frontend.dicom.PortField;
import org.weasis.core.util.annotations.Generated;

@Generated()
public class DicomEchoSelectionDialog extends AbstractDialog {

	// CONTROLLER
	private final DicomEchoSelectionLogic logic;

	private final DicomNodeUtil dicomNodeUtil;

	// UI COMPONENTS
	private Dialog dialog;

	private Div titleBar;

	private FormLayout groupFormLayout;

	private Select<DicomNodeList> dicomNodeTypeSelector;

	private TextField groupNameFld;

	private HorizontalLayout groupBar;

	private Button addGroupBtn;

	private Button renameGroupBtn;

	private Button deleteGroupBtn;

	private FormLayout formLayout;

	private ComboBox<ConfigNode> dicomNodeSelector;

	private TextField editNameFld;

	private TextField editAetFld;

	private TextField editHostnameFld;

	private PortField editPortFld;

	private HorizontalLayout buttonBar;

	private Button cancelBtn;

	private Button deleteBtn;

	private Button updateBtn;

	private Button selectBtn;

	private HorizontalLayout ioBar;

	private Anchor exportAnchor;

	private Upload importUpload;

	private transient UI ui;

	// DATA
	private List<DicomNodeList> dicomNodeTypes;

	private ListDataProvider<DicomNodeList> dataProviderForDicomNodeTypes;

	private DicomNodeList dicomNodes;

	private ListDataProvider<ConfigNode> dataProviderForDicomNodes;

	public DicomEchoSelectionDialog(DicomNodeUtil dicomNodeUtil) {
		this.dicomNodeUtil = dicomNodeUtil;
		this.ui = UI.getCurrent();
		this.logic = new DicomEchoSelectionLogic(this, dicomNodeUtil);
		init();
		createMainLayout();
		dialog.add(mainLayout);
		selectDicomNoldeList(dicomNodeUtil.getAllDicomNodeTypes());
	}

	public void selectDicomNoldeList(List<DicomNodeList> nodeLists) {
		try {
			this.removeMessage();
			this.loadDicomNodeTypes(nodeLists);
		}
		catch (Exception e) {
			Message message = new Message(MessageLevel.ERROR, MessageFormat.TEXT,
					"Cannot read the list of DICOM nodes");
			this.displayMessage(message);
		}
	}

	@Override
	protected void createMainLayout() {
		mainLayout = new VerticalLayout();
		mainLayout.setSizeFull();
		mainLayout.setPadding(false);
		mainLayout.setSpacing(true);

		buildTitleBar();
		buildGroupFormLayout();
		buildGroupBar();
		buildFormLayout();
		buildIoBar();
		buildButtonBar();

		mainLayout.add(titleBar, groupFormLayout, groupBar, formLayout, ioBar, buttonBar);
	}

	private void buildGroupFormLayout() {
		groupFormLayout = new FormLayout();
		groupFormLayout.setSizeFull();

		buildDicomNodeTypeSelector();

		groupNameFld = new TextField("Group name");
		groupNameFld.setPlaceholder("New or renamed group");

		groupFormLayout.add(dicomNodeTypeSelector, groupNameFld);
	}

	private void buildGroupBar() {
		groupBar = new HorizontalLayout();
		groupBar.setWidthFull();
		groupBar.setPadding(false);
		groupBar.setSpacing(true);

		addGroupBtn = new Button("Add group");
		addGroupBtn.addClickListener(event -> addGroup());

		renameGroupBtn = new Button("Rename group");
		renameGroupBtn.setEnabled(false);
		renameGroupBtn.addClickListener(event -> renameGroup());

		deleteGroupBtn = new Button("Delete group");
		deleteGroupBtn.setEnabled(false);
		deleteGroupBtn.addClickListener(event -> deleteGroup());

		groupBar.add(addGroupBtn, renameGroupBtn, deleteGroupBtn);
	}

	private String currentGroupName() {
		DicomNodeList group = dicomNodeTypeSelector.getValue();
		return group == null ? null : group.getName();
	}

	private void addGroup() {
		try {
			String name = groupNameFld.getValue();
			dicomNodeUtil.createGroup(name);
			reloadGroups(name.trim());
			displayMessage(new Message(MessageLevel.INFO, MessageFormat.TEXT, "Group \"" + name.trim() + "\" created"));
		}
		catch (Exception e) {
			displayMessage(
					new Message(MessageLevel.ERROR, MessageFormat.TEXT, "Cannot create the group: " + e.getMessage()));
		}
	}

	private void renameGroup() {
		String oldName = currentGroupName();
		if (oldName == null) {
			return;
		}
		try {
			String newName = groupNameFld.getValue();
			dicomNodeUtil.renameGroup(oldName, newName);
			reloadGroups(newName.trim());
			displayMessage(
					new Message(MessageLevel.INFO, MessageFormat.TEXT, "Group renamed to \"" + newName.trim() + "\""));
		}
		catch (Exception e) {
			displayMessage(
					new Message(MessageLevel.ERROR, MessageFormat.TEXT, "Cannot rename the group: " + e.getMessage()));
		}
	}

	private void deleteGroup() {
		String name = currentGroupName();
		if (name == null) {
			return;
		}
		try {
			int removed = dicomNodeUtil.deleteGroup(name);
			reloadGroups(null);
			displayMessage(new Message(MessageLevel.INFO, MessageFormat.TEXT,
					"Group \"" + name + "\" deleted (" + removed + " node(s) removed)"));
		}
		catch (Exception e) {
			displayMessage(
					new Message(MessageLevel.ERROR, MessageFormat.TEXT, "Cannot delete the group: " + e.getMessage()));
		}
	}

	private void reloadGroups(String groupToSelect) {
		selectDicomNoldeList(dicomNodeUtil.getAllDicomNodeTypes());
		if (groupToSelect != null) {
			dicomNodeTypes.stream()
				.filter(group -> groupToSelect.equals(group.getName()))
				.findFirst()
				.ifPresent(dicomNodeTypeSelector::setValue);
		}
	}

	private void buildIoBar() {
		ioBar = new HorizontalLayout();
		ioBar.setWidthFull();
		ioBar.setPadding(false);
		ioBar.setSpacing(true);

		exportAnchor = new Anchor();
		exportAnchor.setHref(DownloadHandler
			.fromInputStream(event -> new DownloadResponse(new ByteArrayInputStream(dicomNodeUtil.exportDicomNodes()),
					"dicom-nodes.csv", "text/csv", -1)));
		exportAnchor.getElement().setAttribute("download", true);
		exportAnchor.add(new Button("Export all nodes (CSV)"));

		importUpload = new Upload((UploadHandler) event -> {
			byte[] bytes;
			try (InputStream in = event.getInputStream()) {
				bytes = in.readAllBytes();
			}
			importNodesFromCsv(bytes);
		});
		importUpload.setDropLabel(new Span("Import nodes (CSV)"));
		importUpload.setMaxFiles(1);

		ioBar.add(exportAnchor, importUpload);
	}

	private void importNodesFromCsv(byte[] bytes) {
		try {
			int count = dicomNodeUtil.importDicomNodes(new ByteArrayInputStream(bytes), ',');
			runOnUi(() -> {
				selectDicomNoldeList(dicomNodeUtil.getAllDicomNodeTypes());
				displayMessage(
						new Message(MessageLevel.INFO, MessageFormat.TEXT, count + " DICOM node(s) imported from CSV"));
			});
		}
		catch (Exception e) {
			runOnUi(() -> displayMessage(new Message(MessageLevel.ERROR, MessageFormat.TEXT,
					"Cannot import the CSV file: " + e.getMessage())));
		}
	}

	private void runOnUi(Runnable command) {
		if (ui != null) {
			ui.access(() -> command.run());
		}
		else {
			command.run();
		}
	}

	public void open() {
		dialog.open();
	}

	public void loadDicomNodeTypes(List<DicomNodeList> dicomNodeTypes) {
		this.dicomNodeTypes.clear();

		if (dicomNodeTypes != null && !dicomNodeTypes.isEmpty()) {
			dicomNodeTypes.sort((dn1, dn2) -> dn1.getName().compareTo(dn2.getName()));
			this.dicomNodeTypes.addAll(dicomNodeTypes);
		}

		dataProviderForDicomNodeTypes.refreshAll();

		selectFirstItemInDicomNodeTypes();
	}

	public void loadDicomNodes(DicomNodeList dicomNodes) {
		this.dicomNodes.clear();

		if (dicomNodes != null && !dicomNodes.isEmpty()) {
			this.dicomNodes.addAll(dicomNodes);
			this.dicomNodes.sort((wl1, wl2) -> wl1.getName().compareTo(wl2.getName()));
		}

		dataProviderForDicomNodes.refreshAll();

		selectFirstItemInDicomNodes();
	}

	// LISTENERS
	public void addDicomNodeSelectionListener(ComponentEventListener<DicomNodeSelectionEvent> listener) {
		addListener(DicomNodeSelectionEvent.class, listener);
	}

	private void init() {
		dialog = this.getContent();
		dialog.setWidth("500px");

		dicomNodeTypes = new ArrayList<>();
		dicomNodes = new DicomNodeList("DicomNodes");

		buildDataProviders();
	}

	private void buildDataProviders() {
		dataProviderForDicomNodeTypes = new ListDataProvider<>(dicomNodeTypes);

		dataProviderForDicomNodeTypes
			.addDataProviderListener((DataProviderListener<DicomNodeList>) event -> selectFirstItemInDicomNodeTypes());

		dataProviderForDicomNodes = new ListDataProvider<>(dicomNodes);

		dataProviderForDicomNodes
			.addDataProviderListener((DataProviderListener<ConfigNode>) event -> selectFirstItemInDicomNodes());
	}

	private void buildTitleBar() {
		titleBar = new Div();
		titleBar.setText("Dicom Node Selection");
		titleBar.getStyle().set("font-weight", "500");
	}

	private void buildFormLayout() {
		formLayout = new FormLayout();
		formLayout.setSizeFull();

		buildDicomNodeSelector();
		buildEditFields();

		formLayout.add(dicomNodeSelector, editNameFld, editAetFld, editHostnameFld, editPortFld);
	}

	private void buildEditFields() {
		editNameFld = new TextField("Description");
		editAetFld = new TextField("AET");
		editHostnameFld = new TextField("Hostname");
		editPortFld = new PortField();
		editPortFld.setLabel("Port");
	}

	private void populateEditFields(ConfigNode node) {
		if (node == null) {
			editNameFld.clear();
			editAetFld.clear();
			editHostnameFld.clear();
			editPortFld.clear();
			deleteBtn.setEnabled(false);
			updateBtn.setEnabled(false);
			return;
		}

		editNameFld.setValue(node.getName());
		editAetFld.setValue(node.getAet());
		editHostnameFld.setValue(node.getHostname());
		editPortFld.setValue(node.getPort());

		boolean persisted = node.getId() != null;
		deleteBtn.setEnabled(persisted);
		updateBtn.setEnabled(persisted);
	}

	private void buildDicomNodeTypeSelector() {
		dicomNodeTypeSelector = new Select<>();
		dicomNodeTypeSelector.setLabel("Group");
		dicomNodeTypeSelector.setItems(dataProviderForDicomNodeTypes);

		dicomNodeTypeSelector.addValueChangeListener((ValueChangeListener<ValueChangeEvent<DicomNodeList>>) event -> {
			DicomNodeList selectedNodeType = event.getValue();

			loadDicomNodes(selectedNodeType);
			onGroupSelected(selectedNodeType);
		});
	}

	private void onGroupSelected(DicomNodeList group) {
		boolean hasGroup = group != null;
		// The dynamic gateway destinations group is computed on the fly and cannot be
		// renamed or deleted.
		boolean editable = hasGroup && !DicomNodeUtil.GATEWAY_DESTINATIONS_GROUP_NAME.equals(group.getName());
		if (hasGroup && groupNameFld != null) {
			groupNameFld.setValue(group.getName());
		}
		if (renameGroupBtn != null) {
			renameGroupBtn.setEnabled(editable);
		}
		if (deleteGroupBtn != null) {
			deleteGroupBtn.setEnabled(editable);
		}
	}

	private void buildDicomNodeSelector() {
		dicomNodeSelector = new ComboBox<>();
		dicomNodeSelector.setLabel("Dicom Node");
		dicomNodeSelector.setClearButtonVisible(true);
		dicomNodeSelector.setItems(dataProviderForDicomNodes);
		dicomNodeSelector.setItemLabelGenerator(item -> item.getName() + " [" + item.getAet() + " | "
				+ item.getHostname() + " | " + item.getPort() + "]");
		dicomNodeSelector.setRenderer(buildDicomNodeRenderer());

		dicomNodeSelector.addValueChangeListener(event -> populateEditFields(event.getValue()));
	}

	private ComponentRenderer<Div, ConfigNode> buildDicomNodeRenderer() {
		return getDivConfigNodeComponentRenderer();
	}

	private void selectFirstItemInDicomNodeTypes() {
		if (dicomNodeTypes != null && !dicomNodeTypes.isEmpty()) {
			dicomNodeTypeSelector.setValue(dicomNodeTypes.getFirst());
		}
	}

	private void selectFirstItemInDicomNodes() {
		if (dicomNodes != null && !dicomNodes.isEmpty()) {
			dicomNodeSelector.setValue(dicomNodes.getFirst());
		}
	}

	private void buildButtonBar() {
		buttonBar = new HorizontalLayout();
		buttonBar.setWidthFull();
		buttonBar.setPadding(false);
		buttonBar.setSpacing(true);

		buildCancelBtn();
		buildDeleteBtn();
		buildUpdateBtn();
		buildSelectBtn();

		buttonBar.add(cancelBtn, deleteBtn, updateBtn, selectBtn);
	}

	private void buildCancelBtn() {
		cancelBtn = new Button("Cancel");

		cancelBtn.addClickListener(event -> dialog.close());
	}

	private void buildDeleteBtn() {
		deleteBtn = new Button("Delete");
		deleteBtn.setEnabled(false);

		deleteBtn.addClickListener(event -> deleteSelectedNode());
	}

	private void buildUpdateBtn() {
		updateBtn = new Button("Save changes");
		updateBtn.setEnabled(false);

		updateBtn.addClickListener(event -> updateSelectedNode());
	}

	private void deleteSelectedNode() {
		ConfigNode selected = dicomNodeSelector.getValue();
		if (selected == null || selected.getId() == null) {
			return;
		}

		try {
			dicomNodeUtil.deleteDicomNode(selected.getId());
			selectDicomNoldeList(dicomNodeUtil.getAllDicomNodeTypes());
		}
		catch (Exception e) {
			displayMessage(new Message(MessageLevel.ERROR, MessageFormat.TEXT,
					"Cannot delete the DICOM node: " + e.getMessage()));
		}
	}

	private void updateSelectedNode() {
		ConfigNode selected = dicomNodeSelector.getValue();
		if (selected == null || selected.getId() == null) {
			return;
		}

		if (editAetFld.isEmpty() || editHostnameFld.isEmpty() || editPortFld.isEmpty()) {
			displayMessage(new Message(MessageLevel.WARN, MessageFormat.TEXT, "AET, Hostname and Port are required"));
			return;
		}

		try {
			dicomNodeUtil.updateDicomNode(selected.getId(), editNameFld.getValue(), editAetFld.getValue(),
					editHostnameFld.getValue(), editPortFld.getValue());
			selectDicomNoldeList(dicomNodeUtil.getAllDicomNodeTypes());
		}
		catch (Exception e) {
			displayMessage(new Message(MessageLevel.ERROR, MessageFormat.TEXT,
					"Cannot update the DICOM node: " + e.getMessage()));
		}
	}

	private void buildSelectBtn() {
		selectBtn = new Button("Select");
		selectBtn.addClassName("stroked-button");

		selectBtn.addClickListener(event -> {
			fireDicomNodeSelectionEvent();
			dialog.close();
		});
	}

	private void fireDicomNodeSelectionEvent() {
		ConfigNode selectedDicomNode = dicomNodeSelector.getValue();
		fireEvent(new DicomNodeSelectionEvent(this, false, selectedDicomNode));
	}

	@Getter
	public static class DicomNodeSelectionEvent extends ComponentEvent<DicomEchoSelectionDialog> {

		private final ConfigNode selectedDicomNode;

		public DicomNodeSelectionEvent(DicomEchoSelectionDialog source, boolean fromClient,
				ConfigNode selectedDicomNode) {
			super(source, fromClient);

			this.selectedDicomNode = selectedDicomNode;
		}

	}

}
