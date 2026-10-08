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
import org.karnak.backend.data.entity.DicomNodeConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DicomNodeConfigRepo extends JpaRepository<DicomNodeConfigEntity, Long> {

	List<DicomNodeConfigEntity> findByNodeType(String nodeType);

	List<DicomNodeConfigEntity> findByNodeTypeNot(String nodeType);

	List<DicomNodeConfigEntity> findByNodeGroup(String nodeGroup);

	List<DicomNodeConfigEntity> findByNodeGroupIsNull();

	void deleteByNodeGroup(String nodeGroup);

	@Query("select distinct n.nodeGroup from DicomNodeConfig n where n.nodeGroup is not null order by n.nodeGroup")
	List<String> findDistinctNodeGroups();

	@Query("select distinct n.nodeType from DicomNodeConfig n order by n.nodeType")
	List<String> findDistinctNodeTypes();

}
