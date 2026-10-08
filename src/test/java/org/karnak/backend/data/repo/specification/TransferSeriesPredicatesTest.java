/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.repo.specification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.karnak.backend.enums.TransferStatusType;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TransferSeriesPredicatesTest {

	private CriteriaBuilder criteriaBuilder;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Root<TransferSeriesStatusEntity> root;

	@BeforeEach
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void setUp() {
		criteriaBuilder = Mockito.mock(CriteriaBuilder.class);
		root = Mockito.mock(Root.class);
		Predicate predicate = Mockito.mock(Predicate.class);
		Path path = Mockito.mock(Path.class);

		Mockito.doReturn(path).when(root).get(ArgumentMatchers.anyString());
		Mockito.lenient()
			.when(criteriaBuilder.like(ArgumentMatchers.any(), ArgumentMatchers.anyString()))
			.thenReturn(predicate);
		Mockito.lenient()
			.when(criteriaBuilder.or(ArgumentMatchers.any(), ArgumentMatchers.any()))
			.thenReturn(predicate);
		Mockito.lenient()
			.when(criteriaBuilder.equal(ArgumentMatchers.any(), ArgumentMatchers.any()))
			.thenReturn(predicate);
		Mockito.lenient()
			.when(criteriaBuilder.greaterThan(ArgumentMatchers.any(Expression.class), ArgumentMatchers.any(Long.class)))
			.thenReturn(predicate);
		Mockito.lenient()
			.when(criteriaBuilder.greaterThan(ArgumentMatchers.any(Expression.class),
					ArgumentMatchers.any(Expression.class)))
			.thenReturn(predicate);
		Mockito.lenient()
			.when(criteriaBuilder.greaterThanOrEqualTo(ArgumentMatchers.any(),
					ArgumentMatchers.any(LocalDateTime.class)))
			.thenReturn(predicate);
		Mockito.lenient()
			.when(criteriaBuilder.lessThanOrEqualTo(ArgumentMatchers.any(), ArgumentMatchers.any(LocalDateTime.class)))
			.thenReturn(predicate);
		Mockito.lenient()
			.when(criteriaBuilder.between(ArgumentMatchers.any(), ArgumentMatchers.any(LocalDateTime.class),
					ArgumentMatchers.any(LocalDateTime.class)))
			.thenReturn(predicate);
	}

	private int count(TransferStatusFilter filter) {
		return TransferSeriesPredicates.build(root, criteriaBuilder, filter).size();
	}

	@Test
	void empty_filter_produces_no_predicate() {
		assertEquals(0, count(new TransferStatusFilter()));
	}

	@Test
	void uid_filter_adds_one_like_predicate() {
		TransferStatusFilter filter = new TransferStatusFilter();
		filter.setStudyUid("1.2.3");
		assertEquals(1, count(filter));
	}

	@Test
	void valid_range_adds_one_between_predicate() {
		TransferStatusFilter filter = new TransferStatusFilter();
		LocalDateTime now = LocalDateTime.now();
		filter.setStart(now);
		filter.setEnd(now.plusHours(1));
		assertEquals(1, count(filter));
	}

	@Test
	void inverted_range_adds_no_date_predicate() {
		TransferStatusFilter filter = new TransferStatusFilter();
		LocalDateTime now = LocalDateTime.now();
		filter.setStart(now.plusHours(1));
		filter.setEnd(now);
		assertEquals(0, count(filter));
	}

	@Test
	void sent_status_adds_one_counter_predicate() {
		TransferStatusFilter filter = new TransferStatusFilter();
		filter.setTransferStatusType(TransferStatusType.SENT);
		assertEquals(1, count(filter));
	}

	@Test
	void error_status_adds_one_counter_predicate() {
		TransferStatusFilter filter = new TransferStatusFilter();
		filter.setTransferStatusType(TransferStatusType.ERROR);
		assertEquals(1, count(filter));
	}

	@Test
	void not_sent_status_adds_one_counter_predicate() {
		TransferStatusFilter filter = new TransferStatusFilter();
		filter.setTransferStatusType(TransferStatusType.NOT_SENT);
		assertEquals(1, count(filter));
	}

	@Test
	void excluded_status_adds_two_counter_predicates() {
		TransferStatusFilter filter = new TransferStatusFilter();
		filter.setTransferStatusType(TransferStatusType.EXCLUDED);
		assertEquals(2, count(filter));
	}

}
