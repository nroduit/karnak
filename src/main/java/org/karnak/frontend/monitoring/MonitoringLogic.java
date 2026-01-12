/*
 * Copyright (c) 2022 Karnak Team and other contributors.
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
import java.io.IOException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.karnak.backend.data.entity.TransferStatusEntity;
import org.karnak.backend.service.TransferMonitoringService;
import org.karnak.frontend.monitoring.component.ExportSettings;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;
import org.karnak.frontend.util.NotificationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Monitoring logic service use to make calls to backend and implement logic linked to the
 * monitoring view
 */
@Service
@Slf4j
public class MonitoringLogic {

	// View
	@Setter
	@Getter
	private MonitoringView monitoringView;

	// Services
	private final TransferMonitoringService transferMonitoringService;

	@Autowired
	public MonitoringLogic(final TransferMonitoringService transferMonitoringService) {
		this.transferMonitoringService = transferMonitoringService;
		this.monitoringView = null;
	}

	/**
	 * Retrieve transfer status
	 * @param filter Filter to apply
	 * @param pageable Pageable
	 * @return Page of trnasfer entities
	 */
	public Page<TransferStatusEntity> retrieveTransferStatus(TransferStatusFilter filter, Pageable pageable) {
		return transferMonitoringService.retrieveTransferStatusPageable(filter, pageable);
	}

	/**
	 * Count number of transfer status
	 * @param filter Filter to apply
	 * @return number of transfer status
	 */
	public int countTransferStatus(TransferStatusFilter filter) {
		return transferMonitoringService.countTransferStatus(filter);
	}

	/**
	 * Build monitoring export in CSV format
	 * @param exportSettings Export settings
	 */
	public byte[] buildCsv(ExportSettings exportSettings) {
		byte[] csvBuilt = new byte[0];
		try {
			csvBuilt = transferMonitoringService
				.buildCsv(monitoringView.getTransferStatusGrid().getTransferStatusFilter(), exportSettings);
		}
		catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException | IOException e) {
			String message = "Error when creating monitoring export CSV file";
			log.error(message, e.getMessage());
			NotificationUtil.displayErrorMessage(message, Position.BOTTOM_CENTER);
		}
		return csvBuilt;
	}

}
