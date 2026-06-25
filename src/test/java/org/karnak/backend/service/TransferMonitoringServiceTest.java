/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.dcm4che3.data.Attributes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.karnak.backend.data.repo.TransferSeriesReasonRepo;
import org.karnak.backend.data.repo.TransferSeriesStatusRepo;
import org.karnak.backend.data.repo.specification.TransferSeriesSpecification;
import org.karnak.backend.model.event.TransferMonitoringEvent;
import org.karnak.backend.model.monitoring.MonitoringEntry;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TransferMonitoringServiceTest {

	private final TransferSeriesStatusRepo seriesRepoMock = Mockito.mock(TransferSeriesStatusRepo.class);

	private final TransferSeriesReasonRepo reasonRepoMock = Mockito.mock(TransferSeriesReasonRepo.class);

	private final MonitoringWriteService monitoringWriteServiceMock = Mockito.mock(MonitoringWriteService.class);

	private TransferMonitoringService transferMonitoringService;

	@BeforeEach
	void setUp() {
		transferMonitoringService = new TransferMonitoringService(seriesRepoMock, reasonRepoMock,
				monitoringWriteServiceMock);
	}

	private static TransferMonitoringEvent event() {
		Attributes attributes = new Attributes();
		MonitoringEntry entry = MonitoringEntry.of(2L, 1L, attributes, attributes, true, false, null, "OT",
				"1.2.840.10008.5.1.4.1.1.7");
		return new TransferMonitoringEvent(entry);
	}

	@Test
	void should_upsert_the_series_aggregate_on_event() {
		transferMonitoringService.onTransferMonitoringEvent(event());

		Mockito.verify(monitoringWriteServiceMock, Mockito.times(1)).upsert(any(MonitoringEntry.class));
	}

	@Test
	void should_retry_the_first_insert_race() {
		Mockito.doThrow(new DataIntegrityViolationException("duplicate"))
			.doNothing()
			.when(monitoringWriteServiceMock)
			.upsert(any(MonitoringEntry.class));

		transferMonitoringService.onTransferMonitoringEvent(event());

		Mockito.verify(monitoringWriteServiceMock, Mockito.times(2)).upsert(any(MonitoringEntry.class));
	}

	@Test
	void should_retrieve_series_without_filter() {
		TransferSeriesStatusEntity series = new TransferSeriesStatusEntity();
		series.setStudyUidOriginal("studyUidOriginal");
		when(seriesRepoMock.findAll()).thenReturn(Collections.singletonList(series));

		List<TransferSeriesStatusEntity> result = transferMonitoringService.retrieveSeries(new TransferStatusFilter());

		assertEquals(1, result.size());
		assertEquals("studyUidOriginal", result.get(0).getStudyUidOriginal());
		Mockito.verify(seriesRepoMock, Mockito.times(1)).findAll();
	}

	@Test
	void should_retrieve_series_with_filter() {
		TransferStatusFilter filter = new TransferStatusFilter();
		filter.setStudyUid("studyUid");
		when(seriesRepoMock.findAll(any(TransferSeriesSpecification.class)))
			.thenReturn(Collections.singletonList(new TransferSeriesStatusEntity()));

		List<TransferSeriesStatusEntity> result = transferMonitoringService.retrieveSeries(filter);

		assertEquals(1, result.size());
		Mockito.verify(seriesRepoMock, Mockito.times(1)).findAll(any(TransferSeriesSpecification.class));
	}

	@Test
	void should_delete_all_monitoring_records() {
		transferMonitoringService.deleteAllTransferStatus();

		Mockito.verify(reasonRepoMock, Mockito.times(1)).deleteAllInBatch();
		Mockito.verify(seriesRepoMock, Mockito.times(1)).deleteAllInBatch();
	}

}
