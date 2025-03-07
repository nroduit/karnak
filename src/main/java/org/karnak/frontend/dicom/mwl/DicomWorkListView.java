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

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.dcm4che3.data.Attributes;
import org.karnak.backend.enums.MessageFormat;
import org.karnak.backend.enums.MessageLevel;
import org.karnak.backend.enums.Modality;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.Message;
import org.karnak.backend.model.dicom.WorkListQueryData;
import org.karnak.frontend.dicom.AbstractView;
import org.karnak.frontend.dicom.PortField;

/**
 * Calling Order 1) constructor 2) setParameter 3) beforeEnter
 */
public class DicomWorkListView extends AbstractView implements HasUrlParameter<String> {

	private static final long serialVersionUID = 1L;

	private static final String PARAMETER_CALLING_AET = "callingAET";

	private static final String PARAMETER_WORKLIST_AET = "worklistAet";

	private static final String PARAMETER_WORKLIST_HOSTNAME = "worklistHostname";

	private static final String PARAMETER_WORKLIST_PORT = "worklistPort";

	private static final String PARAMETER_ACTION = "action";

	private static final String ACTION_QUERY = "query";

	// CONTROLLER
	private final DicomWorkListLogic logic = new DicomWorkListLogic(this);

	// DIALOGS
	private DicomWorkListSelectionDialog dicomWorklistSelectionDialog;

	// UI COMPONENTS
	private VerticalLayout wlConfigurationAndQueryLayout;

	// WL Configuration
	private H6 wlConfigurationTitle;

	private FormLayout wlConfigurationForm;

	private TextField callingAetFld;

	private TextField workListAetFld;

	private TextField workListHostnameFld;

	private PortField workListPortFld;

	// WL Query
	private H6 wlQueryTitle;

	private FormLayout wlQueryForm;

	private TextField scheduledStationAetFld;

	private Select<Modality> scheduledModalitySelector;

	private TextField patientIdfld;

	private TextField admissionIdFld;

	private DatePicker scheduledFromFld;

	private DatePicker scheduledToFld;

	private TextField patientNameFld;

	private TextField accessionNumberFld;

	private HorizontalLayout buttonBar;

	private Button clearBtn;

	private Button selectWorkListBtn;

	private Button queryBtn;

	// Query Result
	private VerticalLayout queryResultLayout;

	private H6 queryResultTitle;

	private DicomWorkListGrid queryResultGrid;

	private DicomPane dicomPane;

	// DATA
	private WorkListQueryData workListQueryData;

	private Binder<WorkListQueryData> binderForWorkListQuery;

	private List<Attributes> attributesList;

	private ListDataProvider<Attributes> dataProviderForAttributes;

	// PARAMETERS
	private String callingAetParam;

	private String worklistAetParam;

	private String worklistHostnameParam;

	private String worklistPortParam;

	private String actionParam;

	public DicomWorkListView() {
		init();
		createView();
		createMainLayout();

		add(mainLayout);

		bindFields();
	}

	public void loadAttributes(List<Attributes> attributes) {
		this.attributesList.clear();
		this.attributesList.addAll(attributes);
		dataProviderForAttributes.refreshAll();

		queryResultLayout.setVisible(true);
	}

	public void openDicomPane(Attributes attributes) {
		dicomPane = new DicomPane(attributes);

		dicomPane.open();
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		readParameters(parameter);

		fillFieldsFromParameters();

		executeAction();
	}

	private void init() {
		workListQueryData = new WorkListQueryData();
		binderForWorkListQuery = new Binder<>();

		attributesList = new ArrayList<>();
		dataProviderForAttributes = new ListDataProvider<>(attributesList);
	}

	private void createView() {
		setSizeFull();
	}

	private void createMainLayout() {
		mainLayout = new VerticalLayout();
		mainLayout.setPadding(true);
		mainLayout.setSpacing(true);
		mainLayout.setWidthFull();

		buildWlConfigurationLayout();
		buildQueryResultLayout();

		mainLayout.add(wlConfigurationAndQueryLayout, queryResultLayout);
	}

