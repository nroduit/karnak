/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.repo;

import java.util.List;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DestinationRepo extends JpaRepository<DestinationEntity, Long> {

	/**
	 * Destinations that reference the given project as their <b>tag-morphing</b> project.
	 * {@code ProjectEntity} only maps the inverse side of the de-identification
	 * association ({@code getDestinationEntities()}), so tag-morphing destinations must
	 * be looked up explicitly.
	 * @param tagMorphingProjectEntity the tag-morphing project
	 * @return the destinations using it for tag morphing
	 */
	List<DestinationEntity> findByTagMorphingProjectEntity(ProjectEntity tagMorphingProjectEntity);

}
