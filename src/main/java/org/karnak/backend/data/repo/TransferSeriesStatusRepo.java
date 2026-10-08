/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.repo;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.karnak.backend.data.entity.TransferSeriesStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TransferSeriesStatusRepo
		extends JpaRepository<TransferSeriesStatusEntity, Long>, JpaSpecificationExecutor<TransferSeriesStatusEntity> {

	/**
	 * Locks (PESSIMISTIC_WRITE) and returns the aggregate row for a series so concurrent
	 * counter increments cannot lose updates.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<TransferSeriesStatusEntity> findWithLockByForwardNodeIdAndDestinationIdAndSerieUidOriginal(
			Long forwardNodeId, Long destinationId, String serieUidOriginal);

	/** Series rows touched after the last notification check, for a destination. */
	List<TransferSeriesStatusEntity> findByDestinationIdAndLastSeenAfter(Long destinationId, LocalDateTime lastSeen);

	/** All series rows for a destination (first notification run). */
	List<TransferSeriesStatusEntity> findByDestinationId(Long destinationId);

	@Transactional
	@Modifying
	@Query("DELETE FROM TransferSeriesStatus s WHERE s.lastSeen < :threshold")
	void deleteOlderThan(@Param("threshold") LocalDateTime threshold);

}