	private void buildWlConfigurationLayout() {
		wlConfigurationAndQueryLayout = new VerticalLayout();
		wlConfigurationAndQueryLayout.setWidthFull();
		wlConfigurationAndQueryLayout.setPadding(true);
		wlConfigurationAndQueryLayout.setSpacing(false);
		wlConfigurationAndQueryLayout.getStyle()
			.set("box-shadow",
					"0 2px 1px -1px rgba(0,0,0,.2), 0 1px 1px 0 rgba(0,0,0,.14), 0 1px 3px 0 rgba(0,0,0,.12)");
		wlConfigurationAndQueryLayout.getStyle().set("border-radius", "4px");

		buildWlConfigurationTitle();
		buildWlConfigurationForm();
		buildWlQueryTitle();
		buildWlQueryForm();
		buildButtonBar();

		wlConfigurationAndQueryLayout.add(wlConfigurationTitle, wlConfigurationForm, wlQueryTitle, wlQueryForm,
				buttonBar);
	}

	private void buildWlConfigurationTitle() {
		wlConfigurationTitle = new H6("Worklist Configuration");
		wlConfigurationTitle.getStyle().set("margin-top", "0px");
	}

	private void buildWlConfigurationForm() {
		wlConfigurationForm = new FormLayout();

		buildCallingAETitleFld();

		buildWlAetFld();
		buildWlHostnameFld();
		buildWlPortFld();

		wlConfigurationForm.add(callingAetFld, workListAetFld, workListHostnameFld, workListPortFld);

		setFormResponsive(wlConfigurationForm);
	}

	private void buildCallingAETitleFld() {
		callingAetFld = new TextField();
		callingAetFld.setLabel("Calling AETitle");
		callingAetFld.setRequired(true);
		callingAetFld.setRequiredIndicatorVisible(true);
		callingAetFld.setValueChangeMode(ValueChangeMode.EAGER);
	}

	private void buildWlAetFld() {
		workListAetFld = new TextField("Worklist AET");
		workListAetFld.setValueChangeMode(ValueChangeMode.EAGER);
	}

	private void buildWlHostnameFld() {
		workListHostnameFld = new TextField("Worklist Hostname");
		workListHostnameFld.setValueChangeMode(ValueChangeMode.EAGER);
	}

	private void buildWlPortFld() {
		workListPortFld = new PortField();
		workListPortFld.setLabel("Worklist Port");
		workListPortFld.setValueChangeMode(ValueChangeMode.EAGER);
	}

	@SuppressWarnings("serial")
	private void buildSelectWorkListBtn() {
		selectWorkListBtn = new Button("Select Worklist");
		selectWorkListBtn.getStyle().set("cursor", "pointer");

		selectWorkListBtn.addClickListener(e -> openDicomWorklistSelectionDialog());
	}

	@SuppressWarnings("serial")
	private void openDicomWorklistSelectionDialog() {
		dicomWorklistSelectionDialog = new DicomWorkListSelectionDialog();

		dicomWorklistSelectionDialog.addWorkListSelectionListener(e -> {
			ConfigNode selectedWorkList = e.getSelectedWorkList();

			workListAetFld.setValue(selectedWorkList.getAet());
			workListHostnameFld.setValue(selectedWorkList.getHostname());
			workListPortFld.setValue(selectedWorkList.getPort());
		});

		dicomWorklistSelectionDialog.open();
	}

	private void buildWlQueryTitle() {
		wlQueryTitle = new H6("Worklist Query");
	}

