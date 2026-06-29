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

import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.IOException;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.model.monitoring.DestinationActivity;
import org.karnak.backend.model.monitoring.ErrorBreakdown;
import org.karnak.backend.model.monitoring.NodeActivity;
import org.karnak.backend.model.monitoring.SeriesActivity;
import org.karnak.backend.model.monitoring.StudyActivity;
import org.karnak.backend.service.MonitoringAggregationService;
import org.karnak.backend.service.TransferMonitoringService;
import org.karnak.frontend.monitoring.component.ExportSettings;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;
import org.karnak.frontend.util.NotificationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.weasis.core.util.annotations.Generated;

/**
 * Monitoring logic service use to make calls to backend and implement logic linked to the
 * monitoring view
 */
@SpringComponent
@UIScope
@Slf4j
@Generated()
@NullUnmarked
public class MonitoringLogic {

	// View
	@Setter
	@Getter
	private MonitoringView monitoringView;

	// Services
	private final transient TransferMonitoringService transferMonitoringService;

	private final transient MonitoringAggregationService monitoringAggregationService;

	@Autowired
	public MonitoringLogic(final TransferMonitoringService transferMonitoringService,
			final MonitoringAggregationService monitoringAggregationService) {
		this.transferMonitoringService = transferMonitoringService;
		this.monitoringAggregationService = monitoringAggregationService;
		this.monitoringView = null;
	}

	// --- Hierarchy aggregation (Destination / Study / Series / errors) ---------------

	public List<DestinationActivity> listDestinations(TransferStatusFilter filter) {
		return monitoringAggregationService.listDestinations(filter);
	}

	public List<StudyActivity> listStudies(TransferStatusFilter filter, Long destinationId) {
		return monitoringAggregationService.listStudies(filter, destinationId);
	}

	public List<SeriesActivity> listSeries(TransferStatusFilter filter, Long destinationId, String studyUid) {
		return monitoringAggregationService.listSeries(filter, destinationId, studyUid);
	}

	public List<ErrorBreakdown> listErrors(TransferStatusFilter filter, Long destinationId, String serieUid) {
		return monitoringAggregationService.listErrors(filter, destinationId, serieUid);
	}

	// --- Forward node dashboard ------------------------------------------------------

	public List<NodeActivity> listNodeActivity(TransferStatusFilter filter) {
		return monitoringAggregationService.listNodeActivity(filter);
	}

	// --- Maintenance & export --------------------------------------------------------

	/**
	 * Delete all transfer status records
	 */
	public void deleteAllTransferStatus() {
		transferMonitoringService.deleteAllTransferStatus();
	}

	/**
	 * Build monitoring export in CSV format for the matching rows
	 * @param filter the current filter
	 * @param exportSettings Export settings
	 */
	public byte[] buildCsv(TransferStatusFilter filter, ExportSettings exportSettings) {
		byte[] csvBuilt = new byte[0];
		try {
			csvBuilt = transferMonitoringService.buildCsv(filter, exportSettings);
		}
		catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException | IOException e) {
			String message = "Error when creating monitoring export CSV file";
			log.error(message, e.getMessage());
			NotificationUtil.displayErrorMessage(message, Position.BOTTOM_CENTER);
		}
		return csvBuilt;
	}

}
