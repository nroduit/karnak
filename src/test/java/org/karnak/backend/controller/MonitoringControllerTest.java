/*
 * Copyright (c) 2024-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.karnak.backend.enums.TransferStatusType;
import org.karnak.backend.service.TransferMonitoringService;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MonitoringControllerTest {

	private TransferMonitoringService transferMonitoringService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		transferMonitoringService = Mockito.mock(TransferMonitoringService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new MonitoringController(transferMonitoringService)).build();
	}

	@Test
	void getTransfers_returns_empty_page_with_metadata() throws Exception {
		Page<TransferSeriesStatusEntity> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);
		when(transferMonitoringService.retrieveSeriesPageable(any(), any())).thenReturn(page);

		mockMvc.perform(get("/api/monitoring/transfers"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content.length()").value(0))
			.andExpect(jsonPath("$.totalElements").value(0))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(50));
	}

	@Test
	void getTransfers_clamps_excessive_page_size() throws Exception {
		Page<TransferSeriesStatusEntity> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 1000), 0);
		when(transferMonitoringService.retrieveSeriesPageable(any(), any())).thenReturn(page);

		mockMvc.perform(get("/api/monitoring/transfers").param("size", "999999"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.size").value(1000));

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(transferMonitoringService).retrieveSeriesPageable(any(), captor.capture());
		// Clamped to MAX_PAGE_SIZE (1000)
		org.junit.jupiter.api.Assertions.assertEquals(1000, captor.getValue().getPageSize());
	}

	@Test
	void getTransfers_clamps_negative_page() throws Exception {
		Page<TransferSeriesStatusEntity> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);
		when(transferMonitoringService.retrieveSeriesPageable(any(), any())).thenReturn(page);

		mockMvc.perform(get("/api/monitoring/transfers").param("page", "-5"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.page").value(0));
	}

	@Test
	void getTransfers_invalid_status_defaults_to_ALL() throws Exception {
		Page<TransferSeriesStatusEntity> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);
		when(transferMonitoringService.retrieveSeriesPageable(any(), any())).thenReturn(page);

		mockMvc.perform(get("/api/monitoring/transfers").param("status", "NOT_A_REAL_STATUS"))
			.andExpect(status().isOk());

		ArgumentCaptor<TransferStatusFilter> captor = ArgumentCaptor.forClass(TransferStatusFilter.class);
		verify(transferMonitoringService).retrieveSeriesPageable(captor.capture(), any());
		org.junit.jupiter.api.Assertions.assertEquals(TransferStatusType.ALL,
				captor.getValue().getTransferStatusType());
	}

	@Test
	void getTransfers_forwards_uid_filters() throws Exception {
		Page<TransferSeriesStatusEntity> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);
		when(transferMonitoringService.retrieveSeriesPageable(any(), any())).thenReturn(page);

		mockMvc
			.perform(get("/api/monitoring/transfers").param("studyUid", "1.2.840")
				.param("serieUid", "1.2.3")
				.param("status", "ERROR"))
			.andExpect(status().isOk());

		ArgumentCaptor<TransferStatusFilter> captor = ArgumentCaptor.forClass(TransferStatusFilter.class);
		verify(transferMonitoringService).retrieveSeriesPageable(captor.capture(), any());
		TransferStatusFilter filter = captor.getValue();
		org.junit.jupiter.api.Assertions.assertEquals("1.2.840", filter.getStudyUid());
		org.junit.jupiter.api.Assertions.assertEquals("1.2.3", filter.getSerieUid());
		org.junit.jupiter.api.Assertions.assertEquals(TransferStatusType.ERROR, filter.getTransferStatusType());
	}

	@Test
	void countTransfers_returns_count_json() throws Exception {
		when(transferMonitoringService.countSeries(any())).thenReturn(123L);
		mockMvc.perform(get("/api/monitoring/transfers/count"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.count").value(123));
	}

	@Test
	void exportTransfers_returns_csv_attachment() throws Exception {
		byte[] csvBytes = "h1,h2\nv1,v2\n".getBytes();
		when(transferMonitoringService.buildCsv(any(), any())).thenReturn(csvBytes);

		mockMvc.perform(get("/api/monitoring/transfers/export"))
			.andExpect(status().isOk())
			.andExpect(result -> org.junit.jupiter.api.Assertions
				.assertTrue(result.getResponse().getHeader("Content-Disposition").contains("monitoring-export.csv")));
	}

	@Test
	void exportTransfers_returns_500_on_service_failure() throws Exception {
		when(transferMonitoringService.buildCsv(any(), any())).thenThrow(new RuntimeException("boom"));
		mockMvc.perform(get("/api/monitoring/transfers/export")).andExpect(status().isInternalServerError());
		// Sanity: service did get called
		verify(transferMonitoringService, Mockito.atLeastOnce()).buildCsv(any(), any());
		// And we never reached the paginated JSON branch
		verify(transferMonitoringService, never()).retrieveSeriesPageable(any(), any());
	}

}
