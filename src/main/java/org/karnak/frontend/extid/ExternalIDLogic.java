/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.extid;

import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.weasis.core.util.annotations.Generated;

@SpringComponent
@UIScope
@Generated()
@NullUnmarked
public class ExternalIDLogic {

	@Getter
	@Setter
	private ExternalIDView externalIDView;

	private final transient ProjectService projectService;

	@Autowired
	public ExternalIDLogic(final ProjectService projectService) {
		this.projectService = projectService;
		this.externalIDView = null;
	}

	public List<ProjectEntity> retrieveProject() {
		return projectService.retrieveAllProjects();
	}

	/**
	 * Finds, among the given projects, the one with the given uuid.
	 * @param projects the projects to search in
	 * @param uuid the project uuid as text (e.g. a query parameter); may be malformed
	 * @return the matching project; empty if the uuid is malformed or matches no project
	 */
	public static Optional<ProjectEntity> findProjectByUuid(List<ProjectEntity> projects, String uuid) {
		UUID projectUuid;
		try {
			projectUuid = UUID.fromString(uuid);
		}
		catch (IllegalArgumentException | NullPointerException e) {
			return Optional.empty();
		}
		return projects.stream().filter(project -> Objects.equals(projectUuid, project.getUuid())).findFirst();
	}

}
