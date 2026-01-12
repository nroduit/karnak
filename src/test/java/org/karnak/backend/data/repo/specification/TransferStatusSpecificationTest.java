/*
 * Copyright (c) 2022 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.repo.specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.TransferStatusEntity;
import org.karnak.backend.data.repo.TransferStatusRepo;
import org.karnak.backend.enums.TransferStatusType;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;

@DataJpaTest
class TransferStatusSpecificationTest {

	@Autowired
	private TransferStatusRepo repository;

	@BeforeEach
	void setUp() {

		// Study Uid original
		TransferStatusEntity transferStatusWithStudyUidOriginal = new TransferStatusEntity();
		transferStatusWithStudyUidOriginal.setStudyUidOriginal("studyUidOriginal");
		// Study Uid to Send
		TransferStatusEntity transferStatusWithStudyUidToSend = new TransferStatusEntity();
		transferStatusWithStudyUidToSend.setStudyUidToSend("studyUidToSend");

		// Serie Uid original
		TransferStatusEntity transferStatusWithSerieUidOriginal = new TransferStatusEntity();
		transferStatusWithSerieUidOriginal.setSerieUidOriginal("serieUidOriginal");
		// Serie Uid to Send
		TransferStatusEntity transferStatusWithSerieUidToSend = new TransferStatusEntity();
		transferStatusWithSerieUidToSend.setSerieUidToSend("serieUidToSend");

		// Sop Instance Uid original
		TransferStatusEntity transferStatusWithSopInstanceUidOriginal = new TransferStatusEntity();
		transferStatusWithSopInstanceUidOriginal.setSopInstanceUidOriginal("sopInstanceUidOriginal");
		// Sop Instance Uid to Send
		TransferStatusEntity transferStatusWithSopInstanceUidToSend = new TransferStatusEntity();
		transferStatusWithSopInstanceUidToSend.setSopInstanceUidToSend("sopInstanceUidToSend");

		// Sent true
		TransferStatusEntity transferStatusWithHasBeenSent = new TransferStatusEntity();
		transferStatusWithHasBeenSent.setSent(true);

		// With transfer date
		TransferStatusEntity transferStatusWithTransferDate = new TransferStatusEntity();
		transferStatusWithTransferDate.setTransferDate(LocalDateTime.of(2022, 1, 1, 6, 0, 0));

		// Save in DB
		Arrays
			.asList(transferStatusWithStudyUidOriginal, transferStatusWithStudyUidToSend,
					transferStatusWithSerieUidOriginal, transferStatusWithSerieUidToSend,
					transferStatusWithSopInstanceUidOriginal, transferStatusWithSopInstanceUidToSend,
					transferStatusWithHasBeenSent, transferStatusWithTransferDate)
			.forEach(transferStatusEntity -> repository.saveAndFlush(transferStatusEntity));
	}

	@Test
	void shouldFilterByStudyUid() {
		// Init data
		// Original
		TransferStatusFilter filterOriginal = new TransferStatusFilter();
		filterOriginal.setStudyUid("studyUidOriginal");
		Specification<TransferStatusEntity> transferStatusSpecificationOriginal = new TransferStatusSpecification(
				filterOriginal);
		// To Send
		TransferStatusFilter filterToSend = new TransferStatusFilter();
		filterToSend.setStudyUid("studyUidToSend");
		Specification<TransferStatusEntity> transferStatusSpecificationToSend = new TransferStatusSpecification(
				filterToSend);

		// Call service
		List<TransferStatusEntity> transferStatusEntitiesOriginal = repository
			.findAll(transferStatusSpecificationOriginal);
		List<TransferStatusEntity> transferStatusEntitiesToSend = repository.findAll(transferStatusSpecificationToSend);

		// Test results
		// Original
		assertNotNull(transferStatusEntitiesOriginal);
		assertFalse(transferStatusEntitiesOriginal.isEmpty());
		assertEquals(1, transferStatusEntitiesOriginal.size());
		assertEquals("studyUidOriginal", transferStatusEntitiesOriginal.get(0).getStudyUidOriginal());
		// To Send
		assertNotNull(transferStatusEntitiesToSend);
		assertFalse(transferStatusEntitiesToSend.isEmpty());
		assertEquals(1, transferStatusEntitiesToSend.size());
		assertEquals("studyUidToSend", transferStatusEntitiesToSend.get(0).getStudyUidToSend());
	}

	@Test
	void shouldFilterBySerieUid() {
		// Init data
		// Original
		TransferStatusFilter filterOriginal = new TransferStatusFilter();
		filterOriginal.setSerieUid("serieUidOriginal");
		Specification<TransferStatusEntity> transferStatusSpecificationOriginal = new TransferStatusSpecification(
				filterOriginal);
		// To Send
		TransferStatusFilter filterToSend = new TransferStatusFilter();
		filterToSend.setSerieUid("serieUidToSend");
		Specification<TransferStatusEntity> transferStatusSpecificationToSend = new TransferStatusSpecification(
				filterToSend);

		// Call service
		List<TransferStatusEntity> transferStatusEntitiesOriginal = repository
			.findAll(transferStatusSpecificationOriginal);
		List<TransferStatusEntity> transferStatusEntitiesToSend = repository.findAll(transferStatusSpecificationToSend);

		// Test results
		// Original
		assertNotNull(transferStatusEntitiesOriginal);
		assertFalse(transferStatusEntitiesOriginal.isEmpty());
		assertEquals(1, transferStatusEntitiesOriginal.size());
		assertEquals("serieUidOriginal", transferStatusEntitiesOriginal.get(0).getSerieUidOriginal());
		// To Send
		assertNotNull(transferStatusEntitiesToSend);
		assertFalse(transferStatusEntitiesToSend.isEmpty());
		assertEquals(1, transferStatusEntitiesToSend.size());
		assertEquals("serieUidToSend", transferStatusEntitiesToSend.get(0).getSerieUidToSend());
	}

	@Test
	void shouldFilterBySopInstanceUid() {
		// Init data
		// Original
		TransferStatusFilter filterOriginal = new TransferStatusFilter();
		filterOriginal.setSopInstanceUid("sopInstanceUidOriginal");
		Specification<TransferStatusEntity> transferStatusSpecificationOriginal = new TransferStatusSpecification(
				filterOriginal);
		// To Send
		TransferStatusFilter filterToSend = new TransferStatusFilter();
		filterToSend.setSopInstanceUid("sopInstanceUidToSend");
		Specification<TransferStatusEntity> transferStatusSpecificationToSend = new TransferStatusSpecification(
				filterToSend);

		// Call service
		List<TransferStatusEntity> transferStatusEntitiesOriginal = repository
			.findAll(transferStatusSpecificationOriginal);
		List<TransferStatusEntity> transferStatusEntitiesToSend = repository.findAll(transferStatusSpecificationToSend);

		// Test results
		// Original
		assertNotNull(transferStatusEntitiesOriginal);
		assertFalse(transferStatusEntitiesOriginal.isEmpty());
		assertEquals(1, transferStatusEntitiesOriginal.size());
		assertEquals("sopInstanceUidOriginal", transferStatusEntitiesOriginal.get(0).getSopInstanceUidOriginal());
		// To Send
		assertNotNull(transferStatusEntitiesToSend);
		assertFalse(transferStatusEntitiesToSend.isEmpty());
		assertEquals(1, transferStatusEntitiesToSend.size());
		assertEquals("sopInstanceUidToSend", transferStatusEntitiesToSend.get(0).getSopInstanceUidToSend());
	}

	@Test
	void shouldFilterByTransferStatusType() {
		// Init data
		TransferStatusFilter filter = new TransferStatusFilter();
		filter.setTransferStatusType(TransferStatusType.SENT);
		Specification<TransferStatusEntity> transferStatusSpecification = new TransferStatusSpecification(filter);

		// Call service
		List<TransferStatusEntity> transferStatusEntities = repository.findAll(transferStatusSpecification);

		// Test results
		assertNotNull(transferStatusEntities);
		assertFalse(transferStatusEntities.isEmpty());
		assertEquals(1, transferStatusEntities.size());
		assertEquals(TransferStatusType.SENT.getSent(), transferStatusEntities.get(0).isSent());
	}

	@Test
	void shouldFilterByTransferDate() {
		// Init data
		// In range
		TransferStatusFilter filterInRange = new TransferStatusFilter();
		filterInRange.setStart(LocalDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(5, 0, 0)));
		filterInRange.setEnd(LocalDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(7, 0, 0)));
		Specification<TransferStatusEntity> transferStatusSpecificationInRange = new TransferStatusSpecification(
				filterInRange);
		// Out of range
		TransferStatusFilter filterOutOfRange = new TransferStatusFilter();
		filterOutOfRange.setStart(LocalDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(12, 1, 0)));
		filterOutOfRange.setEnd(LocalDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(13, 0, 0)));
		Specification<TransferStatusEntity> transferStatusSpecificationOutOfRange = new TransferStatusSpecification(
				filterOutOfRange);

		// Call service
		List<TransferStatusEntity> transferStatusEntitiesInRange = repository
			.findAll(transferStatusSpecificationInRange);
		List<TransferStatusEntity> transferStatusEntitiesOutOfRange = repository
			.findAll(transferStatusSpecificationOutOfRange);

		// Test results
		// In Range
		assertNotNull(transferStatusEntitiesInRange);
		assertFalse(transferStatusEntitiesInRange.isEmpty());
		assertEquals(1, transferStatusEntitiesInRange.size());
		assertEquals(LocalDateTime.of(LocalDate.of(2022, 1, 1), LocalTime.of(6, 0, 0)),
				transferStatusEntitiesInRange.get(0).getTransferDate());
		// Out of range
		assertNotNull(transferStatusEntitiesOutOfRange);
		assertTrue(transferStatusEntitiesOutOfRange.isEmpty());
	}

}
