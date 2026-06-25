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
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.data.repo.TransferSeriesReasonRepo;
import org.karnak.backend.data.repo.TransferSeriesStatusRepo;
import org.karnak.backend.model.notification.TransferMonitoringNotification;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

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

}
