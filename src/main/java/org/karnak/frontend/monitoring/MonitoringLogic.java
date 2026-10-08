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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.model.monitoring.DestinationActivityModel;
import org.karnak.backend.model.monitoring.ErrorBreakdownModel;
import org.karnak.backend.model.monitoring.MonitoringSearchCriteria;
import org.karnak.backend.model.monitoring.NodeActivityModel;
import org.karnak.backend.model.monitoring.SeriesActivityModel;
import org.karnak.backend.model.monitoring.StudyActivityModel;
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

	public List<DestinationActivityModel> listDestinations(TransferStatusFilter filter) {
		return monitoringAggregationService.searchDestinations(MonitoringSearchCriteria.from(filter));
	}

	public List<StudyActivityModel> listStudies(TransferStatusFilter filter, UUID destinationUuid) {
		return monitoringAggregationService
			.searchStudies(MonitoringSearchCriteria.from(filter).withDestinationUuid(destinationUuid));
	}

	public List<SeriesActivityModel> listSeries(TransferStatusFilter filter, UUID destinationUuid, String studyUid) {
		return monitoringAggregationService.searchSeries(
				MonitoringSearchCriteria.from(filter).withDestinationUuid(destinationUuid).withStudyUid(studyUid));
	}

	public List<ErrorBreakdownModel> listErrors(TransferStatusFilter filter, UUID destinationUuid, String serieUid) {
		return monitoringAggregationService.searchErrors(
				MonitoringSearchCriteria.from(filter).withDestinationUuid(destinationUuid).withSerieUid(serieUid));
	}

	// --- Forward node dashboard ------------------------------------------------------

	public List<NodeActivityModel> listNodeActivity(TransferStatusFilter filter) {
		return monitoringAggregationService.searchNodeActivity(MonitoringSearchCriteria.from(filter));
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
