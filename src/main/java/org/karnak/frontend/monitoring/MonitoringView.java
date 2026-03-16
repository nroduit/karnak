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

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import java.io.ByteArrayInputStream;
import lombok.Getter;
import org.karnak.backend.data.entity.TransferStatusEntity;
import org.karnak.frontend.MainLayout;
import org.karnak.frontend.component.WarningConfirmDialog;
import org.karnak.frontend.monitoring.component.ExportSettingsDialog;
import org.karnak.frontend.monitoring.component.TransferStatusGrid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;

/**
 * Monitoring View
 */
@Route(value = MonitoringView.ROUTE, layout = MainLayout.class)
@PageTitle("Karnak - Monitoring")
@Secured({ "ROLE_admin" })
public class MonitoringView extends VerticalLayout {

	public static final String VIEW_NAME = "Monitoring";

	public static final String ROUTE = "monitoring";

	// Monitoring Logic
	private final MonitoringLogic monitoringLogic;

	// UI components
	@Getter
	private TransferStatusGrid transferStatusGrid;

	private final TransferStatusDataProvider<TransferStatusEntity> transferStatusDataProvider;

	private Button refreshGridButton;

	private Anchor exportAnchor;

	private Button exportSettingsButton;

	private Button deleteButton;

	private ExportSettingsDialog exportSettingsDialog;

	/**
	 * Autowired constructor.
	 * @param monitoringLogic Monitoring Logic used to call backend services and implement
	 * logic linked to the monitoring view
	 */
	@Autowired
	public MonitoringView(final MonitoringLogic monitoringLogic,
			final TransferStatusDataProvider<TransferStatusEntity> transferStatusDataProvider) {
		// Bind the autowired service
		this.monitoringLogic = monitoringLogic;
		this.transferStatusDataProvider = transferStatusDataProvider;

		// Set the view in the service
		this.monitoringLogic.setMonitoringView(this);

		// Build components
		buildComponents();

		// Add components in the view
		addComponentsView();
	}

	/**
	 * Build components
	 */
	private void buildComponents() {
		// Paginated Grid + data provider
		transferStatusGrid = new TransferStatusGrid(transferStatusDataProvider);
		transferStatusDataProvider.setFilter(transferStatusGrid.getTransferStatusFilter());
		transferStatusGrid.setDataProvider(transferStatusDataProvider);

		// Refresh button
		refreshGridButton = new Button("Refresh", new Icon(VaadinIcon.REFRESH));
		refreshGridButton.addClickListener(buttonClickEvent -> transferStatusDataProvider.refreshAll());
		refreshGridButton.setWidth("25%");

		// Export Settings Dialog
		exportSettingsDialog = new ExportSettingsDialog();

		// Export Settings Button
		exportSettingsButton = new Button("Export Settings", new Icon(VaadinIcon.COGS));
		exportSettingsButton.setWidth("25%");
		exportSettingsButton.addClickListener(buttonClickEvent -> exportSettingsDialog.open());

		// Export button
		exportAnchor = new Anchor();
		exportAnchor.setHref(DownloadHandler.fromInputStream(event -> new DownloadResponse(
				new ByteArrayInputStream(monitoringLogic.buildCsv(exportSettingsDialog.getExportSettings())),
				"export.csv", "text/csv", -1)));
		exportAnchor.setWidth("25%");
		Button exportButton = new Button("Export", new Icon(VaadinIcon.DOWNLOAD_ALT));
		exportButton.setWidthFull();
		exportAnchor.getElement().setAttribute("download", true);
		exportAnchor.add(exportButton);

		// Delete button
		deleteButton = new Button("Delete All", new Icon(VaadinIcon.TRASH));
		deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
		deleteButton.setWidth("25%");
		deleteButton.addClickListener(buttonClickEvent -> {
			Div dialogContent = new Div();
			dialogContent.add(new Text(
					"You are about to delete all entries from monitoring. This action cannot be undone. Are you sure?"));
			WarningConfirmDialog dialog = new WarningConfirmDialog("Delete all monitoring entries", dialogContent,
					"Delete", "Cancel");
			dialog.addConfirmationListener(componentEvent -> {
				monitoringLogic.deleteAllTransferStatus();
				transferStatusDataProvider.refreshAll();
			});
			dialog.open();
		});
	}

	/**
	 * Add components in the view
	 */
	private void addComponentsView() {
		add(transferStatusGrid);
		HorizontalLayout buttonLayout = new HorizontalLayout(exportSettingsButton, exportAnchor, refreshGridButton,
				deleteButton);
		buttonLayout.setWidthFull();
		add(buttonLayout);
		setSizeFull();
		setWidthFull();
	}

}
