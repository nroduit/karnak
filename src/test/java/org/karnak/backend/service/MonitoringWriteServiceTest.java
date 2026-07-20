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
import static org.mockito.Mockito.atLeastOnce;
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
import org.karnak.backend.data.repo.TransferSeriesInstanceRepo;
import org.karnak.backend.data.repo.TransferSeriesReasonRepo;
import org.karnak.backend.data.repo.TransferSeriesStatusRepo;
import org.karnak.backend.model.monitoring.MonitoringEntry;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MonitoringWriteServiceTest {

	private final TransferSeriesStatusRepo seriesRepo = Mockito.mock(TransferSeriesStatusRepo.class);

	private final TransferSeriesReasonRepo reasonRepo = Mockito.mock(TransferSeriesReasonRepo.class);

	private final TransferSeriesInstanceRepo instanceRepo = Mockito.mock(TransferSeriesInstanceRepo.class);

	private MonitoringWriteService writeService;

	@BeforeEach
	void setUp() {
		writeService = new MonitoringWriteService(seriesRepo, reasonRepo, instanceRepo);
		when(seriesRepo.saveAndFlush(any(TransferSeriesStatusEntity.class))).thenAnswer(invocation -> {
			TransferSeriesStatusEntity saved = invocation.getArgument(0);
			if (saved.getId() == null) {
				saved.setId(10L);
			}
			return saved;
		});
	}

	private MonitoringEntry entry(boolean sent, boolean error, boolean duplicate, String reason,
			String sopInstanceUid) {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.SeriesInstanceUID, VR.UI, "1.2.3");
		if (sopInstanceUid != null) {
			attributes.setString(Tag.SOPInstanceUID, VR.UI, sopInstanceUid);
		}
		return MonitoringEntry.of(2L, 1L, attributes, attributes, sent, error, duplicate, reason, "CT", "cuid");
	}

	@Test
	void creates_a_new_series_row_with_one_sent_distinct_instance() {
		when(seriesRepo.findWithLockByForwardNodeIdAndDestinationIdAndSerieUidOriginal(anyLong(), anyLong(),
				anyString()))
			.thenReturn(Optional.empty());

		writeService.upsert(entry(true, false, false, null, "sop-1"));

		ArgumentCaptor<TransferSeriesStatusEntity> captor = ArgumentCaptor.forClass(TransferSeriesStatusEntity.class);
		verify(seriesRepo, atLeastOnce()).saveAndFlush(captor.capture());
		TransferSeriesStatusEntity saved = captor.getValue();
		assertEquals(1, saved.getInstances());
		assertEquals(0, saved.getRetries());
		assertEquals(1, saved.getSent());
		assertEquals(0, saved.getErrors());
		assertEquals(0, saved.getExcluded());
		assertEquals("1.2.3", saved.getSerieUidOriginal());
	}

	@Test
	void a_skipped_outcome_counts_as_excluded_not_sent_or_errored() {
		TransferSeriesStatusEntity existing = new TransferSeriesStatusEntity();
		existing.setId(10L);
		when(seriesRepo.findWithLockByForwardNodeIdAndDestinationIdAndSerieUidOriginal(anyLong(), anyLong(),
				anyString()))
			.thenReturn(Optional.of(existing));

		// Filtered / aborted: sent=false, error=false, duplicate=false
		writeService.upsert(entry(false, false, false, null, "sop-1"));

		assertEquals(1, existing.getInstances());
		assertEquals(0, existing.getRetries());
		assertEquals(0, existing.getSent());
		assertEquals(0, existing.getErrors());
		assertEquals(1, existing.getExcluded());
	}

	@Test
	void re_sending_the_same_instance_counts_as_a_retry_not_a_new_instance() {
		TransferSeriesStatusEntity existing = new TransferSeriesStatusEntity();
		existing.setId(10L);
		existing.setInstances(5);
		when(seriesRepo.findWithLockByForwardNodeIdAndDestinationIdAndSerieUidOriginal(anyLong(), anyLong(),
				anyString()))
			.thenReturn(Optional.of(existing));
		when(instanceRepo.existsBySeriesStatusIdAndSopInstanceUid(10L, "sop-1")).thenReturn(true);

		writeService.upsert(entry(true, false, false, null, "sop-1"));

		assertEquals(5, existing.getInstances());
		assertEquals(1, existing.getRetries());
		assertEquals(1, existing.getSent());
		assertEquals(0, existing.getExcluded());
	}

	@Test
	void http_409_counts_as_a_retry_without_a_reason_row() {
		TransferSeriesStatusEntity existing = new TransferSeriesStatusEntity();
		existing.setId(10L);
		existing.setInstances(5);
		existing.setSent(5);
		when(seriesRepo.findWithLockByForwardNodeIdAndDestinationIdAndSerieUidOriginal(anyLong(), anyLong(),
				anyString()))
			.thenReturn(Optional.of(existing));
		when(instanceRepo.existsBySeriesStatusIdAndSopInstanceUid(10L, "sop-1")).thenReturn(true);

		// HTTP 409 "already present": sent=false, error=false, duplicate=true
		writeService.upsert(entry(false, false, true, null, "sop-1"));

		assertEquals(5, existing.getInstances());
		assertEquals(1, existing.getRetries());
		assertEquals(5, existing.getSent());
		assertEquals(0, existing.getErrors());
		assertEquals(0, existing.getExcluded());
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

		writeService.upsert(entry(false, true, false, "timeout", "sop-1"));

		assertEquals(1, existing.getInstances());
		assertEquals(0, existing.getRetries());
		assertEquals(0, existing.getSent());
		assertEquals(1, existing.getErrors());
		assertEquals(0, existing.getExcluded());
		ArgumentCaptor<TransferSeriesReasonEntity> captor = ArgumentCaptor.forClass(TransferSeriesReasonEntity.class);
		verify(reasonRepo).saveAndFlush(captor.capture());
		assertEquals("timeout", captor.getValue().getReason());
		assertEquals(1, captor.getValue().getErrorCount());
		assertEquals(0, captor.getValue().getExcludedCount());
		assertEquals(0, captor.getValue().getRetryCount());
		assertEquals(10L, captor.getValue().getSeriesStatusId());
	}

	@Test
	void aborted_excluded_outcome_with_a_reason_creates_the_reason_counter() {
		TransferSeriesStatusEntity existing = new TransferSeriesStatusEntity();
		existing.setId(10L);
		when(seriesRepo.findWithLockByForwardNodeIdAndDestinationIdAndSerieUidOriginal(anyLong(), anyLong(),
				anyString()))
			.thenReturn(Optional.of(existing));
		when(reasonRepo.findBySeriesStatusIdAndReason(10L, "de-identification abort")).thenReturn(Optional.empty());

		// Editor-requested abort: sent=false, error=false, duplicate=false -> counted as
		// excluded, but the reason must still be recorded.
		writeService.upsert(entry(false, false, false, "de-identification abort", "sop-1"));

		assertEquals(0, existing.getErrors());
		assertEquals(1, existing.getExcluded());
		ArgumentCaptor<TransferSeriesReasonEntity> captor = ArgumentCaptor.forClass(TransferSeriesReasonEntity.class);
		verify(reasonRepo).saveAndFlush(captor.capture());
		assertEquals("de-identification abort", captor.getValue().getReason());
		assertEquals(0, captor.getValue().getErrorCount());
		assertEquals(1, captor.getValue().getExcludedCount());
		assertEquals(0, captor.getValue().getRetryCount());
		assertEquals(10L, captor.getValue().getSeriesStatusId());
	}

	@Test
	void an_excluded_re_send_of_a_known_instance_increments_the_reason_retry_counter() {
		TransferSeriesStatusEntity existing = new TransferSeriesStatusEntity();
		existing.setId(10L);
		when(seriesRepo.findWithLockByForwardNodeIdAndDestinationIdAndSerieUidOriginal(anyLong(), anyLong(),
				anyString()))
			.thenReturn(Optional.of(existing));
		// Already-seen instance -> the excluded outcome is a retry on the novelty axis.
		when(instanceRepo.existsBySeriesStatusIdAndSopInstanceUid(10L, "sop-1")).thenReturn(true);
		when(reasonRepo.findBySeriesStatusIdAndReason(10L, "SOP Class X is not in the SOPClassUID filter"))
			.thenReturn(Optional.empty());

		writeService.upsert(entry(false, false, false, "SOP Class X is not in the SOPClassUID filter", "sop-1"));

		assertEquals(1, existing.getRetries());
		assertEquals(1, existing.getExcluded());
		assertEquals(0, existing.getInstances());
		ArgumentCaptor<TransferSeriesReasonEntity> captor = ArgumentCaptor.forClass(TransferSeriesReasonEntity.class);
		verify(reasonRepo).saveAndFlush(captor.capture());
		assertEquals(0, captor.getValue().getErrorCount());
		assertEquals(1, captor.getValue().getExcludedCount());
		assertEquals(1, captor.getValue().getRetryCount());
	}

	@Test
	void an_error_without_an_original_sop_instance_uid_does_not_inflate_instances() {
		TransferSeriesStatusEntity existing = new TransferSeriesStatusEntity();
		existing.setId(10L);
		existing.setInstances(3);
		existing.setErrors(3);
		when(seriesRepo.findWithLockByForwardNodeIdAndDestinationIdAndSerieUidOriginal(anyLong(), anyLong(),
				anyString()))
			.thenReturn(Optional.of(existing));
		when(reasonRepo.findBySeriesStatusIdAndReason(10L, "charset")).thenReturn(Optional.empty());

		// Failure raised before the original attributes were read: no SOP Instance UID,
		// so the outcome cannot be attributed to a distinct instance.
		writeService.upsert(entry(false, true, false, "charset", null));

		// Delivery outcome still recorded, but neither novelty bucket moves and no
		// instance identity row is written.
		assertEquals(3, existing.getInstances());
		assertEquals(0, existing.getRetries());
		assertEquals(4, existing.getErrors());
		verify(instanceRepo, never()).saveAndFlush(any());
	}

	@Test
	void repeated_unidentified_failures_do_not_accumulate_phantom_instances() {
		TransferSeriesStatusEntity existing = new TransferSeriesStatusEntity();
		existing.setId(10L);
		when(seriesRepo.findWithLockByForwardNodeIdAndDestinationIdAndSerieUidOriginal(anyLong(), anyLong(),
				anyString()))
			.thenReturn(Optional.of(existing));
		when(reasonRepo.findBySeriesStatusIdAndReason(10L, "charset")).thenReturn(Optional.empty());

		writeService.upsert(entry(false, true, false, "charset", null));
		writeService.upsert(entry(false, true, false, "charset", null));
		writeService.upsert(entry(false, true, false, "charset", null));

		// Three identity-less failures used to be counted as three distinct instances.
		assertEquals(0, existing.getInstances());
		assertEquals(0, existing.getRetries());
		assertEquals(3, existing.getErrors());
	}

}