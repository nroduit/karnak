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
import org.karnak.backend.data.entity.TransferStatusEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.data.repo.TransferStatusRepo;
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

	private final TransferStatusRepo transferStatusRepoMock = Mockito.mock(TransferStatusRepo.class);

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

		// Mock transfer status
		TransferStatusEntity transferStatusEntity = new TransferStatusEntity();
		transferStatusEntity.setPatientIdToSend("patientIdToSend");
		transferStatusEntity.setPatientIdOriginal("patientIdOriginal");
		transferStatusEntity.setSent(true);
		transferStatusEntity.setDestinationEntity(destinationEntity);
		transferStatusEntity.setForwardNodeId(2L);
		transferStatusEntity.setStudyUidOriginal("studyUidOriginal");
		transferStatusEntity.setStudyUidToSend("studyUidToSend");
		transferStatusEntity.setSerieUidOriginal("serieUidOriginal");
		transferStatusEntity.setStudyDescriptionToSend("studyDescriptionToSend");
		transferStatusEntity.setStudyDateToSend(LocalDateTime.MIN);
		transferStatusEntity.setSerieUidToSend("serieUidToSend");
		transferStatusEntity.setSerieDescriptionToSend("serieDescriptionToSend");
		transferStatusEntity.setSerieDateToSend(LocalDateTime.MIN);
		transferStatusEntity.setSopClassUid("sopClassUid");
		transferStatusEntity.setModality("modality");
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();
		transferStatusEntity.setForwardNodeEntity(forwardNodeEntity);
		when(transferStatusRepoMock.findByDestinationId(Mockito.anyLong())).thenReturn(List.of(transferStatusEntity));

		// Build mocked service
		notificationService = new NotificationService(templateEngineMock, javaMailSenderMock, transferStatusRepoMock,
				destinationRepositoryMock);
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
