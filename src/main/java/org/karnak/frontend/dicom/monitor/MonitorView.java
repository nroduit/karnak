/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom.monitor;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.WebDestinationConfigEntity;
import org.karnak.backend.enums.MessageFormat;
import org.karnak.backend.enums.MessageLevel;
import org.karnak.backend.model.dicom.DicomNodeList;
import org.karnak.backend.model.dicom.Message;
import org.karnak.backend.model.dicom.WebDestinationNode;
import org.karnak.backend.model.dicom.result.DicomCapabilitiesResult;
import org.karnak.backend.model.dicom.result.DicomNodeCheckResult;
import org.karnak.backend.model.dicom.result.WebNodeCheckResult;
import org.karnak.backend.service.WebDestinationConfigService;
import org.karnak.backend.service.dicom.DicomCapabilitiesCheckService;
import org.karnak.backend.service.dicom.DicomNodeCheckService;
import org.karnak.backend.service.dicom.DicomWebCheckService;
import org.karnak.backend.util.DicomNodeUtil;
import org.karnak.frontend.dicom.AETField;
import org.karnak.frontend.dicom.AbstractView;
import org.karnak.frontend.dicom.DicomCapabilitiesPanel;
import org.karnak.frontend.dicom.DicomNodeCheckResultGrid;
import org.karnak.frontend.dicom.WebDestinationCheckResultGrid;
import org.weasis.core.util.annotations.Generated;

@Generated()
@NullUnmarked
public class MonitorView extends AbstractView {

	private static final String DEFAULT_CALLING_AET = "PACSMONITOR";

	// CONTROLLER
	private final MonitorLogic logic;

	// UI COMPONENTS
	private VerticalLayout dicomLayout;

	// Dicom Layout
	private HorizontalLayout dicomEchoLayout;

	private H6 dicomEchoLayoutTitle;

	private Select<DicomNodeList> dicomEchoNodeListSelector;

	private TextField callingAetFld;

	private Button dicomEchoBtn;

	// Result Layout
	private VerticalLayout resultLayout;

	private H6 resultTitle;

	private Div resultNote;

	private DicomNodeCheckResultGrid resultGrid;

	// DICOMweb Layout
	private VerticalLayout webLayout;

	private ComboBox<String> webGroupFld;

	private Button webBtn;

	private Div webNote;

	private WebDestinationCheckResultGrid webGrid;

	private final DicomNodeUtil dicomNodeUtil;

	private final transient WebDestinationConfigService webDestinationConfigService;

	public MonitorView(DicomNodeUtil dicomNodeUtil, WebDestinationConfigService webDestinationConfigService,
			DicomNodeCheckService dicomNodeCheckService, DicomCapabilitiesCheckService dicomCapabilitiesCheckService,
			DicomWebCheckService dicomWebCheckService) {
		this.dicomNodeUtil = dicomNodeUtil;
		this.webDestinationConfigService = webDestinationConfigService;
		this.logic = new MonitorLogic(this, dicomNodeCheckService, dicomCapabilitiesCheckService, dicomWebCheckService);
		createView();
		createMainLayout();

		add(mainLayout);
	}

	public void displayResults(List<DicomNodeCheckResult> results) {
		resultGrid.setItems(results);
		resultNote.setText(results.size() + " node(s) checked - select a row to view the details");
		resultLayout.setVisible(true);
	}

	public void displayWebResults(List<WebNodeCheckResult> results) {
		webGrid.setItems(results);
		webNote.setText(results.size() + " DICOMweb destination(s) checked - select a row to view the details");
		webGrid.setVisible(true);
	}

	private void createView() {
		setSizeFull();
	}

	private void createMainLayout() {
		mainLayout = new VerticalLayout();
		mainLayout.setPadding(true);
		mainLayout.setSpacing(true);
		mainLayout.setWidthFull();

		buildDicomLayout();
		buildResultLayout();
		buildWebLayout();

		// A second tab line switches between the two monitoring sections (DICOM nodes vs
		// DICOMweb) instead of stacking them on the page.
		VerticalLayout dicomSection = sectionWrapper(dicomLayout, resultLayout);
		VerticalLayout webSection = sectionWrapper(webLayout);
		webSection.setVisible(false);

		Tab tabDicomNodes = new Tab("DICOM Nodes");
		Tab tabDicomWeb = new Tab("DICOMweb");
		Tabs sectionTabs = new Tabs(tabDicomNodes, tabDicomWeb);
		sectionTabs.setWidthFull();
		sectionTabs.addSelectedChangeListener(event -> {
			boolean dicomSelected = event.getSelectedTab() == tabDicomNodes;
			dicomSection.setVisible(dicomSelected);
			webSection.setVisible(!dicomSelected);
		});

		mainLayout.add(sectionTabs, dicomSection, webSection);
	}

