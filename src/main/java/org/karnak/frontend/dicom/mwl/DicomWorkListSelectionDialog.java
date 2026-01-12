/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom.mwl;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.Comparator;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.DicomNodeList;
import org.karnak.frontend.component.AbstractDialog;

public class DicomWorkListSelectionDialog extends AbstractDialog {

	// CONTROLLER
	private final DicomWorkListSelectionLogic logic = new DicomWorkListSelectionLogic(this);

	// UI COMPONENTS
	private Dialog dialog;

	private Div titleBar;

	private FormLayout formLayout;

	private Select<ConfigNode> worklistNodeSelector;

	private HorizontalLayout buttonBar;

	private Button cancelBtn;

	private Button selectBtn;

	// DATA
	private DicomNodeList workListNodes;

	private ListDataProvider<ConfigNode> dataProviderForWorkListNodes;

	public DicomWorkListSelectionDialog() {
		init();
		logic.loadDicomNodeList();
		createMainLayout();
		dataProviderForWorkListNodes.refreshAll();
		selectFirstItemInWorkListNodes();
		dialog.add(mainLayout);
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

	public void loadWorkListNodes(DicomNodeList workListNodes) {
		this.workListNodes.clear();

		if (workListNodes != null && !workListNodes.isEmpty()) {
			this.workListNodes.addAll(workListNodes);
			this.workListNodes.sort(Comparator.comparing(ConfigNode::getName));
		}
	}

	// LISTENERS
	public void addWorkListSelectionListener(ComponentEventListener<WorkListSelectionEvent> listener) {
		addListener(WorkListSelectionEvent.class, listener);
	}

	private void init() {
		dialog = this.getContent();
		dialog.setWidth("500px");

		workListNodes = new DicomNodeList("Worklists");
		buildDataProvider();
	}

	private void buildDataProvider() {
		dataProviderForWorkListNodes = new ListDataProvider<>(workListNodes);

		dataProviderForWorkListNodes.addDataProviderListener(e -> selectFirstItemInWorkListNodes());
	}

	private void buildTitleBar() {
		titleBar = new Div();
		titleBar.setText("Worklist Selection");
		titleBar.getStyle().set("font-weight", "500");
	}

	private void buildFormLayout() {
		formLayout = new FormLayout();
		formLayout.setSizeFull();

		buildWorklistNodeSelector();

		formLayout.add(worklistNodeSelector);
	}

	private void buildWorklistNodeSelector() {
		worklistNodeSelector = new Select<>();
		worklistNodeSelector.setLabel("Worklist Node");
		worklistNodeSelector.setDataProvider(dataProviderForWorkListNodes);
		worklistNodeSelector.setItemLabelGenerator(item -> item.getName() + " [" + item.getAet() + " | "
				+ item.getHostname() + " | " + item.getPort() + "]");
		worklistNodeSelector.setRenderer(buildDicomNodeRenderer());
	}

	private ComponentRenderer<Div, ConfigNode> buildDicomNodeRenderer() {
		return getDivConfigNodeComponentRenderer();
	}

	@NotNull
	public static ComponentRenderer<Div, ConfigNode> getDivConfigNodeComponentRenderer() {
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

	private void selectFirstItemInWorkListNodes() {
		if (workListNodes != null && !workListNodes.isEmpty()) {
			worklistNodeSelector.setValue(workListNodes.getFirst());
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

		cancelBtn.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {

			@Override
			public void onComponentEvent(ClickEvent<Button> event) {
				dialog.close();
			}
		});
	}

	private void buildSelectBtn() {
		selectBtn = new Button("Select");
		selectBtn.addClassName("stroked-button");

		selectBtn.addClickListener(e -> {
			fireWorkListSelectionEvent();
			dialog.close();
		});
	}

	private void fireWorkListSelectionEvent() {
		ConfigNode selectedWorkList = worklistNodeSelector.getValue();
		fireEvent(new WorkListSelectionEvent(this, false, selectedWorkList));
	}

	@Getter
	public static class WorkListSelectionEvent extends ComponentEvent<DicomWorkListSelectionDialog> {

		private final ConfigNode selectedWorkList;

		public WorkListSelectionEvent(DicomWorkListSelectionDialog source, boolean fromClient,
				ConfigNode selectedWorkList) {
			super(source, fromClient);

			this.selectedWorkList = selectedWorkList;
		}

	}

}
