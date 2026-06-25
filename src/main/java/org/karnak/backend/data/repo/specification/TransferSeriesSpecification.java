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
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.io.Serial;
import java.util.List;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;
import org.springframework.data.jpa.domain.Specification;

/** Specification over {@code transfer_series_status} for the filtered CSV export. */
public class TransferSeriesSpecification implements Specification<TransferSeriesStatusEntity> {

	@Serial
	private static final long serialVersionUID = 1L;

	private final TransferStatusFilter transferStatusFilter;

	public TransferSeriesSpecification(TransferStatusFilter transferStatusFilter) {
		this.transferStatusFilter = transferStatusFilter;
	}

	@Override
	public Predicate toPredicate(Root<TransferSeriesStatusEntity> root, CriteriaQuery<?> query,
			CriteriaBuilder criteriaBuilder) {
		List<Predicate> predicates = TransferSeriesPredicates.build(root, criteriaBuilder, transferStatusFilter);
		return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
	}

}
