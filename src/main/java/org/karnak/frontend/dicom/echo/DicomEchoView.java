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

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.StatusChangeListener;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.enums.MessageFormat;
import org.karnak.backend.enums.MessageLevel;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.DicomEchoQueryData;
import org.karnak.backend.model.dicom.Message;
import org.karnak.backend.model.dicom.result.DicomCapabilitiesResult;
import org.karnak.backend.model.dicom.result.DicomNodeCheckHistory;
import org.karnak.backend.model.dicom.result.DicomNodeCheckResult;
import org.karnak.backend.service.dicom.DicomCapabilitiesCheckService;
import org.karnak.backend.service.dicom.DicomNodeCheckHistoryService;
import org.karnak.backend.service.dicom.DicomNodeCheckService;
import org.karnak.backend.util.DicomNodeUtil;
import org.karnak.frontend.dicom.AETField;
import org.karnak.frontend.dicom.AbstractView;
import org.karnak.frontend.dicom.DicomCapabilitiesPanel;
import org.karnak.frontend.dicom.DicomNodeCheckHistoryGrid;
import org.karnak.frontend.dicom.DicomNodeCheckResultGrid;
import org.karnak.frontend.dicom.DicomNodeSelectionDialog;
import org.karnak.frontend.dicom.DicomNodeSelectionDialog.SelectDicomNodeEvent;
import org.karnak.frontend.dicom.PortField;
import org.weasis.core.util.annotations.Generated;

/**
 * Calling Order 1) constructor 2) setParameter 3) beforeEnter
 */

@Generated()
@NullUnmarked
public class DicomEchoView extends AbstractView implements HasUrlParameter<String> {

	public static final String VIEW_NAME = "Dicom Echo";

	private static final String PARAMETER_CALLING_AET = "callingAET";

	private static final String PARAMETER_CALLED_AET = "calledAET";

	private static final String PARAMETER_CALLED_HOSTNAME = "calledHostname";

	private static final String PARAMETER_CALLED_PORT = "calledPort";

	private static final String PARAMETER_ACTION = "action";

	private static final String ACTION_ECHO = "echo";

	public static final String ERROR_MESSAGE = "This filed is mandatory";

	// CONTROLLER
	private final DicomEchoLogic logic;

	// UI COMPONENTS
	private VerticalLayout dicomEchoQueryLayout;

	private FormLayout formLayout;

	private H6 formLayoutTitle;

	private TextField callingAetFld;

	private TextField calledAetFld;

	private TextField calledHostnameFld;

	private PortField calledPortFld;

	private HorizontalLayout buttonBar;

	private Button clearBtn;

	private Button selectDicomNodeBtn;

	private Button dicomEchoBtn;

	// Current check result (single row, with an on-demand capabilities probe)
	private VerticalLayout currentResultLayout;

	private DicomNodeCheckResultGrid currentResultGrid;

	// Persisted check history, detached from the current result
	private VerticalLayout historyLayout;

	private H6 historyTitle;

	private DicomNodeCheckHistoryGrid historyGrid;

	// DATA
	private DicomEchoQueryData dicomEchoQueryData;

	private Binder<DicomEchoQueryData> binder;

	// Number of persisted checks currently shown; drives history-layout visibility so a
	// field edit does not hide the existing history.
	private int historyCount;

	// PARAMETERS
	private String callingAetParam;

	private String dicomNodeAetParam;

	private String dicomNodeHostnameParam;

	private String dicomNodePortParam;

	private String actionParam;

	private final DicomNodeUtil dicomNodeUtil;

	private final DicomNodeCheckHistoryService historyService;

	public DicomEchoView(DicomNodeUtil dicomNodeUtil, DicomNodeCheckService dicomNodeCheckService,
			DicomCapabilitiesCheckService dicomCapabilitiesCheckService, DicomNodeCheckHistoryService historyService) {
		this.dicomNodeUtil = dicomNodeUtil;
		this.historyService = historyService;
		this.logic = new DicomEchoLogic(this, dicomNodeCheckService, dicomCapabilitiesCheckService);
		init();
		createView();
		createMainLayout();

		add(mainLayout);

		bindFields();

		// Show the persisted history straight away, before any check has been run.
		refreshHistory();
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		readParameters(parameter);

		fillFieldsFromParameters();

		executeAction();
	}