	private void buildWlQueryForm() {
		wlQueryForm = new FormLayout();

		buildScheduledStationAetFld();
		buildScheduledModalitySelector();
		buildPatientIdfld();
		buildAdmissionIdFld();
		buildScheduledFromFld();
		buildScheduledToFld();
		buildPatientNameFld();
		buildAccessionNumberFld();

		wlQueryForm.add(scheduledStationAetFld, scheduledModalitySelector, patientIdfld, admissionIdFld,
				scheduledFromFld, scheduledToFld, patientNameFld, accessionNumberFld);

		setFormResponsive(wlQueryForm);
	}

	private void buildScheduledStationAetFld() {
		scheduledStationAetFld = new TextField("Scheduled Station AET");
	}

	private void buildScheduledModalitySelector() {
		scheduledModalitySelector = new Select<>();
		scheduledModalitySelector.setLabel("Scheduled Modality");
		scheduledModalitySelector.setItems(Modality.values());
		scheduledModalitySelector.setValue(Modality.ALL);
	}

	private void buildPatientIdfld() {
		patientIdfld = new TextField("Patient ID");
	}

	private void buildAdmissionIdFld() {
		admissionIdFld = new TextField("Admission ID");
	}

	private void buildScheduledFromFld() {
		scheduledFromFld = new DatePicker("Scheduled from");
	}

	private void buildScheduledToFld() {
		scheduledToFld = new DatePicker("Scheduled to");
	}

	private void buildPatientNameFld() {
		patientNameFld = new TextField("Patient Name");
	}

	private void buildAccessionNumberFld() {
		accessionNumberFld = new TextField("Accession Number");
	}

	private void buildButtonBar() {
		buttonBar = new HorizontalLayout();
		buttonBar.setWidthFull();
		buttonBar.getStyle().set("margin-top", "1em");

		buildClearBtn();
		buildSelectWorkListBtn();
		buildQueryBtn();

		buttonBar.add(clearBtn, selectWorkListBtn, queryBtn);
	}

	@SuppressWarnings("serial")
	private void buildClearBtn() {
		clearBtn = new Button("Clear");
		clearBtn.getStyle().set("cursor", "pointer");

		clearBtn.addClickListener(e -> binderForWorkListQuery.readBean(workListQueryData));
	}

	@SuppressWarnings("serial")
	private void buildQueryBtn() {
		queryBtn = new Button("Query");
		queryBtn.getStyle().set("cursor", "pointer");
		queryBtn.setEnabled(false);
		queryBtn.addClassName("stroked-button");

		queryBtn.addClickListener(e -> executeQuery());
	}

	private void buildQueryResultLayout() {
		queryResultLayout = new VerticalLayout();
		queryResultLayout.setSizeFull();
		queryResultLayout.setPadding(true);
		queryResultLayout.setSpacing(false);
		queryResultLayout.getStyle()
			.set("box-shadow",
					"0 2px 1px -1px rgba(0,0,0,.2), 0 1px 1px 0 rgba(0,0,0,.14), 0 1px 3px 0 rgba(0,0,0,.12)");
		queryResultLayout.getStyle().set("border-radius", "4px");
		queryResultLayout.setVisible(false);

		buildQueryResultTitleBar();
		buildGrid();

		queryResultLayout.add(queryResultTitle, queryResultGrid);
	}

	private void buildQueryResultTitleBar() {
		queryResultTitle = new H6("Query Result");
		queryResultTitle.getStyle().set("margin-top", "0px");
		queryResultTitle.getStyle().set("padding-bottom", "var(--lumo-space-m)");
	}

	@SuppressWarnings("serial")
	private void buildGrid() {
		queryResultGrid = new DicomWorkListGrid();
		queryResultGrid.setWidthFull();

		queryResultGrid.setDataProvider(dataProviderForAttributes);

		queryResultGrid.addItemClickListener(
				(ComponentEventListener<ItemClickEvent<Attributes>>) event -> logic.itemSelected(event.getItem()));
	}

