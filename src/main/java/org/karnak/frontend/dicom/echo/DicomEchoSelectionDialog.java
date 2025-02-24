/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom.echo;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.DataProviderListener;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import java.util.ArrayList;
import java.util.List;
import org.karnak.backend.enums.MessageFormat;
import org.karnak.backend.enums.MessageLevel;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.DicomNodeList;
import org.karnak.backend.model.dicom.Message;
import org.karnak.backend.util.DicomNodeUtil;
import org.karnak.frontend.component.AbstractDialog;

public class DicomEchoSelectionDialog extends AbstractDialog {

	private static final long serialVersionUID = 1L;

	// CONTROLLER
	private final DicomEchoSelectionLogic logic = new DicomEchoSelectionLogic(this);

	// UI COMPONENTS
	private Dialog dialog;

	private Div titleBar;

	private FormLayout formLayout;

	private Select<DicomNodeList> dicomNodeTypeSelector;

	private ComboBox<ConfigNode> dicomNodeSelector;

	private HorizontalLayout buttonBar;

	private Button cancelBtn;

	private Button selectBtn;

	// DATA
	private List<DicomNodeList> dicomNodeTypes;

	private ListDataProvider<DicomNodeList> dataProviderForDicomNodeTypes;

	private DicomNodeList dicomNodes;

	private ListDataProvider<ConfigNode> dataProviderForDicomNodes;

	public DicomEchoSelectionDialog() {
		init();
		createMainLayout();
		dialog.add(mainLayout);
		selectDicomNoldeList(DicomNodeUtil.getAllDicomNodeTypesDefinedLocally());
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
		buildFormLayout();
		buildButtonBar();

		mainLayout.add(titleBar, formLayout, buttonBar);
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
	public Registration addDicomNodeSelectionListener(ComponentEventListener<DicomNodeSelectionEvent> listener) {
		return addListener(DicomNodeSelectionEvent.class, listener);
	}

	private void init() {
		dialog = this.getContent();

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

		buildDicomNodeTypeSelector();
		buildDicomNodeSelector();

		formLayout.add(dicomNodeTypeSelector, dicomNodeSelector);
	}

	private void buildDicomNodeTypeSelector() {
		dicomNodeTypeSelector = new Select<>();
		dicomNodeTypeSelector.setLabel("Dicom Nodes Type");
		dicomNodeTypeSelector.setItems(dataProviderForDicomNodeTypes);

		dicomNodeTypeSelector.addValueChangeListener((ValueChangeListener<ValueChangeEvent<DicomNodeList>>) event -> {
			DicomNodeList selectedNodeType = event.getValue();

			loadDicomNodes(selectedNodeType);
		});
	}

	private void buildDicomNodeSelector() {
		dicomNodeSelector = new ComboBox<>();
		dicomNodeSelector.setLabel("Dicom Node");
		dicomNodeSelector.setClearButtonVisible(true);
		dicomNodeSelector.setItems(dataProviderForDicomNodes);
		dicomNodeSelector.setItemLabelGenerator(item -> item.getName() + " [" + item.getAet() + " | "
				+ item.getHostname() + " | " + item.getPort() + "]");
		dicomNodeSelector.setRenderer(buildDicomNodeRenderer());
	}

	private ComponentRenderer<Div, ConfigNode> buildDicomNodeRenderer() {
		return new ComponentRenderer<>(item -> {
			Div div = new Div();
			div.getStyle().set("line-height", "92%");

			Span spanDescription = new Span(item.getName());
			spanDescription.getStyle().set("font-weight", "500");

			HtmlComponent htmlLineBreak = new HtmlComponent("BR");

			Span spanOtherAttributes = new Span(item.getAet() + " | " + item.getHostname() + " | " + item.getPort());
			spanOtherAttributes.getStyle().set("font-size", "75%");

			div.add(spanDescription, htmlLineBreak, spanOtherAttributes);

			return div;
		});
	}

	private void selectFirstItemInDicomNodeTypes() {
		if (dicomNodeTypes != null && !dicomNodeTypes.isEmpty()) {
			dicomNodeTypeSelector.setValue(dicomNodeTypes.get(0));
		}
	}

	private void selectFirstItemInDicomNodes() {
		if (dicomNodes != null && !dicomNodes.isEmpty()) {
			dicomNodeSelector.setValue(dicomNodes.get(0));
		}
	}

	private void buildButtonBar() {
		buttonBar = new HorizontalLayout();
		buttonBar.setWidthFull();
		buttonBar.setPadding(false);
		buttonBar.setSpacing(true);

		buildCancelBtn();
		buildSelectBtn();

		buttonBar.add(cancelBtn, selectBtn);
	}

	private void buildCancelBtn() {
		cancelBtn = new Button("Cancel");

		cancelBtn.addClickListener(event -> dialog.close());
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

	public static class DicomNodeSelectionEvent extends ComponentEvent<DicomEchoSelectionDialog> {

		private static final long serialVersionUID = 1L;

		private final ConfigNode selectedDicomNode;

		public DicomNodeSelectionEvent(DicomEchoSelectionDialog source, boolean fromClient,
				ConfigNode selectedDicomNode) {
			super(source, fromClient);

			this.selectedDicomNode = selectedDicomNode;
		}

		public ConfigNode getSelectedDicomNode() {
			return selectedDicomNode;
		}

	}

}
