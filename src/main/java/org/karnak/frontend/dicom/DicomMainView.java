/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.service.DicomNodeConfigService;
import org.karnak.backend.service.WebDestinationConfigService;
import org.karnak.backend.service.dicom.DicomCapabilitiesCheckService;
import org.karnak.backend.service.dicom.DicomNodeCheckHistoryService;
import org.karnak.backend.service.dicom.DicomNodeCheckService;
import org.karnak.backend.service.dicom.DicomWebCheckService;
import org.karnak.backend.util.DicomNodeUtil;
import org.karnak.frontend.MainLayout;
import org.karnak.frontend.dicom.echo.DicomEchoView;
import org.karnak.frontend.dicom.monitor.MonitorView;
import org.karnak.frontend.dicom.mwl.DicomWorkListView;
import org.karnak.frontend.dicom.web.DicomWebView;
import org.karnak.frontend.dicom.web.ManageDicomWebView;
import org.springframework.beans.factory.annotation.Autowired;
import org.weasis.core.util.annotations.Generated;

@Route(value = DicomMainView.ROUTE, layout = MainLayout.class)
@PageTitle("Karnak - DICOM Web Tools")
@RolesAllowed("admin")
@Generated()
@NullUnmarked
public class DicomMainView extends VerticalLayout {

	public static final String VIEW_NAME = "DICOM Web Tools";

	public static final String ROUTE = "dicom";

	// UI COMPONENTS
	private DicomWebToolsBrand dicomWebToolsBrand;

	private Tabs menu;

	private Map<Tab, Component> tabsToPages;

	private Set<Component> pagesShown;

	// DATA
	private final DicomNodeUtil dicomNodeUtil;

	private final DicomNodeConfigService dicomNodeConfigService;

	private final WebDestinationConfigService webDestinationConfigService;

	private final DicomNodeCheckService dicomNodeCheckService;

	private final DicomCapabilitiesCheckService dicomCapabilitiesCheckService;

	private final DicomWebCheckService dicomWebCheckService;

	private final DicomNodeCheckHistoryService dicomNodeCheckHistoryService;

	// Kept to refresh the persisted echo history when its tab is (re)selected.
	private DicomEchoView dicomEchoView;

	@Autowired
	public DicomMainView(DicomNodeUtil dicomNodeUtil, DicomNodeConfigService dicomNodeConfigService,
			WebDestinationConfigService webDestinationConfigService, DicomNodeCheckService dicomNodeCheckService,
			DicomCapabilitiesCheckService dicomCapabilitiesCheckService, DicomWebCheckService dicomWebCheckService,
			DicomNodeCheckHistoryService dicomNodeCheckHistoryService) {
		this.dicomNodeUtil = dicomNodeUtil;
		this.dicomNodeConfigService = dicomNodeConfigService;
		this.webDestinationConfigService = webDestinationConfigService;
		this.dicomNodeCheckService = dicomNodeCheckService;
		this.dicomCapabilitiesCheckService = dicomCapabilitiesCheckService;
		this.dicomWebCheckService = dicomWebCheckService;
		this.dicomNodeCheckHistoryService = dicomNodeCheckHistoryService;

		createDicomWebToolsBrand();
		createMenu();
	}

	private void createDicomWebToolsBrand() {
		dicomWebToolsBrand = new DicomWebToolsBrand();
	}

	private void createMenu() {
		// Tabs follow the DICOM Web Tools section order.
		var pages = new LinkedHashMap<Tab, Component>();
		dicomEchoView = new DicomEchoView(dicomNodeUtil, dicomNodeCheckService, dicomCapabilitiesCheckService,
				dicomNodeCheckHistoryService);
		pages.put(new Tab("DICOM Echo"), dicomEchoView);
		pages.put(new Tab("DICOM Worklist"), new DicomWorkListView(dicomNodeUtil));
		pages.put(new Tab("Manage DICOM Nodes"), new ManageDicomNodesView(dicomNodeConfigService, dicomNodeUtil));
		pages.put(new Tab("DICOMweb"),
				new DicomWebView(dicomWebCheckService, webDestinationConfigService, dicomNodeUtil));
		pages.put(new Tab("Manage DICOMweb"), new ManageDicomWebView(webDestinationConfigService, dicomNodeUtil));
		pages.put(new Tab("Monitor"), new MonitorView(dicomNodeUtil, webDestinationConfigService, dicomNodeCheckService,
				dicomCapabilitiesCheckService, dicomWebCheckService));

		tabsToPages = pages;

		// Fill the available height so a page can flex-grow its content (e.g. the
		// management
		// grids, which scroll internally instead of overflowing the page).
		setSizeFull();

		menu = new Tabs(pages.keySet().toArray(Tab[]::new));
		add(menu);

		Component firstPage = pages.values().iterator().next();
		pagesShown = Stream.of(firstPage).collect(Collectors.toSet());
		add(firstPage);
		setFlexGrow(1, firstPage);

		menu.addSelectedChangeListener(event -> {
			pagesShown.forEach(page -> page.setVisible(false));
			pagesShown.clear();
			Component selectedPage = tabsToPages.get(menu.getSelectedTab());
			selectedPage.setVisible(true);
			pagesShown.add(selectedPage);
			add(selectedPage);
			setFlexGrow(1, selectedPage);

			if (selectedPage == dicomEchoView) {
				dicomEchoView.onSelected();
			}
		});
	}

}
