/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.repo;

import java.util.List;
import org.karnak.backend.data.entity.DicomNodeCheckHistoryEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DicomNodeCheckHistoryRepo extends JpaRepository<DicomNodeCheckHistoryEntity, Long> {

	// Most recent first, for display (limited via Pageable).
	List<DicomNodeCheckHistoryEntity> findByOrderByCheckedAtDesc(Pageable pageable);

	// Oldest first, used to select the surplus rows to prune.
	List<DicomNodeCheckHistoryEntity> findByOrderByCheckedAtAsc(Pageable pageable);

}