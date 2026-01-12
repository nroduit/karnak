/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import com.opencsv.CSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.karnak.backend.data.entity.TransferStatusEntity;
import org.karnak.backend.data.repo.TransferStatusRepo;
import org.karnak.backend.data.repo.specification.TransferStatusSpecification;
import org.karnak.backend.model.event.TransferMonitoringEvent;
import org.karnak.frontend.monitoring.component.ExportSettings;
import org.karnak.frontend.monitoring.component.MonitoringCsvMappingStrategy;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Handle transfer monitoring
 */
@Service
@Slf4j
public class TransferMonitoringService {

	@Value("${monitoring.max-history}")
	private int sizeLimit;

	// Repositories
	private final TransferStatusRepo transferStatusRepo;

	@Autowired
	public TransferMonitoringService(final TransferStatusRepo transferStatusRepo) {
		this.transferStatusRepo = transferStatusRepo;
	}

	/**
	 * Listener on TransferMonitoringEvent: event is saved when received
	 * @param transferMonitoringEvent TransferMonitoringEvent to save
	 */
	@Async
	@EventListener
	public void onTransferMonitoringEvent(TransferMonitoringEvent transferMonitoringEvent) {
		TransferStatusEntity transferStatusEntity = (TransferStatusEntity) transferMonitoringEvent.getSource();
		transferStatusRepo.save(transferStatusEntity);
	}

	/**
	 * Occurs every 5 min: clean transfer_status table if over the size limit LIFO: clean
	 * oldest records
	 */
	@Scheduled(fixedRate = 5 * 60 * 1000)
	public void cleanTransferStatus() {
		int nbRecords = (int) transferStatusRepo.count();
		if (nbRecords > sizeLimit) {
			Pageable pageable = PageRequest.of(0, nbRecords - sizeLimit);
			transferStatusRepo.deleteAll(transferStatusRepo.findAllByOrderByTransferDateAsc(pageable));
		}
	}

	/**
	 * Retrieve transfer status depending on filter and pageable
	 * @param filter Filter to evaluate
	 * @param pageable Pageable to evaluate
	 * @return Transfer status entities found
	 */
	public Page<TransferStatusEntity> retrieveTransferStatusPageable(TransferStatusFilter filter, Pageable pageable) {
		Page<TransferStatusEntity> transferStatusFound;
		if (!filter.hasFilter()) {
			// No filter
			transferStatusFound = transferStatusRepo.findAll(pageable);
		}
		else {
			// Create the specification and query the transfer status table
			Specification<TransferStatusEntity> transferStatusSpecification = new TransferStatusSpecification(filter);
			transferStatusFound = transferStatusRepo.findAll(transferStatusSpecification, pageable);
		}
		return transferStatusFound;
	}

	/**
	 * Retrieve transfer status depending on filter
	 * @param filter Filter to evaluate
	 * @return Transfer status entities found
	 */
	public List<TransferStatusEntity> retrieveTransferStatus(TransferStatusFilter filter) {
		List<TransferStatusEntity> transferStatusFound;
		if (!filter.hasFilter()) {
			// No filter
			transferStatusFound = transferStatusRepo.findAll();
		}
		else {
			// Create the specification and query the transfer status table
			Specification<TransferStatusEntity> transferStatusSpecification = new TransferStatusSpecification(filter);
			transferStatusFound = transferStatusRepo.findAll(transferStatusSpecification);
		}
		return transferStatusFound;
	}

	/**
	 * Count transfer status depending on filter
	 * @param filter Filter to evaluate
	 * @return Count of Transfer status entities found
	 */
	public int countTransferStatus(TransferStatusFilter filter) {
		int countTransferStatus;

		if (!filter.hasFilter()) {
			// No filter
			countTransferStatus = (int) transferStatusRepo.count();
		}
		else {
			// Create the specification and query the transfer status table
			Specification<TransferStatusEntity> transferStatusSpecification = new TransferStatusSpecification(filter);
			countTransferStatus = (int) transferStatusRepo.count(transferStatusSpecification);
		}
		return countTransferStatus;
	}

	/**
	 * Build a transfer status csv file depending on filters
	 * @param filter Filters
	 * @param exportSettings Export settings
	 */
	public byte[] buildCsv(TransferStatusFilter filter, ExportSettings exportSettings)
			throws CsvRequiredFieldEmptyException, CsvDataTypeMismatchException, IOException {
		// Init outputStream + writer
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		OutputStreamWriter streamWriter = new OutputStreamWriter(stream);
		CSVWriter writer = new CSVWriter(streamWriter,
				exportSettings.getDelimiter() != null ? exportSettings.getDelimiter().charAt(0)
						: ExportSettings.DEFAULT_CSV_DELIMITER,
				exportSettings.getQuoteCharacter() != null ? exportSettings.getQuoteCharacter().charAt(0)
						: CSVWriter.DEFAULT_QUOTE_CHARACTER,
				CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);

		// Mapping strategy
		MonitoringCsvMappingStrategy<TransferStatusEntity> monitoringCsvMappingStrategy = new MonitoringCsvMappingStrategy<>();

		// Bean to CSV
		StatefulBeanToCsv<TransferStatusEntity> beanToCsv = new StatefulBeanToCsvBuilder<TransferStatusEntity>(writer)
			.withMappingStrategy(monitoringCsvMappingStrategy)
			.build();

		// Write CSV
		beanToCsv.write(retrieveTransferStatus(filter));

		streamWriter.flush();
		return stream.toByteArray();
	}

}
