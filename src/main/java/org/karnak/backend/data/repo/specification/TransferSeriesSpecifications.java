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

import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.UUID;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;
import org.springframework.data.jpa.domain.Specification;

/**
 * Small ad-hoc {@link Specification}s on {@code transfer_series_status}, composed by the
 * monitoring aggregation queries and the CSV export so every level of the hierarchy
 * (destination / study / series / errors) is scoped through specifications rather than
 * hand-rolled predicates. The scope restrictions ({@code hasXxx}) are optional: a
 * {@code null} value means "no restriction" (unlike a plain equality check, which would
 * otherwise only match rows with a null column).
 */
public final class TransferSeriesSpecifications {

	private TransferSeriesSpecifications() {
	}

	/** Restricts to rows matching the given search filter (status, dates, UID patterns). */
	public static Specification<TransferSeriesStatusEntity> matchesFilter(TransferStatusFilter filter) {
		return (root, query, criteriaBuilder) -> {
			List<Predicate> predicates = TransferSeriesPredicates.build(root, criteriaBuilder, filter);
			return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
		};
	}

	/** Restricts to rows for the given destination (its stable UUID). */
	public static Specification<TransferSeriesStatusEntity> hasDestinationUuid(UUID destinationUuid) {
		return (root, query, criteriaBuilder) -> destinationUuid == null ? criteriaBuilder.conjunction()
				: criteriaBuilder.equal(root.get("destinationEntity").get("uuid"), destinationUuid);
	}

	/** Restricts to rows for the given original study UID. */
	public static Specification<TransferSeriesStatusEntity> hasStudyUidOriginal(String studyUid) {
		return (root, query, criteriaBuilder) -> studyUid == null ? criteriaBuilder.conjunction()
				: criteriaBuilder.equal(root.get("studyUidOriginal"), studyUid);
	}

	/** Restricts to rows for the given original series UID. */
	public static Specification<TransferSeriesStatusEntity> hasSerieUidOriginal(String serieUid) {
		return (root, query, criteriaBuilder) -> serieUid == null ? criteriaBuilder.conjunction()
				: criteriaBuilder.equal(root.get("serieUidOriginal"), serieUid);
	}

}

