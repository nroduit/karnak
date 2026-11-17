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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
		Mockito.verify(projectServiceMock, Mockito.times(1)).retrieveAllProjects();
	}

	@Test
	void should_find_project_by_uuid() {
		ProjectEntity first = new ProjectEntity();
		ProjectEntity second = new ProjectEntity();

		Optional<ProjectEntity> found = ExternalIDLogic.findProjectByUuid(List.of(first, second),
				second.getUuid().toString());

		assertTrue(found.isPresent());
		assertSame(second, found.get());
	}

	@Test
	void should_not_find_project_when_uuid_is_unknown() {
		List<ProjectEntity> projects = List.of(new ProjectEntity());

		assertTrue(ExternalIDLogic.findProjectByUuid(projects, UUID.randomUUID().toString()).isEmpty());
	}

	@Test
	void should_not_find_project_when_uuid_is_malformed_or_null() {
		List<ProjectEntity> projects = List.of(new ProjectEntity());

		assertTrue(ExternalIDLogic.findProjectByUuid(projects, "not-a-uuid").isEmpty());
		assertTrue(ExternalIDLogic.findProjectByUuid(projects, null).isEmpty());
	}

	@Test
	void should_not_find_project_in_empty_list() {
		assertTrue(ExternalIDLogic.findProjectByUuid(List.of(), UUID.randomUUID().toString()).isEmpty());
	}

}