	public void displayResult(DicomNodeCheckResult result) {
		currentResultGrid.setItems(List.of(result));
		currentResultLayout.setVisible(true);

		historyService.record(result);
		refreshHistory();
	}

	/**
	 * Called when the Dicom Echo tab is (re)selected: reload the persisted history so
	 * checks run in another session are reflected without a page reload.
	 */
	public void onSelected() {
		refreshHistory();
	}

	private void refreshHistory() {
		List<DicomNodeCheckHistory> history = historyService.getRecentChecks();
		historyCount = history.size();
		historyGrid.setItems(history);
		historyTitle.setText("History (" + historyCount + ")");
		historyLayout.setVisible(historyCount > 0);
	}

	private void init() {
		dicomEchoQueryData = new DicomEchoQueryData();
		binder = new Binder<>();
	}

	private void createView() {
		setSizeFull();
		getStyle().set("min-height", "0");
	}

	private void createMainLayout() {
		mainLayout = new VerticalLayout();
		mainLayout.setPadding(true);
		mainLayout.setSpacing(true);
		mainLayout.setSizeFull();
		mainLayout.getStyle().set("min-height", "0");

		buildDicomEchoQueryLayout();
		buildCurrentResultLayout();
		buildHistoryLayout();

		mainLayout.add(dicomEchoQueryLayout, currentResultLayout, historyLayout);

		// Let the history section absorb the remaining vertical space; the grid inside
		// it scrolls internally when the checks exceed the available height.
		mainLayout.setFlexGrow(1, historyLayout);
	}

	private void buildDicomEchoQueryLayout() {
		dicomEchoQueryLayout = new VerticalLayout();
		dicomEchoQueryLayout.setWidthFull();
		dicomEchoQueryLayout.setPadding(true);
		dicomEchoQueryLayout.setSpacing(false);
		dicomEchoQueryLayout.getStyle()
			.set("box-shadow",
					"0 2px 1px -1px rgba(0,0,0,.2), 0 1px 1px 0 rgba(0,0,0,.14), 0 1px 3px 0 rgba(0,0,0,.12)");
		dicomEchoQueryLayout.getStyle().set("border-radius", "4px");

		buildFormLayoutTitle();
		buildFormLayout();
		buildButtonBar();

		dicomEchoQueryLayout.add(formLayoutTitle, formLayout, buttonBar);
	}

	private void buildFormLayoutTitle() {
		formLayoutTitle = new H6("Dicom Echo");
		formLayoutTitle.getStyle().set("margin-top", "0px");
	}

	private void buildFormLayout() {
		formLayout = new FormLayout();

		buildCallingAetFld();
		buildCalledAetFld();
		buildCalledHostnameFld();
		buildCalledPortFld();

		formLayout.add(callingAetFld, calledAetFld, calledHostnameFld, calledPortFld);

		setFormResponsive(formLayout);
	}

	private void buildCallingAetFld() {
		callingAetFld = new AETField();
		callingAetFld.setLabel("Calling AE Title");
		callingAetFld.setRequired(true);
		callingAetFld.setRequiredIndicatorVisible(true);
		callingAetFld.setValueChangeMode(ValueChangeMode.EAGER);
	}

	private void buildCalledAetFld() {
		calledAetFld = new AETField("Called AE Title");
		calledAetFld.setValueChangeMode(ValueChangeMode.EAGER);
	}

	private void buildCalledHostnameFld() {
		calledHostnameFld = new TextField("Called Hostname");
		calledHostnameFld.setValueChangeMode(ValueChangeMode.EAGER);
	}

	private void buildCalledPortFld() {
		calledPortFld = new PortField();
		calledPortFld.setLabel("Called Port");
		calledPortFld.setValueChangeMode(ValueChangeMode.EAGER);
	}

	private void buildButtonBar() {
		buttonBar = new HorizontalLayout();
		buttonBar.setWidthFull();
		buttonBar.setPadding(false);
		buttonBar.setMargin(false);
		buttonBar.getStyle().set("margin-top", "1em");

		buildClearBtn();
		buildSelectDicomNodeBtn();
		buildDicomEchoBtn();

		buttonBar.add(clearBtn, selectDicomNodeBtn, dicomEchoBtn);
	}

