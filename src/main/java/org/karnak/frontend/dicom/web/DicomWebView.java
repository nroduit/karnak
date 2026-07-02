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

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.WebDestinationConfigEntity;
import org.karnak.backend.enums.DicomWebServiceType;
import org.karnak.backend.enums.MessageFormat;
import org.karnak.backend.enums.MessageLevel;
import org.karnak.backend.model.dicom.Message;
import org.karnak.backend.model.dicom.WebDestinationNode;
import org.karnak.backend.model.dicom.result.TlsCertificateInfo;
import org.karnak.backend.model.dicom.result.WebDestinationCheckResult;
import org.karnak.backend.service.WebDestinationConfigService;
import org.karnak.backend.service.dicom.DicomWebCheckService;
import org.karnak.frontend.dicom.AbstractView;
import org.weasis.core.util.annotations.Generated;

@Generated()
@NullUnmarked
public class DicomWebView extends AbstractView {

	private final transient DicomWebCheckService dicomWebCheckService;

	private final transient WebDestinationConfigService webDestinationConfigService;

	private ComboBox<String> groupFilterFld;

	private ComboBox<WebDestinationConfigEntity> savedEndpointFld;

	private transient List<WebDestinationConfigEntity> allEndpoints = List.of();

	private TextField urlFld;

	private CheckboxGroup<DicomWebServiceType> servicesFld;

	private Button checkBtn;

	private Button saveBtn;

	private VerticalLayout resultLayout;

	private Badge resultBadge;

	private UnorderedList resultDetails;

	public DicomWebView(DicomWebCheckService dicomWebCheckService,
			WebDestinationConfigService webDestinationConfigService) {
		this.dicomWebCheckService = dicomWebCheckService;
		this.webDestinationConfigService = webDestinationConfigService;
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
		// Aura stacks checkbox-group options vertically by default; lay them out in a row
		// as they were under Lumo.
		servicesFld.addThemeVariants(CheckboxGroupVariant.AURA_HORIZONTAL);

		groupFilterFld = new ComboBox<>("Group");
		groupFilterFld.setClearButtonVisible(true);
		groupFilterFld.setPlaceholder("All groups");
		groupFilterFld.setWidth("12em");
		groupFilterFld.addValueChangeListener(event -> applyGroupFilter(event.getValue()));

		savedEndpointFld = new ComboBox<>("Saved endpoint");
		savedEndpointFld.setItemLabelGenerator(DicomWebView::endpointLabel);
		savedEndpointFld.setClearButtonVisible(true);
		savedEndpointFld.setPlaceholder("Select to fill the form");
		savedEndpointFld.setWidth("22em");
		savedEndpointFld.addValueChangeListener(event -> applyEndpoint(event.getValue()));

		checkBtn = new Button("Check", (event) -> runCheck());
		checkBtn.addThemeVariants(ButtonVariant.PRIMARY);

		saveBtn = new Button("Save as endpoint", (event) -> saveAsEndpoint());

		HorizontalLayout bar = new HorizontalLayout(groupFilterFld, savedEndpointFld, urlFld, checkBtn, saveBtn);
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

		List<String> groups = allEndpoints.stream()
			.map(WebDestinationConfigEntity::getGroupName)
			.filter(group -> group != null && !group.isBlank())
			.distinct()
			.sorted()
			.toList();

		groupFilterFld.setItems(groups);
		// Only show the Group filter when there is more than one group to choose from;
		// otherwise the group already shown in the endpoint label is enough.
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

		resultBadge = new Badge();
		resultDetails = new UnorderedList();

		resultLayout.add(title, resultBadge, resultDetails);
		return resultLayout;
	}

	private void runCheck() {
		if (urlFld.isEmpty()) {
			displayMessage(new Message(MessageLevel.WARN, MessageFormat.TEXT, "A DICOMweb URL is required"));
			return;
		}

		String url = urlFld.getValue();
		WebDestinationCheckResult result = dicomWebCheckService.check(new WebDestinationNode(url, url),
				selectedServices());
		display(result);
	}

	private void display(WebDestinationCheckResult result) {
		resultDetails.removeAll();

		if (result.isUnexpectedError()) {
			initBadge(false);
			resultDetails.add(new ListItem("Error: " + result.getUnexpectedErrorMessage()));
			resultLayout.setVisible(true);
			return;
		}

		initBadge(result.isSuccessful());

		String endpoint = result.getHost() + ":" + result.getPort();
		resultDetails.add(new ListItem(result.isTcpReachable() ? "TCP connection to " + endpoint + " succeeded"
				: "TCP connection to " + endpoint + " failed"));

		if (result.isHttpResponded()) {
			resultDetails.add(new ListItem("HTTP OPTIONS returned status " + result.getHttpStatus()));
		}
		else if (result.isTcpReachable()) {
			resultDetails.add(new ListItem("No HTTP response from the endpoint"));
		}

		if (result.isSecure()) {
			TlsCertificateInfo tls = result.getTls();
			resultDetails.add(new ListItem(tls != null ? "TLS: " + tls.getSummary() : "TLS handshake failed"));
		}

		result.getServiceProbes().forEach((probe) -> resultDetails.add(new ListItem(probe.getSummary())));

		resultLayout.setVisible(true);
	}

	private void initBadge(boolean successful) {
		resultBadge.removeThemeVariants(BadgeVariant.values());
		resultBadge.addThemeVariants(successful ? BadgeVariant.SUCCESS : BadgeVariant.ERROR);
		resultBadge.setText(successful ? "Reachable" : "Unreachable");
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
