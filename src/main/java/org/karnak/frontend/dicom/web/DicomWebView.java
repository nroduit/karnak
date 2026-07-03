/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom.web;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.WebDestinationConfigEntity;
import org.karnak.backend.enums.DicomWebServiceType;
import org.karnak.backend.enums.MessageFormat;
import org.karnak.backend.enums.MessageLevel;
import org.karnak.backend.model.dicom.Message;
import org.karnak.backend.model.dicom.WebDestinationNode;
import org.karnak.backend.model.dicom.result.WebDestinationCheckResult;
import org.karnak.backend.model.dicom.result.WebNodeCheckResult;
import org.karnak.backend.service.WebDestinationConfigService;
import org.karnak.backend.service.dicom.DicomWebCheckService;
import org.karnak.backend.util.DicomNodeUtil;
import org.karnak.frontend.dicom.AbstractView;
import org.karnak.frontend.dicom.WebDestinationCheckResultGrid;
import org.weasis.core.util.annotations.Generated;

@Generated()
@NullUnmarked
public class DicomWebView extends AbstractView {

	private final transient DicomWebCheckService dicomWebCheckService;

	private final transient WebDestinationConfigService webDestinationConfigService;

	private final transient DicomNodeUtil dicomNodeUtil;

	private ComboBox<String> groupFilterFld;

	private ComboBox<WebDestinationConfigEntity> savedEndpointFld;

	private transient List<WebDestinationConfigEntity> allEndpoints = List.of();

	private TextField urlFld;

	private CheckboxGroup<DicomWebServiceType> servicesFld;

	private Button checkBtn;

	private Button checkGroupBtn;

	private Button saveBtn;

	private VerticalLayout resultLayout;

	private Div resultNote;

	private WebDestinationCheckResultGrid resultGrid;

	public DicomWebView(DicomWebCheckService dicomWebCheckService,
			WebDestinationConfigService webDestinationConfigService, DicomNodeUtil dicomNodeUtil) {
		this.dicomWebCheckService = dicomWebCheckService;
		this.webDestinationConfigService = webDestinationConfigService;
		this.dicomNodeUtil = dicomNodeUtil;
		setSizeFull();
		createMainLayout();
		add(mainLayout);
	}

	private void createMainLayout() {
		mainLayout = new VerticalLayout();
		mainLayout.setPadding(true);
		mainLayout.setSpacing(true);
		mainLayout.setWidthFull();

		mainLayout.add(buildQueryLayout(), buildResultLayout());
	}

	private VerticalLayout buildQueryLayout() {
		VerticalLayout queryLayout = boxed(new VerticalLayout());

		H6 title = new H6("DICOMweb");
		title.getStyle().set("margin-top", "0px");

		urlFld = new TextField("DICOMweb base URL");
		urlFld.setWidthFull();
		urlFld.setPlaceholder("https://host:443/dicom-web");
		urlFld.setValueChangeMode(ValueChangeMode.EAGER);

		servicesFld = new CheckboxGroup<>("Services to probe");
		servicesFld.setItems(DicomWebServiceType.values());
		servicesFld.setItemLabelGenerator(DicomWebServiceType::getDisplayName);
		servicesFld.setHelperText("none selected means all services");
		servicesFld.setValue(EnumSet.allOf(DicomWebServiceType.class));
		servicesFld.addThemeVariants(CheckboxGroupVariant.AURA_HORIZONTAL);

		groupFilterFld = new ComboBox<>("Group");
		groupFilterFld.setClearButtonVisible(true);
		groupFilterFld.setPlaceholder("All groups");
		groupFilterFld.setWidth("14em");
		groupFilterFld.addValueChangeListener(event -> applyGroupFilter(event.getValue()));
		checkGroupBtn = new Button("Check group", (event) -> runGroupCheck());

		savedEndpointFld = new ComboBox<>("Saved endpoint");
		savedEndpointFld.setItemLabelGenerator(DicomWebView::endpointLabel);
		savedEndpointFld.setClearButtonVisible(true);
		savedEndpointFld.setPlaceholder("Select to fill the form");
		savedEndpointFld.setWidth("22em");
		savedEndpointFld.addValueChangeListener(event -> applyEndpoint(event.getValue()));

		checkBtn = new Button("Check URL", (event) -> runCheck());
		checkBtn.addThemeVariants(ButtonVariant.PRIMARY);

		saveBtn = new Button("Save as endpoint", (event) -> saveAsEndpoint());

		HorizontalLayout bar = new HorizontalLayout(groupFilterFld, checkGroupBtn, savedEndpointFld, urlFld, checkBtn,
				saveBtn);
		bar.setWidthFull();
		bar.setDefaultVerticalComponentAlignment(Alignment.END);
		bar.expand(urlFld);

		queryLayout.add(title, bar, servicesFld);
		refreshSavedEndpoints();
		return queryLayout;
	}

	private static String endpointLabel(WebDestinationConfigEntity endpoint) {
		String label = (endpoint.getDescription() != null && !endpoint.getDescription().isBlank())
				? endpoint.getDescription() : endpoint.getUrl();
		return (endpoint.getGroupName() != null) ? label + " (" + endpoint.getGroupName() + ")" : label;
	}

