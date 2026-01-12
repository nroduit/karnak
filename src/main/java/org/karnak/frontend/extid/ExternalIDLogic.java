/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.extid;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExternalIDLogic {

	@Getter
	@Setter
	private ExternalIDView externalIDView;

	private final ProjectService projectService;

	@Autowired
	public ExternalIDLogic(final ProjectService projectService) {
		this.projectService = projectService;
		this.externalIDView = null;
	}

	public List<ProjectEntity> retrieveProject() {
		return projectService.getAllProjects();
	}

}
