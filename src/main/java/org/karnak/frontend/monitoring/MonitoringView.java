/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.monitoring;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import jakarta.annotation.security.RolesAllowed;
import java.io.ByteArrayInputStream;
import org.karnak.frontend.MainLayout;
import org.karnak.frontend.component.WarningConfirmDialog;
import org.karnak.frontend.monitoring.component.ExportSettingsDialog;
import org.karnak.frontend.monitoring.component.MonitoringDetailPanel;
import org.karnak.frontend.monitoring.component.MonitoringFilterBar;
import org.karnak.frontend.monitoring.component.MonitoringTreeGrid;
import org.karnak.frontend.monitoring.component.NodeActivityDashboard;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.weasis.core.util.annotations.Generated;

/**
 * Monitoring view: a shared date-range filter over two tabs — an Activity tab showing the
 * Destination / Study / Series hierarchy (with one error-reason line per failing series)
 * and a Dashboard tab showing per-forward-node activity.
 */
@Route(value = MonitoringView.ROUTE, layout = MainLayout.class)
@PageTitle("Karnak - Monitoring")
@RolesAllowed("admin")
@Generated()
public class MonitoringView extends VerticalLayout {

	public static final String VIEW_NAME = "Monitoring";

	public static final String ROUTE = "monitoring";

	private final MonitoringLogic monitoringLogic;

	private MonitoringFilterBar filterBar;

	private MonitoringTreeGrid treeGrid;

	private MonitoringDetailPanel detailPanel;

	private NodeActivityDashboard dashboard;

	private ExportSettingsDialog exportSettingsDialog;

	private Component activityPanel;

	private Tabs tabs;

	private Tab activityTab;

	private Tab dashboardTab;

	private final Div content = new Div();

	@Autowired
	public MonitoringView(final MonitoringLogic monitoringLogic) {
		this.monitoringLogic = monitoringLogic;
		this.monitoringLogic.setMonitoringView(this);

		buildComponents();
		addComponentsView();
	}

	/** The filter currently applied (used by the CSV export). */
	public TransferStatusFilter getCurrentFilter() {
		return filterBar.getFilter();
	}

	private void buildComponents() {
		filterBar = new MonitoringFilterBar(this::onFilterChanged);
		treeGrid = new MonitoringTreeGrid(monitoringLogic, filterBar::getFilter);
		detailPanel = new MonitoringDetailPanel();
		treeGrid.setSelectionListener(detailPanel::show);
		dashboard = new NodeActivityDashboard(monitoringLogic, filterBar::getFilter);
		exportSettingsDialog = new ExportSettingsDialog();

		activityPanel = buildActivityPanel();

		activityTab = new Tab(VaadinIcon.LIST_OL.create(), new Text("Activity"));
		dashboardTab = new Tab(VaadinIcon.DASHBOARD.create(), new Text("Dashboard"));
		tabs = new Tabs(activityTab, dashboardTab);
		tabs.addSelectedChangeListener(event -> showSelectedTab());

		content.setSizeFull();
		content.add(activityPanel);
	}

	private Component buildActivityPanel() {
		Button expandErrorsButton = new Button("Expand errors", new Icon(VaadinIcon.WARNING));
		expandErrorsButton.addClickListener(event -> treeGrid.expandErrors());

		Button refreshButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH));
		refreshButton.addClickListener(event -> treeGrid.refresh());

		Button exportSettingsButton = new Button("Export Settings", new Icon(VaadinIcon.COGS));
		exportSettingsButton.addClickListener(event -> exportSettingsDialog.open());

		Anchor exportAnchor = new Anchor();
		exportAnchor.setHref(DownloadHandler.fromInputStream(event -> new DownloadResponse(
				new ByteArrayInputStream(
						monitoringLogic.buildCsv(getCurrentFilter(), exportSettingsDialog.getExportSettings())),
				"export.csv", "text/csv", -1)));
		exportAnchor.getElement().setAttribute("download", true);
		Button exportButton = new Button("Export", new Icon(VaadinIcon.DOWNLOAD_ALT));
		exportButton.setWidthFull();
		exportAnchor.add(exportButton);

		Button deleteButton = new Button("Delete All", new Icon(VaadinIcon.TRASH));
		deleteButton.addThemeVariants(ButtonVariant.ERROR, ButtonVariant.PRIMARY);
		deleteButton.addClickListener(event -> confirmDeleteAll());

		HorizontalLayout buttonLayout = new HorizontalLayout(expandErrorsButton, exportSettingsButton, exportAnchor,
				refreshButton, deleteButton);
		buttonLayout.setWidthFull();

		VerticalLayout treeSide = new VerticalLayout(treeGrid, buttonLayout);
		treeSide.setSizeFull();
		treeSide.setFlexGrow(1, treeGrid);
		treeSide.setPadding(false);

		SplitLayout split = new SplitLayout(treeSide, detailPanel);
		split.setSizeFull();
		split.setSplitterPosition(62);
		return split;
	}

	private void confirmDeleteAll() {
		Div dialogContent = new Div();
		dialogContent.add(new Text(
				"You are about to delete all entries from monitoring. This action cannot be undone. Are you sure?"));
		WarningConfirmDialog dialog = new WarningConfirmDialog("Delete all monitoring entries", dialogContent, "Delete",
				"Cancel");
		dialog.addConfirmationListener(event -> {
			monitoringLogic.deleteAllTransferStatus();
			treeGrid.refresh();
		});
		dialog.open();
	}

	private void onFilterChanged() {
		if (tabs.getSelectedTab() == dashboardTab) {
			dashboard.refresh();
		}
		else {
			treeGrid.refresh();
		}
	}

	private void showSelectedTab() {
		content.removeAll();
		if (tabs.getSelectedTab() == dashboardTab) {
			content.add(dashboard);
			dashboard.refresh();
		}
		else {
			content.add(activityPanel);
			treeGrid.refresh();
		}
	}

	private void addComponentsView() {
		add(filterBar, tabs, content);
		setFlexGrow(1, content);
		setSizeFull();
		setWidthFull();
	}

}