	@SuppressWarnings("serial")
	private void bindFields() {
		binderForWorkListQuery.forField(callingAetFld)
			.asRequired("Ce champ est obligatoire")
			.bind(WorkListQueryData::getCallingAet, WorkListQueryData::setCallingAet);

		binderForWorkListQuery.forField(workListAetFld)
			.asRequired("Ce champ est obligatoire")
			.bind(WorkListQueryData::getWorkListAet, WorkListQueryData::setWorkListAet);

		binderForWorkListQuery.forField(workListHostnameFld)
			.asRequired("Ce champ est obligatoire")
			.bind(WorkListQueryData::getWorkListHostname, WorkListQueryData::setWorkListHostname);

		binderForWorkListQuery.forField(workListPortFld)
			.asRequired("Ce champ est obligatoire")
			.withValidator(new IntegerRangeValidator("Le port est invalide", 1, 65535))
			.bind(WorkListQueryData::getWorkListPort, WorkListQueryData::setWorkListPort);

		binderForWorkListQuery.bind(scheduledStationAetFld, WorkListQueryData::getScheduledStationAet,
				WorkListQueryData::setScheduledStationAet);
		binderForWorkListQuery.bind(scheduledModalitySelector, WorkListQueryData::getScheduledModality,
				WorkListQueryData::setScheduledModality);
		binderForWorkListQuery.bind(patientIdfld, WorkListQueryData::getPatientId, WorkListQueryData::setPatientId);
		binderForWorkListQuery.bind(admissionIdFld, WorkListQueryData::getAdmissionId,
				WorkListQueryData::setAdmissionId);
		binderForWorkListQuery.bind(scheduledFromFld, WorkListQueryData::getScheduledFrom,
				WorkListQueryData::setScheduledFrom);
		binderForWorkListQuery.bind(scheduledToFld, WorkListQueryData::getScheduledTo,
				WorkListQueryData::setScheduledTo);
		binderForWorkListQuery.bind(patientNameFld, WorkListQueryData::getPatientName,
				WorkListQueryData::setPatientName);
		binderForWorkListQuery.bind(accessionNumberFld, WorkListQueryData::getAccessionNumber,
				WorkListQueryData::setAccessionNumber);

		binderForWorkListQuery.readBean(workListQueryData);

		binderForWorkListQuery.addStatusChangeListener(e -> {
			if (callingAetFld.isEmpty() || workListAetFld.isEmpty() || workListHostnameFld.isEmpty()
					|| workListPortFld.isEmpty()) {
				queryBtn.setEnabled(false);
			}
			else {
				queryBtn.setEnabled(!e.hasValidationErrors());
			}

			attributesList.clear();
			dataProviderForAttributes.refreshAll();
			queryResultLayout.setVisible(false);
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
					case PARAMETER_WORKLIST_AET:
						worklistAetParam = parameterValue;
						break;
					case PARAMETER_WORKLIST_HOSTNAME:
						worklistHostnameParam = parameterValue;
						break;
					case PARAMETER_WORKLIST_PORT:
						worklistPortParam = parameterValue;
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

		if (worklistAetParam != null && !worklistAetParam.isEmpty()) {
			workListAetFld.setValue(worklistAetParam);
		}

		if (worklistHostnameParam != null && !worklistHostnameParam.isEmpty()) {
			workListHostnameFld.setValue(worklistHostnameParam);
		}

		if (worklistPortParam != null && !worklistPortParam.isEmpty()) {
			workListPortFld.setValue(Integer.valueOf(worklistPortParam));
		}
	}

	private void executeAction() {
		if (actionParam != null && actionParam.equals(ACTION_QUERY)) {
			executeQuery();
		}
	}

	private void executeQuery() {
		WorkListQueryData data = new WorkListQueryData();
		try {
			binderForWorkListQuery.writeBean(data);
			logic.query(data);
		}
		catch (ValidationException e) {
			Message message = new Message(MessageLevel.ERROR, MessageFormat.TEXT, e.getMessage());
			displayMessage(message);
		}
	}

}