	private void refreshSavedEndpoints() {
		allEndpoints = webDestinationConfigService.findAll();

		// The dynamic "Gateway destinations" group is always offered (as in the Monitor
		// tab), followed by the organizational groups actually in use by saved endpoints.
		List<String> groups = new ArrayList<>();
		groups.add(DicomNodeUtil.GATEWAY_DESTINATIONS_GROUP_NAME);
		allEndpoints.stream()
			.map(WebDestinationConfigEntity::getGroupName)
			.filter(group -> group != null && !group.isBlank())
			.distinct()
			.sorted()
			.forEach(groups::add);

		groupFilterFld.setItems(groups);
		// Only show the Group filter when there is more than one group to choose from;
		// otherwise an empty selection already checks everything.
		groupFilterFld.setVisible(groups.size() > 1);
		if (!groupFilterFld.isVisible()) {
			groupFilterFld.clear();
		}

		applyGroupFilter(groupFilterFld.getValue());
	}

	private void applyGroupFilter(String group) {
		List<WebDestinationConfigEntity> filtered = (group == null || group.isBlank()) ? allEndpoints
				: allEndpoints.stream().filter(endpoint -> group.equals(endpoint.getGroupName())).toList();
		savedEndpointFld.setItems(filtered);
	}

	private void applyEndpoint(WebDestinationConfigEntity endpoint) {
		if (endpoint == null) {
			return;
		}
		urlFld.setValue(endpoint.getUrl());
		Set<DicomWebServiceType> services = WebDestinationConfigService.decodeServices(endpoint.getServices());
		servicesFld.setValue(services.isEmpty() ? EnumSet.allOf(DicomWebServiceType.class) : services);
	}

	private void saveAsEndpoint() {
		if (urlFld.isEmpty()) {
			displayMessage(new Message(MessageLevel.WARN, MessageFormat.TEXT, "A DICOMweb URL is required"));
			return;
		}

		var prefill = new WebDestinationConfigEntity(null, urlFld.getValue(),
				WebDestinationConfigService.encodeServices(selectedServices()), null);
		prefill.setId(null);
		WebDestinationEditorDialog dialog = new WebDestinationEditorDialog(prefill,
				webDestinationConfigService.getKnownGroups());
		dialog.addSaveEndpointListener(event -> {
			try {
				webDestinationConfigService.save(event.getDescription(), event.getUrl(), event.getServices(),
						event.getGroup());
				refreshSavedEndpoints();
				displayMessage(new Message(MessageLevel.INFO, MessageFormat.TEXT,
						"DICOMweb endpoint saved to the configuration"));
			}
			catch (Exception ex) {
				displayMessage(new Message(MessageLevel.ERROR, MessageFormat.TEXT,
						"Cannot save the DICOMweb endpoint: " + ex.getMessage()));
			}
		});
		dialog.open();
	}

	private Set<DicomWebServiceType> selectedServices() {
		Set<DicomWebServiceType> selected = servicesFld.getValue();
		return (selected == null || selected.isEmpty()) ? EnumSet.allOf(DicomWebServiceType.class) : selected;
	}

	private VerticalLayout buildResultLayout() {
		resultLayout = boxed(new VerticalLayout());
		resultLayout.setVisible(false);

		H6 title = new H6("Result");
		title.getStyle().set("margin-top", "0px");

		resultNote = new Div();
		resultNote.getStyle().set("font-size", "var(--aura-font-size-xs)");
		resultNote.getStyle().set("font-style", "italic");

		resultGrid = new WebDestinationCheckResultGrid();
		resultGrid.setWidthFull();
		resultGrid.setAllRowsVisible(true);

		resultLayout.add(title, resultNote, resultGrid);
		return resultLayout;
	}

	private void runCheck() {
		if (urlFld.isEmpty()) {
			displayMessage(new Message(MessageLevel.WARN, MessageFormat.TEXT, "A DICOMweb URL is required"));
			return;
		}

		String url = urlFld.getValue();
		WebDestinationNode node = new WebDestinationNode(url, url);
		WebDestinationCheckResult result = dicomWebCheckService.check(node, selectedServices());
		displayResults(List.of(new WebNodeCheckResult(node, result)));
	}

	private void runGroupCheck() {
		List<WebDestinationNode> destinations = collectWebDestinations();
		if (destinations.isEmpty()) {
			displayMessage(new Message(MessageLevel.WARN, MessageFormat.TEXT,
					"No DICOMweb (STOW-RS) destination is configured for the selected group"));
			return;
		}

		displayResults(dicomWebCheckService.check(destinations));
	}

	/**
	 * The DICOMweb destinations to check for the selected group, deduplicated by URL: the
	 * saved endpoints (filtered by group) and, unless a specific saved group is selected,
	 * the dynamic Gateway STOW destinations (their own
	 * {@value DicomNodeUtil#GATEWAY_DESTINATIONS_GROUP_NAME} group). An empty selection
	 * checks everything.
	 */
	private List<WebDestinationNode> collectWebDestinations() {
		String group = groupFilterFld.getValue();
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

	private void displayResults(List<WebNodeCheckResult> results) {
		resultGrid.setItems(results);
		resultNote.setText(results.size() + " destination(s) checked - select a row to view the details");
		resultLayout.setVisible(true);
	}

	private static VerticalLayout boxed(VerticalLayout layout) {
		layout.setWidthFull();
		layout.setPadding(true);
		layout.setSpacing(false);
		layout.getStyle()
			.set("box-shadow",
					"0 2px 1px -1px rgba(0,0,0,.2), 0 1px 1px 0 rgba(0,0,0,.14), 0 1px 3px 0 rgba(0,0,0,.12)");
		layout.getStyle().set("border-radius", "4px");
		return layout;
	}

}