	private void buildClearBtn() {
		clearBtn = new Button("Reset Form");
		clearBtn.getStyle().set("cursor", "pointer");

		clearBtn.addClickListener(event -> binder.readBean(dicomEchoQueryData));
	}

	private void buildSelectDicomNodeBtn() {
		selectDicomNodeBtn = new Button("Select DICOM Node");
		selectDicomNodeBtn.getStyle().set("cursor", "pointer");

		selectDicomNodeBtn.addClickListener(event -> openDicomNodeSelectionDialog());
	}

	private void openDicomNodeSelectionDialog() {
		DicomNodeSelectionDialog dialog = new DicomNodeSelectionDialog(dicomNodeUtil.getAllNodeTypesIncludingWorklist(),
				"Select DICOM Node", "DICOM nodes");

		dialog.addSelectDicomNodeListener((ComponentEventListener<SelectDicomNodeEvent>) event -> {
			ConfigNode selectedDicomNode = event.getSelectedDicomNode();

			calledAetFld.setValue(selectedDicomNode.getAet());
			calledHostnameFld.setValue(selectedDicomNode.getHostname());
			calledPortFld.setValue(selectedDicomNode.getPort());
		});

		dialog.open();
	}

	private void buildDicomEchoBtn() {
		dicomEchoBtn = new Button("Check DICOM Node");
		dicomEchoBtn.getStyle().set("cursor", "pointer");
		dicomEchoBtn.addThemeVariants(ButtonVariant.PRIMARY);
		dicomEchoBtn.setEnabled(false);

		dicomEchoBtn.addClickListener(event -> executeEcho());
	}

	private void buildCurrentResultLayout() {
		currentResultLayout = new VerticalLayout();
		currentResultLayout.setWidthFull();
		currentResultLayout.setPadding(true);
		currentResultLayout.setSpacing(false);
		currentResultLayout.getStyle()
			.set("box-shadow",
					"0 2px 1px -1px rgba(0,0,0,.2), 0 1px 1px 0 rgba(0,0,0,.14), 0 1px 3px 0 rgba(0,0,0,.12)");
		currentResultLayout.getStyle().set("border-radius", "4px");
		currentResultLayout.setVisible(false);

		H6 resultTitle = new H6("Result");
		resultTitle.getStyle().set("margin-top", "0px");
		resultTitle.getStyle().set("padding-bottom", "0px");

		Div resultNote = new Div("Select the row to view the details, or probe the node capabilities");
		resultNote.getStyle().set("font-size", "var(--aura-font-size-xs)");
		resultNote.getStyle().set("font-style", "italic");

		currentResultGrid = new DicomNodeCheckResultGrid();
		currentResultGrid.setWidthFull();
		currentResultGrid.setAllRowsVisible(true);
		currentResultGrid.setCapabilityProbeAction(this::openCapabilitiesDialog);

		currentResultLayout.add(resultTitle, resultNote, currentResultGrid);
	}