	private static VerticalLayout sectionWrapper(Component... children) {
		VerticalLayout wrapper = new VerticalLayout(children);
		wrapper.setWidthFull();
		wrapper.setPadding(false);
		wrapper.setSpacing(true);
		return wrapper;
	}

	private void buildDicomEchoLayoutTitle() {
		dicomEchoLayoutTitle = new H6("Dicom Echo");
		dicomEchoLayoutTitle.getStyle().set("margin-top", "0px");
	}

	private void buildDicomEchoLayout() {
		dicomEchoLayout = new HorizontalLayout();
		dicomEchoLayout.setMargin(false);
		dicomEchoLayout.setSpacing(true);
		dicomEchoLayout.setWidthFull();
		dicomEchoLayout.setDefaultVerticalComponentAlignment(Alignment.END);

		buildDicomNodeListSelector();
		buildCallingAetFld();
		buildDicomEchoBtn();

		dicomEchoLayout.add(dicomEchoNodeListSelector, callingAetFld, dicomEchoBtn);
	}

	private void buildDicomNodeListSelector() {
		dicomEchoNodeListSelector = new Select<>();
		dicomEchoNodeListSelector.setLabel("Group");
		dicomEchoNodeListSelector.setEmptySelectionAllowed(false);

		var dicomNodeTypes = dicomNodeUtil.getAllNodeTypesIncludingWorklist();

		dicomEchoNodeListSelector.setItems(dicomNodeTypes);

		dicomEchoNodeListSelector
			.addValueChangeListener((ValueChangeListener<ValueChangeEvent<DicomNodeList>>) event -> logic
				.dicomNodeListSelected(event.getValue()));

		if (!dicomNodeTypes.isEmpty()) {
			dicomEchoNodeListSelector.setValue(dicomNodeTypes.getFirst());
		}
	}

	private void buildCallingAetFld() {
		callingAetFld = new AETField("Calling AE Title");
		callingAetFld.setRequired(true);
		callingAetFld.setRequiredIndicatorVisible(true);
		callingAetFld.setValueChangeMode(ValueChangeMode.EAGER);
		callingAetFld.setValue(DEFAULT_CALLING_AET);
	}

	private void buildDicomEchoBtn() {
		dicomEchoBtn = new Button("Check DICOM Nodes");
		dicomEchoBtn.addThemeVariants(ButtonVariant.PRIMARY);
		dicomEchoBtn.addClickListener(event -> runCheck());
	}

	private void runCheck() {
		if (callingAetFld.isEmpty()) {
			displayMessage(new Message(MessageLevel.WARN, MessageFormat.TEXT, "A calling AE Title is required"));
			return;
		}

		DicomNodeList selected = dicomEchoNodeListSelector.getValue();
		if (selected == null || selected.isEmpty()) {
			displayMessage(new Message(MessageLevel.WARN, MessageFormat.TEXT,
					"The selected group has no DICOM node to check"));
			return;
		}

		logic.dicomEcho(callingAetFld.getValue());
	}

	private void buildDicomLayout() {
		dicomLayout = new VerticalLayout();
		dicomLayout.setWidthFull();
		dicomLayout.setPadding(true);
		dicomLayout.setSpacing(false);
		dicomLayout.getStyle()
			.set("box-shadow",
					"0 2px 1px -1px rgba(0,0,0,.2), 0 1px 1px 0 rgba(0,0,0,.14), 0 1px 3px 0 rgba(0,0,0,.12)");
		dicomLayout.getStyle().set("border-radius", "4px");

		buildDicomEchoLayoutTitle();
		buildDicomEchoLayout();

		dicomLayout.add(dicomEchoLayoutTitle, dicomEchoLayout);
	}

	private void buildResultLayout() {
		resultLayout = new VerticalLayout();
		resultLayout.setWidthFull();
		resultLayout.setPadding(true);
		resultLayout.setSpacing(false);
		resultLayout.getStyle()
			.set("box-shadow",
					"0 2px 1px -1px rgba(0,0,0,.2), 0 1px 1px 0 rgba(0,0,0,.14), 0 1px 3px 0 rgba(0,0,0,.12)");
		resultLayout.getStyle().set("border-radius", "4px");
		resultLayout.setVisible(false);

		buildResultTitle();
		buildResultNote();
		buildResultGrid();

		resultLayout.add(resultTitle, resultNote, resultGrid);
	}

	private void buildResultTitle() {
		resultTitle = new H6("Result");
		resultTitle.getStyle().set("margin-top", "0px");
		resultTitle.getStyle().set("padding-bottom", "0px");
	}

	private void buildResultNote() {
		resultNote = new Div();
		resultNote.getStyle().set("font-size", "var(--aura-font-size-xs)");
		resultNote.getStyle().set("font-style", "italic");
	}

