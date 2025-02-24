/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.repo;

import java.time.LocalDateTime;
import java.util.List;
import org.karnak.backend.data.entity.TransferStatusEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferStatusRepo
		extends JpaRepository<TransferStatusEntity, Long>, JpaSpecificationExecutor<TransferStatusEntity> {

	/**
	 * Look for TransferStatusEntity for a destination
	 * @param destinationId Destination id
	 * @return TransferStatusEntity found
	 */
	List<TransferStatusEntity> findByDestinationId(Long destinationId);

	/**
	 * Look for TransferStatusEntity for a destination and after the last check date
	 * @param destinationId Destination id
	 * @param lastCheck Date of the last check
	 * @return TransferStatusEntity found
	 */
	List<TransferStatusEntity> findByDestinationIdAndTransferDateAfter(Long destinationId, LocalDateTime lastCheck);

	/**
	 * Look for oldest transfer status entities, number of entities found is limited by
	 * the pageRequest
	 * @param pageable Limit the number of records found
	 * @return TransferEntities found
	 */
	Page<TransferStatusEntity> findAllByOrderByTransferDateAsc(Pageable pageable);

}