	private void buildHistoryLayout() {
		historyLayout = new VerticalLayout();
		historyLayout.setWidthFull();
		historyLayout.setPadding(true);
		historyLayout.setSpacing(false);
		historyLayout.getStyle()
			.set("box-shadow",
					"0 2px 1px -1px rgba(0,0,0,.2), 0 1px 1px 0 rgba(0,0,0,.14), 0 1px 3px 0 rgba(0,0,0,.12)");
		historyLayout.getStyle().set("border-radius", "4px");
		// Let this section shrink so the grid inside it can flex-shrink down to its
		// min-height instead of forcing the page to scroll while the table is still tall.
		historyLayout.getStyle().set("min-height", "0");
		historyLayout.setVisible(false);

		historyTitle = new H6("History");
		historyTitle.getStyle().set("margin-top", "0px");
		historyTitle.getStyle().set("padding-bottom", "0px");

		Div historyNote = new Div("Previous checks, most recent first - select a row to view the details");
		historyNote.getStyle().set("font-size", "var(--aura-font-size-xs)");
		historyNote.getStyle().set("font-style", "italic");

		historyGrid = new DicomNodeCheckHistoryGrid();
		historyGrid.setWidthFull();
		historyGrid.setMinHeight("200px");

		historyLayout.add(historyTitle, historyNote, historyGrid);

		// Grow the grid to fill whatever height the history section receives.
		historyLayout.setFlexGrow(1, historyGrid);
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

	private void bindFields() {
		binder.forField(callingAetFld)
			.asRequired(ERROR_MESSAGE)
			.bind(DicomEchoQueryData::getCallingAet, DicomEchoQueryData::setCallingAet);

		binder.forField(calledAetFld)
			.asRequired(ERROR_MESSAGE)
			.bind(DicomEchoQueryData::getCalledAet, DicomEchoQueryData::setCalledAet);

		binder.forField(calledHostnameFld)
			.asRequired(ERROR_MESSAGE)
			.bind(DicomEchoQueryData::getCalledHostname, DicomEchoQueryData::setCalledHostname);

		binder.forField(calledPortFld)
			.asRequired(ERROR_MESSAGE)
			.withValidator(new IntegerRangeValidator("Invalid port number", 1, 65535))
			.bind(DicomEchoQueryData::getCalledPort, DicomEchoQueryData::setCalledPort);

		binder.readBean(dicomEchoQueryData);

		binder.addStatusChangeListener((StatusChangeListener) event -> {
			if (callingAetFld.isEmpty() || calledAetFld.isEmpty() || calledHostnameFld.isEmpty()
					|| calledPortFld.isEmpty()) {
				dicomEchoBtn.setEnabled(false);
			}
			else {
				dicomEchoBtn.setEnabled(!event.hasValidationErrors());
			}

			currentResultLayout.setVisible(false);
			historyLayout.setVisible(historyCount > 0);
		});
	}

	private void readParameters(String queryParameter) {
		if (queryParameter != null && !queryParameter.trim().isEmpty()) {

			String queryParameterDecoded = URLDecoder.decode(queryParameter, StandardCharsets.UTF_8);

			String[] parametersArray = queryParameterDecoded.split("&");

			for (String parameter : parametersArray) {
				String[] parameterArray = parameter.split("=");
				String parameterName = parameterArray[0];
				String parameterValue = parameterArray[1];

				switch (parameterName) {
					case PARAMETER_CALLING_AET:
						callingAetParam = parameterValue;
						break;
					case PARAMETER_CALLED_AET:
						dicomNodeAetParam = parameterValue;
						break;
					case PARAMETER_CALLED_HOSTNAME:
						dicomNodeHostnameParam = parameterValue;
						break;
					case PARAMETER_CALLED_PORT:
						dicomNodePortParam = parameterValue;
						break;
					case PARAMETER_ACTION:
						actionParam = parameterValue;
						break;
					default:
						break;
				}
			}
		}
	}

	private void fillFieldsFromParameters() {
		if (callingAetParam != null && !callingAetParam.isEmpty()) {
			callingAetFld.setValue(callingAetParam);
		}

		if (dicomNodeAetParam != null && !dicomNodeAetParam.isEmpty()) {
			calledAetFld.setValue(dicomNodeAetParam);
		}

		if (dicomNodeHostnameParam != null && !dicomNodeHostnameParam.isEmpty()) {
			calledHostnameFld.setValue(dicomNodeHostnameParam);
		}

		if (dicomNodePortParam != null && !dicomNodePortParam.isEmpty()) {
			calledPortFld.setValue(Integer.valueOf(dicomNodePortParam));
		}
	}

	private void executeAction() {
		if (actionParam != null) {
			if (actionParam.equals(ACTION_ECHO)) {
				executeEcho();
			}
		}
	}

	private void executeEcho() {
		DicomEchoQueryData data = new DicomEchoQueryData();
		try {
			binder.writeBean(data);
			logic.dicomEcho(data);
		}
		catch (ValidationException e) {
			Message message = new Message(MessageLevel.ERROR, MessageFormat.TEXT, e.getMessage());
			displayMessage(message);
		}
	}

}