	private void buildResultGrid() {
		resultGrid = new DicomNodeCheckResultGrid();
		resultGrid.setWidthFull();
		resultGrid.setAllRowsVisible(true);
		resultGrid.setCapabilityProbeAction(this::openCapabilitiesDialog);
	}

	private void buildWebLayout() {
		webLayout = new VerticalLayout();
		webLayout.setWidthFull();
		webLayout.setPadding(true);
		webLayout.setSpacing(false);
		webLayout.getStyle()
			.set("box-shadow",
					"0 2px 1px -1px rgba(0,0,0,.2), 0 1px 1px 0 rgba(0,0,0,.14), 0 1px 3px 0 rgba(0,0,0,.12)");
		webLayout.getStyle().set("border-radius", "4px");

		H6 webTitle = new H6("DICOMweb destinations (STOW-RS)");
		webTitle.getStyle().set("margin-top", "0px");

		webGroupFld = new ComboBox<>("Group");
		var webGroups = new ArrayList<String>();
		webGroups.add(DicomNodeUtil.GATEWAY_DESTINATIONS_GROUP_NAME);
		webGroups.addAll(webDestinationConfigService.getKnownGroups());
		webGroupFld.setItems(webGroups);
		webGroupFld.setPlaceholder("All groups");
		webGroupFld.setClearButtonVisible(true);
		webGroupFld.setHelperText("empty = all groups");
		// Only show the Group filter when there is more than one group to choose from;
		// with a single group an empty selection already checks everything.
		webGroupFld.setVisible(webGroups.size() > 1);

		webBtn = new Button("Check DICOMweb", (event) -> runWebCheck());
		webBtn.addThemeVariants(ButtonVariant.PRIMARY);

		HorizontalLayout webBar = new HorizontalLayout(webGroupFld, webBtn);
		// Align on the input baseline so the button lines up with the combo's field
		// rather than dropping to the level of its helper text.
		webBar.setDefaultVerticalComponentAlignment(Alignment.BASELINE);
		webBar.setSpacing(true);

		webNote = new Div();
		webNote.getStyle().set("font-size", "var(--aura-font-size-xs)");
		webNote.getStyle().set("font-style", "italic");

		webGrid = new WebDestinationCheckResultGrid();
		webGrid.setWidthFull();
		webGrid.setAllRowsVisible(true);
		webGrid.setVisible(false);

		webLayout.add(webTitle, webBar, webNote, webGrid);
	}

	private void runWebCheck() {
		List<WebDestinationNode> destinations = collectWebDestinations();
		if (destinations.isEmpty()) {
			displayMessage(new Message(MessageLevel.WARN, MessageFormat.TEXT,
					"No DICOMweb (STOW-RS) destination is configured"));
			return;
		}

		displayWebResults(logic.checkWebDestinations(destinations));
	}

	/**
	 * The DICOMweb destinations to check for the selected group, deduplicated by URL: the
	 * managed endpoints (filtered by group) and, unless a specific managed group is
	 * selected, the dynamic Gateway STOW destinations (their own
	 * {@value DicomNodeUtil#GATEWAY_DESTINATIONS_GROUP_NAME} group). An empty selection
	 * checks everything.
	 */
	private List<WebDestinationNode> collectWebDestinations() {
		String group = webGroupFld.getValue();
		boolean gatewayGroup = DicomNodeUtil.GATEWAY_DESTINATIONS_GROUP_NAME.equals(group);

		var destinations = new ArrayList<WebDestinationNode>();
		Set<String> seen = new HashSet<>();
		if (!gatewayGroup) {
			for (WebDestinationConfigEntity entity : webDestinationConfigService.findAll(group)) {
				if (seen.add(entity.getUrl())) {
					destinations.add(webDestinationConfigService.toWebDestinationNode(entity));
				}
			}
		}
		if (group == null || gatewayGroup) {
			for (WebDestinationNode node : dicomNodeUtil.getGatewayStowDestinations()) {
				if (seen.add(node.url())) {
					destinations.add(node);
				}
			}
		}
		return destinations;
	}

	private void openCapabilitiesDialog(DicomNodeCheckResult result) {
		if (callingAetFld.isEmpty()) {
			displayMessage(new Message(MessageLevel.WARN, MessageFormat.TEXT, "A calling AE Title is required"));
			return;
		}

		DicomCapabilitiesResult capabilities = logic.probeCapabilities(callingAetFld.getValue(),
				result.getCalledNode());

		DicomCapabilitiesPanel panel = new DicomCapabilitiesPanel();
		panel.display(capabilities);

		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("DICOM Capabilities — " + result.getCalledNodeDescription());
		dialog.setWidth("900px");
		dialog.add(panel);
		Button closeBtn = new Button("Close", (event) -> dialog.close());
		dialog.getFooter().add(closeBtn);
		dialog.open();
	}

}