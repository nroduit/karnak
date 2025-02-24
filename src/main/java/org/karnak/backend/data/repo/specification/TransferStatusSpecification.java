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

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.io.Serial;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.karnak.backend.data.entity.TransferStatusEntity;
import org.karnak.backend.enums.TransferStatusType;
import org.karnak.frontend.monitoring.component.TransferStatusFilter;
import org.springframework.data.jpa.domain.Specification;

/**
 * Transfer status specification: used to look for entries depending on criteria
 */
public class TransferStatusSpecification implements Specification<TransferStatusEntity> {

	@Serial
	private static final long serialVersionUID = -939448741462690254L;

	// Like character
	private static final String LIKE = "%";

	// Criteria to look for
	private final TransferStatusFilter transferStatusFilter;

	/**
	 * Constructor with filter
	 * @param transferStatusFilter Criteria to look for
	 */
	public TransferStatusSpecification(TransferStatusFilter transferStatusFilter) {
		this.transferStatusFilter = transferStatusFilter;
	}

	@Override
	public Predicate toPredicate(Root<TransferStatusEntity> root, CriteriaQuery<?> query,
			CriteriaBuilder criteriaBuilder) {
		// Predicates to fill
		List<Predicate> predicates = new ArrayList<>();

		// Paths
		// Study Uid
		Path<String> pStudyUidOriginal = root.get("studyUidOriginal");
		Path<String> pStudyUidToSend = root.get("studyUidToSend");
		// Serie Uid
		Path<String> pSerieUidOriginal = root.get("serieUidOriginal");
		Path<String> pSerieUidToSend = root.get("serieUidToSend");
		// Sop Instance Uid
		Path<String> pSopInstanceUidOriginal = root.get("sopInstanceUidOriginal");
		Path<String> pSopInstanceUidToSend = root.get("sopInstanceUidToSend");
		// Status
		Path<Boolean> pSent = root.get("sent");
		// Transfer date
		Path<LocalDateTime> pTransferDate = root.get("transferDate");

		// Build criteria
		if (transferStatusFilter != null) {
			// Study Uid
			buildCriteriaStudyUid(criteriaBuilder, predicates, pStudyUidOriginal, pStudyUidToSend);
			// Serie Uid
			buildCriteriaSerieUid(criteriaBuilder, predicates, pSerieUidOriginal, pSerieUidToSend);
			// Sop Instance Uid
			buildCriteriaSopInstanceUid(criteriaBuilder, predicates, pSopInstanceUidOriginal, pSopInstanceUidToSend);
			// Sent
			buildCriteriaSent(criteriaBuilder, predicates, pSent);
			// Transfer Date
			buildCriteriaTransferDate(criteriaBuilder, predicates, pTransferDate);
		}

		return criteriaBuilder.and(predicates.toArray(new Predicate[] {}));
	}

	/**
	 * Build criteria for study uid
	 * @param criteriaBuilder CriteriaBuilder
	 * @param predicates Predicates to build
	 * @param pStudyUidOriginal Path of study uid original
	 * @param pStudyUidToSend Path of study uid to send
	 */
	private void buildCriteriaStudyUid(CriteriaBuilder criteriaBuilder, List<Predicate> predicates,
			Path<String> pStudyUidOriginal, Path<String> pStudyUidToSend) {
		if (transferStatusFilter.getStudyUid() != null && StringUtils.isNotBlank(transferStatusFilter.getStudyUid())) {
			predicates.add(criteriaBuilder.or(
					criteriaBuilder.like(pStudyUidOriginal, LIKE + transferStatusFilter.getStudyUid().trim() + LIKE),
					criteriaBuilder.like(pStudyUidToSend, LIKE + transferStatusFilter.getStudyUid().trim() + LIKE)));
		}
	}

	/**
	 * Build criteria for serie uid
	 * @param criteriaBuilder CriteriaBuilder
	 * @param predicates Predicates to build
	 * @param pSerieUidOriginal Path of serie uid original
	 * @param pSerieUidToSend Path of serie uid to send
	 */
	private void buildCriteriaSerieUid(CriteriaBuilder criteriaBuilder, List<Predicate> predicates,
			Path<String> pSerieUidOriginal, Path<String> pSerieUidToSend) {
		if (transferStatusFilter.getSerieUid() != null && StringUtils.isNotBlank(transferStatusFilter.getSerieUid())) {
			predicates.add(criteriaBuilder.or(
					criteriaBuilder.like(pSerieUidOriginal, LIKE + transferStatusFilter.getSerieUid() + LIKE),
					criteriaBuilder.like(pSerieUidToSend, LIKE + transferStatusFilter.getSerieUid() + LIKE)));
		}
	}

	/**
	 * Build criteria for sop instance uid
	 * @param criteriaBuilder CriteriaBuilder
	 * @param predicates Predicates to build
	 * @param pSopInstanceUidOriginal Path of sop instance uid original
	 * @param pSopInstanceUidToSend Path of sop instance uid to send
	 */
	private void buildCriteriaSopInstanceUid(CriteriaBuilder criteriaBuilder, List<Predicate> predicates,
			Path<String> pSopInstanceUidOriginal, Path<String> pSopInstanceUidToSend) {
		if (transferStatusFilter.getSopInstanceUid() != null
				&& StringUtils.isNotBlank(transferStatusFilter.getSopInstanceUid())) {
			predicates.add(criteriaBuilder.or(
					criteriaBuilder.like(pSopInstanceUidOriginal,
							LIKE + transferStatusFilter.getSopInstanceUid() + LIKE),
					criteriaBuilder.like(pSopInstanceUidToSend,
							LIKE + transferStatusFilter.getSopInstanceUid() + LIKE)));
		}
	}

	/**
	 * Build criteria for sent
	 * @param criteriaBuilder CriteriaBuilder
	 * @param predicates Predicates to build
	 * @param pSent Path of sent
	 */
	private void buildCriteriaSent(CriteriaBuilder criteriaBuilder, List<Predicate> predicates, Path<Boolean> pSent) {
		if (transferStatusFilter.getTransferStatusType() != null
				&& !Objects.equals(transferStatusFilter.getTransferStatusType(), TransferStatusType.ALL)) {
			predicates.add(criteriaBuilder.equal(pSent, transferStatusFilter.getTransferStatusType().getCode()));
		}
	}

	/**
	 * Build criteria for transfer date
	 * @param criteriaBuilder CriteriaBuilder
	 * @param predicates Predicates to build
	 * @param pTransferDate Path of transfer date
	 */
	private void buildCriteriaTransferDate(CriteriaBuilder criteriaBuilder, List<Predicate> predicates,
			Path<LocalDateTime> pTransferDate) {
		if (transferStatusFilter.getStart() != null && transferStatusFilter.getEnd() == null) {
			predicates.add(criteriaBuilder.greaterThanOrEqualTo(pTransferDate, transferStatusFilter.getStart()));
		}
		else if (transferStatusFilter.getStart() == null && transferStatusFilter.getEnd() != null) {
			predicates.add(criteriaBuilder.lessThanOrEqualTo(pTransferDate, transferStatusFilter.getEnd()));
		}
		else if (transferStatusFilter.getStart() != null && transferStatusFilter.getEnd() != null
				&& transferStatusFilter.getStart().isBefore(transferStatusFilter.getEnd())) {
			predicates.add(criteriaBuilder.between(pTransferDate, transferStatusFilter.getStart(),
					transferStatusFilter.getEnd()));
		}
	}

}
