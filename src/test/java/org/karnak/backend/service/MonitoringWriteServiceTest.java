/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.TransferSeriesReasonEntity;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.karnak.backend.data.repo.TransferSeriesReasonRepo;
import org.karnak.backend.data.repo.TransferSeriesStatusRepo;
import org.karnak.backend.model.monitoring.MonitoringEntry;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MonitoringWriteServiceTest {

	private final TransferSeriesStatusRepo seriesRepo = Mockito.mock(TransferSeriesStatusRepo.class);

	private final TransferSeriesReasonRepo reasonRepo = Mockito.mock(TransferSeriesReasonRepo.class);

	private MonitoringWriteService writeService;

	@BeforeEach
	void setUp() {
		writeService = new MonitoringWriteService(seriesRepo, reasonRepo);
		when(seriesRepo.saveAndFlush(any(TransferSeriesStatusEntity.class))).thenAnswer(invocation -> {
			TransferSeriesStatusEntity saved = invocation.getArgument(0);
			if (saved.getId() == null) {
				saved.setId(10L);
			}
			return saved;
		});
	}

	private MonitoringEntry entry(boolean sent, boolean error, String reason) {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.SeriesInstanceUID, VR.UI, "1.2.3");
		return MonitoringEntry.of(2L, 1L, attributes, attributes, sent, error, reason, "CT", "cuid");
	}

	@Test
	void creates_a_new_series_row_with_one_sent_instance() {
		when(seriesRepo.findWithLockByForwardNodeIdAndDestinationIdAndSerieUidOriginal(anyLong(), anyLong(),
				anyString()))
			.thenReturn(Optional.empty());

		writeService.upsert(entry(true, false, null));

		ArgumentCaptor<TransferSeriesStatusEntity> captor = ArgumentCaptor.forClass(TransferSeriesStatusEntity.class);
		verify(seriesRepo).saveAndFlush(captor.capture());
		TransferSeriesStatusEntity saved = captor.getValue();
		assertEquals(1, saved.getInstances());
		assertEquals(1, saved.getSent());
		assertEquals(0, saved.getErrors());
		assertEquals("1.2.3", saved.getSerieUidOriginal());
	}

	@Test
	void http_409_counts_as_both_sent_and_error_without_a_reason_row() {
		TransferSeriesStatusEntity existing = new TransferSeriesStatusEntity();
		existing.setId(10L);
		existing.setInstances(5);
		existing.setSent(5);
		existing.setErrors(0);
		when(seriesRepo.findWithLockByForwardNodeIdAndDestinationIdAndSerieUidOriginal(anyLong(), anyLong(),
				anyString()))
			.thenReturn(Optional.of(existing));

		// HTTP 409: sent=true, error=true, reason=null
		writeService.upsert(entry(true, true, null));

		assertEquals(6, existing.getInstances());
		assertEquals(6, existing.getSent());
		assertEquals(1, existing.getErrors());
		verify(reasonRepo, never()).saveAndFlush(any(TransferSeriesReasonEntity.class));
	}

	@Test
	void error_with_a_reason_creates_the_reason_counter() {
		TransferSeriesStatusEntity existing = new TransferSeriesStatusEntity();
		existing.setId(10L);
		when(seriesRepo.findWithLockByForwardNodeIdAndDestinationIdAndSerieUidOriginal(anyLong(), anyLong(),
				anyString()))
			.thenReturn(Optional.of(existing));
		when(reasonRepo.findBySeriesStatusIdAndReason(10L, "timeout")).thenReturn(Optional.empty());

		writeService.upsert(entry(false, true, "timeout"));

		assertEquals(1, existing.getInstances());
		assertEquals(0, existing.getSent());
		assertEquals(1, existing.getErrors());
		ArgumentCaptor<TransferSeriesReasonEntity> captor = ArgumentCaptor.forClass(TransferSeriesReasonEntity.class);
		verify(reasonRepo).saveAndFlush(captor.capture());
		assertEquals("timeout", captor.getValue().getReason());
		assertEquals(1, captor.getValue().getCount());
		assertEquals(10L, captor.getValue().getSeriesStatusId());
	}

}
