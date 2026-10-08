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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.entity.TransferSeriesReasonEntity;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.data.repo.TransferSeriesReasonRepo;
import org.karnak.backend.data.repo.TransferSeriesStatusRepo;
import org.karnak.backend.model.notification.SerieSummaryNotification;
import org.karnak.backend.model.notification.TransferMonitoringNotification;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NotificationServiceTest {

	// Services
	private final TemplateEngine templateEngineMock = Mockito.mock(TemplateEngine.class);

	private final JavaMailSender javaMailSenderMock = Mockito.mock(JavaMailSender.class);

	// Repositories
	private final DestinationRepo destinationRepositoryMock = Mockito.mock(DestinationRepo.class);

	private final TransferSeriesStatusRepo transferSeriesStatusRepoMock = Mockito.mock(TransferSeriesStatusRepo.class);

	private final TransferSeriesReasonRepo transferSeriesReasonRepoMock = Mockito.mock(TransferSeriesReasonRepo.class);

	// Service
	private NotificationService notificationService;

	@BeforeEach
	void setUp() {
		// Mock Destination
		DestinationEntity destinationEntity = new DestinationEntity();
		destinationEntity.setDesidentification(true);
		destinationEntity.setLastTransfer(LocalDateTime.MIN);
		destinationEntity.setId(1L);
		destinationEntity.setActivateNotification(true);
		when(destinationRepositoryMock.findAll()).thenReturn(Arrays.asList(destinationEntity));

		// Mock the aggregated series row for this destination
		TransferSeriesStatusEntity series = new TransferSeriesStatusEntity();
		series.setPatientIdToSend("patientIdToSend");
		series.setPatientIdOriginal("patientIdOriginal");
		series.setInstances(1);
		series.setSent(1);
		series.setErrors(0);
		series.setDestinationEntity(destinationEntity);
		series.setForwardNodeId(2L);
		series.setStudyUidOriginal("studyUidOriginal");
		series.setStudyUidToSend("studyUidToSend");
		series.setSerieUidOriginal("serieUidOriginal");
		series.setStudyDescriptionToSend("studyDescriptionToSend");
		series.setStudyDateToSend(LocalDateTime.MIN);
		series.setSerieUidToSend("serieUidToSend");
		series.setSerieDescriptionToSend("serieDescriptionToSend");
		series.setSerieDateToSend(LocalDateTime.MIN);
		series.setSopClassUids("sopClassUid");
		series.setModality("modality");
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();
		series.setForwardNodeEntity(forwardNodeEntity);
		when(transferSeriesStatusRepoMock.findByDestinationId(Mockito.anyLong())).thenReturn(List.of(series));

		// Build mocked service
		notificationService = new NotificationService(templateEngineMock, javaMailSenderMock,
				transferSeriesStatusRepoMock, transferSeriesReasonRepoMock, destinationRepositoryMock);
	}

	@Test
	void shouldBuildNotificationsToSend() {
		// Call service
		List<TransferMonitoringNotification> transferMonitoringNotifications = notificationService
			.buildNotificationsToSend();

		// Test results
		assertNotNull(transferMonitoringNotifications);
		assertEquals(1, transferMonitoringNotifications.size());
		assertEquals("patientIdToSend", transferMonitoringNotifications.get(0).getPatientId());
		assertEquals("studyUidToSend", transferMonitoringNotifications.get(0).getStudyUid());
		assertEquals("studyDescriptionToSend", transferMonitoringNotifications.get(0).getStudyDescription());
		assertEquals(LocalDateTime.MIN, transferMonitoringNotifications.get(0).getStudyDate());
		assertNotNull(transferMonitoringNotifications.get(0).getSerieSummaryNotifications());
		assertEquals(1, transferMonitoringNotifications.get(0).getSerieSummaryNotifications().size());
		assertEquals("serieUidToSend",
				transferMonitoringNotifications.get(0).getSerieSummaryNotifications().get(0).getSerieUid());
		assertEquals("serieDescriptionToSend",
				transferMonitoringNotifications.get(0).getSerieSummaryNotifications().get(0).getSerieDescription());
		assertEquals(LocalDateTime.MIN,
				transferMonitoringNotifications.get(0).getSerieSummaryNotifications().get(0).getSerieDate());
		assertEquals(1,
				transferMonitoringNotifications.get(0).getSerieSummaryNotifications().get(0).getNbTransferSent());
		assertEquals(0,
				transferMonitoringNotifications.get(0).getSerieSummaryNotifications().get(0).getNbTransferNotSent());
		assertEquals("modality",
				transferMonitoringNotifications.get(0)
					.getSerieSummaryNotifications()
					.get(0)
					.toStringTransferredModalities());
		assertEquals("sopClassUid",
				transferMonitoringNotifications.get(0)
					.getSerieSummaryNotifications()
					.get(0)
					.toStringTransferredSopClassUid());
	}

	@Test
	void not_transferred_count_comes_from_errors_and_excluded_not_from_inflated_instances() {
		// A series whose instances counter is inflated (e.g. blank-UID error events that
		// bypass de-duplication) while every recorded outcome was a successful send:
		// sent=3, errors=0, excluded=0 but instances=6. The email must report 0
		// not-transferred (errors + excluded), not the phantom 3 the old instances -
		// sent formula produced.
		DestinationEntity destinationEntity = new DestinationEntity();
		destinationEntity.setDesidentification(false);
		destinationEntity.setLastTransfer(LocalDateTime.MIN);
		destinationEntity.setId(1L);
		destinationEntity.setActivateNotification(true);
		when(destinationRepositoryMock.findAll()).thenReturn(List.of(destinationEntity));

		TransferSeriesStatusEntity series = new TransferSeriesStatusEntity();
		series.setDestinationEntity(destinationEntity);
		series.setForwardNodeId(2L);
		series.setForwardNodeEntity(new ForwardNodeEntity());
		series.setStudyUidOriginal("studyUidOriginal");
		series.setSerieUidOriginal("serieUidOriginal");
		series.setInstances(6);
		series.setSent(3);
		series.setErrors(0);
		series.setExcluded(0);
		when(transferSeriesStatusRepoMock.findByDestinationId(Mockito.anyLong())).thenReturn(List.of(series));

		List<TransferMonitoringNotification> notifications = notificationService.buildNotificationsToSend();

		assertEquals(1, notifications.size());
		assertEquals(3, notifications.get(0).getSerieSummaryNotifications().get(0).getNbTransferSent());
		assertEquals(0, notifications.get(0).getSerieSummaryNotifications().get(0).getNbTransferNotSent());
	}

	@Test
	void not_transferred_count_sums_errors_and_excluded() {
		// Real not-transferred outcomes: 2 errored + 1 excluded on a non-de-identified
		// destination. Not-transferred is their sum (3) and the row is flagged in error.
		DestinationEntity destinationEntity = new DestinationEntity();
		destinationEntity.setDesidentification(false);
		destinationEntity.setLastTransfer(LocalDateTime.MIN);
		destinationEntity.setId(1L);
		destinationEntity.setActivateNotification(true);
		when(destinationRepositoryMock.findAll()).thenReturn(List.of(destinationEntity));

		TransferSeriesStatusEntity series = new TransferSeriesStatusEntity();
		series.setDestinationEntity(destinationEntity);
		series.setForwardNodeId(2L);
		series.setForwardNodeEntity(new ForwardNodeEntity());
		series.setStudyUidOriginal("studyUidOriginal");
		series.setSerieUidOriginal("serieUidOriginal");
		series.setInstances(5);
		series.setSent(2);
		series.setErrors(2);
		series.setExcluded(1);
		when(transferSeriesStatusRepoMock.findByDestinationId(Mockito.anyLong())).thenReturn(List.of(series));

		List<TransferMonitoringNotification> notifications = notificationService.buildNotificationsToSend();

		assertEquals(1, notifications.size());
		assertEquals(2, notifications.get(0).getSerieSummaryNotifications().get(0).getNbTransferSent());
		assertEquals(3, notifications.get(0).getSerieSummaryNotifications().get(0).getNbTransferNotSent());
		assertTrue(notifications.get(0).getSerieSummaryNotifications().get(0).isContainsError());
	}

	@Test
	void reports_only_the_delta_since_the_last_notification() {
		// 2 instances sent earlier (already notified) then excluded on re-send: snapshot
		// holds notifiedSent=2 / notifiedExcluded=0, current sent=2 / excluded=2. Only
		// the
		// 2 new exclusions are reported, not the 2 historical sends.
		DestinationEntity destinationEntity = new DestinationEntity();
		destinationEntity.setDesidentification(false);
		destinationEntity.setLastTransfer(LocalDateTime.MIN);
		destinationEntity.setId(1L);
		destinationEntity.setActivateNotification(true);
		when(destinationRepositoryMock.findAll()).thenReturn(List.of(destinationEntity));

		TransferSeriesStatusEntity series = new TransferSeriesStatusEntity();
		series.setDestinationEntity(destinationEntity);
		series.setForwardNodeId(2L);
		series.setForwardNodeEntity(new ForwardNodeEntity());
		series.setStudyUidOriginal("studyUidOriginal");
		series.setSerieUidOriginal("serieUidOriginal");
		series.setInstances(2);
		series.setSent(2);
		series.setExcluded(2);
		series.setNotifiedSent(2);
		series.setNotifiedExcluded(0);
		when(transferSeriesStatusRepoMock.findByDestinationId(Mockito.anyLong())).thenReturn(List.of(series));

		List<TransferMonitoringNotification> notifications = notificationService.buildNotificationsToSend();

		assertEquals(1, notifications.size());
		assertEquals(0, notifications.get(0).getSerieSummaryNotifications().get(0).getNbTransferSent());
		assertEquals(2, notifications.get(0).getSerieSummaryNotifications().get(0).getNbTransferNotSent());
		// Snapshot advanced to the current counters for the next notification.
		Mockito.verify(transferSeriesStatusRepoMock).saveAll(Mockito.anyList());
		assertEquals(2, series.getNotifiedExcluded());
	}

	@Test
	void reports_only_reasons_with_a_new_outcome_since_the_last_notification() {
		// Two reasons on the series: "stale" was fully notified in an earlier run
		// (errorCount == notifiedErrorCount, zero delta) while "fresh" gained one new
		// error
		// this session. Only "fresh" must appear; the counters likewise report the single
		// new error, so reasons and counters stay in step.
		DestinationEntity destinationEntity = new DestinationEntity();
		destinationEntity.setDesidentification(false);
		destinationEntity.setLastTransfer(LocalDateTime.MIN);
		destinationEntity.setId(1L);
		destinationEntity.setActivateNotification(true);
		when(destinationRepositoryMock.findAll()).thenReturn(List.of(destinationEntity));

		TransferSeriesStatusEntity series = new TransferSeriesStatusEntity();
		series.setId(7L);
		series.setDestinationEntity(destinationEntity);
		series.setForwardNodeId(2L);
		series.setForwardNodeEntity(new ForwardNodeEntity());
		series.setStudyUidOriginal("studyUidOriginal");
		series.setSerieUidOriginal("serieUidOriginal");
		series.setInstances(2);
		series.setErrors(2);
		series.setNotifiedErrors(1);
		when(transferSeriesStatusRepoMock.findByDestinationId(Mockito.anyLong())).thenReturn(List.of(series));

		TransferSeriesReasonEntity stale = new TransferSeriesReasonEntity(7L, "stale", 1, 0, 0);
		stale.setNotifiedErrorCount(1);
		TransferSeriesReasonEntity fresh = new TransferSeriesReasonEntity(7L, "fresh", 1, 0, 0);
		when(transferSeriesReasonRepoMock.findBySeriesStatusIdIn(Mockito.anyList())).thenReturn(List.of(stale, fresh));

		List<TransferMonitoringNotification> notifications = notificationService.buildNotificationsToSend();

		assertEquals(1, notifications.size());
		SerieSummaryNotification summary = notifications.get(0).getSerieSummaryNotifications().get(0);
		assertEquals(1, summary.getNbTransferNotSent());
		assertEquals(Set.of("fresh"), summary.getUnTransferedReasons());
		// Reason snapshot advanced to the current counts for the next notification.
		assertEquals(1, fresh.getNotifiedErrorCount());
		Mockito.verify(transferSeriesReasonRepoMock).saveAll(Mockito.anyList());
	}

	@Test
	void series_with_no_new_outcome_since_last_notification_is_not_reported() {
		// Snapshot already equals the current counters (nothing new): no notification.
		DestinationEntity destinationEntity = new DestinationEntity();
		destinationEntity.setDesidentification(false);
		destinationEntity.setLastTransfer(LocalDateTime.MIN);
		destinationEntity.setId(1L);
		destinationEntity.setActivateNotification(true);
		when(destinationRepositoryMock.findAll()).thenReturn(List.of(destinationEntity));

		TransferSeriesStatusEntity series = new TransferSeriesStatusEntity();
		series.setDestinationEntity(destinationEntity);
		series.setForwardNodeId(2L);
		series.setForwardNodeEntity(new ForwardNodeEntity());
		series.setStudyUidOriginal("studyUidOriginal");
		series.setSerieUidOriginal("serieUidOriginal");
		series.setInstances(2);
		series.setSent(2);
		series.setNotifiedSent(2);
		when(transferSeriesStatusRepoMock.findByDestinationId(Mockito.anyLong())).thenReturn(List.of(series));

		List<TransferMonitoringNotification> notifications = notificationService.buildNotificationsToSend();

		assertTrue(notifications.isEmpty());
	}

}
