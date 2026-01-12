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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.service.ProjectService;
import org.mockito.Mockito;

class ExternalIDLogicTest {

	// Service
	private ExternalIDLogic externalIDLogic;

	private final ProjectService projectServiceMock = Mockito.mock(ProjectService.class);

	@BeforeEach
	public void setUp() {
		// Build mocked service
		externalIDLogic = new ExternalIDLogic(projectServiceMock);
	}

	@Test
	void should_retrieve_projects() {
		// Call method
		List<ProjectEntity> projectEntities = externalIDLogic.retrieveProject();

		// Test result
		Mockito.verify(projectServiceMock, Mockito.times(1)).getAllProjects();
	}

}
