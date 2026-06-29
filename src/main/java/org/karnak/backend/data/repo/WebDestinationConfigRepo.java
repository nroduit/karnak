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
import org.karnak.backend.data.entity.WebDestinationConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface WebDestinationConfigRepo extends JpaRepository<WebDestinationConfigEntity, Long> {

	List<WebDestinationConfigEntity> findByGroupName(String groupName);

	List<WebDestinationConfigEntity> findByGroupNameIsNull();

	boolean existsByUrl(String url);

	void deleteByGroupName(String groupName);

	@Query("select distinct w.groupName from WebDestinationConfig w where w.groupName is not null order by w.groupName")
	List<String> findDistinctGroupNames();

}
