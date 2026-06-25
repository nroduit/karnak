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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.karnak.backend.enums.TransferStatusType;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;

/**
 * Builds the {@link Predicate}s matching a {@link TransferStatusFilter} on the aggregated
 * {@code transfer_series_status} root. Shared by the CSV specification and the monitoring
 * aggregation queries so the tree, dashboard and export honor the same filter. Status
 * filtering is mapped onto the per-series counters; the date range applies to
 * {@code lastSeen}.
 */
public final class TransferSeriesPredicates {

	private static final String LIKE = "%";

	private TransferSeriesPredicates() {
	}

	public static List<Predicate> build(Root<TransferSeriesStatusEntity> root, CriteriaBuilder criteriaBuilder,
			TransferStatusFilter filter) {
		List<Predicate> predicates = new ArrayList<>();
		if (filter != null) {
			addLikePredicate(criteriaBuilder, predicates, filter.getStudyUid(), root.get("studyUidOriginal"),
					root.get("studyUidToSend"));
			addLikePredicate(criteriaBuilder, predicates, filter.getSerieUid(), root.get("serieUidOriginal"),
					root.get("serieUidToSend"));
			addStatusPredicate(criteriaBuilder, predicates, filter, root);
			addDatePredicate(criteriaBuilder, predicates, filter, root.get("lastSeen"));
		}
		return predicates;
	}

	private static void addLikePredicate(CriteriaBuilder criteriaBuilder, List<Predicate> predicates, String value,
			Path<String> original, Path<String> toSend) {
		if (StringUtils.isNotBlank(value)) {
			String pattern = LIKE + value.trim() + LIKE;
			predicates.add(
					criteriaBuilder.or(criteriaBuilder.like(original, pattern), criteriaBuilder.like(toSend, pattern)));
		}
	}

	/** Maps the selected status onto the series counters. */
	private static void addStatusPredicate(CriteriaBuilder criteriaBuilder, List<Predicate> predicates,
			TransferStatusFilter filter, Root<TransferSeriesStatusEntity> root) {
		TransferStatusType type = filter.getTransferStatusType();
		if (type == null || type == TransferStatusType.ALL) {
			return;
		}
		Path<Long> instances = root.get("instances");
		Path<Long> sent = root.get("sent");
		Path<Long> errors = root.get("errors");
		switch (type) {
			case SENT -> predicates.add(criteriaBuilder.greaterThan(sent, 0L));
			case ERROR -> predicates.add(criteriaBuilder.greaterThan(errors, 0L));
			case NOT_SENT -> predicates.add(criteriaBuilder.greaterThan(instances, sent));
			case EXCLUDED -> {
				predicates.add(criteriaBuilder.greaterThan(instances, sent));
				predicates.add(criteriaBuilder.equal(errors, 0L));
			}
			default -> {
				// ALL handled above
			}
		}
	}

	/** Date range applied to the series' most recent activity. */
	private static void addDatePredicate(CriteriaBuilder criteriaBuilder, List<Predicate> predicates,
			TransferStatusFilter filter, Path<LocalDateTime> lastSeen) {
		LocalDateTime start = filter.getStart();
		LocalDateTime end = filter.getEnd();
		if (start != null && end == null) {
			predicates.add(criteriaBuilder.greaterThanOrEqualTo(lastSeen, start));
		}
		else if (start == null && end != null) {
			predicates.add(criteriaBuilder.lessThanOrEqualTo(lastSeen, end));
		}
		else if (start != null && end != null && start.isBefore(end)) {
			predicates.add(criteriaBuilder.between(lastSeen, start, end));
		}
	}

}
