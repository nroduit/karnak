/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.model.project.ProjectDeleteBlockerModel;
import org.karnak.backend.model.project.ProjectDeleteBlockersModel;
import org.karnak.backend.service.ProjectDeleteBlockedException;
import org.karnak.backend.service.ProjectService;
import org.karnak.backend.service.SecretService;
import org.karnak.backend.service.profilepipe.ProfilePipeService;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ProjectControllerTest {

	private final ProjectService projectServiceMock = Mockito.mock(ProjectService.class);

	private ProjectController projectController;

	private final UUID projectUuid = UUID.randomUUID();

	private ProjectEntity projectEntity;

	@BeforeEach
	void setUp() {
		projectController = new ProjectController(projectServiceMock, Mockito.mock(ProfilePipeService.class),
				Mockito.mock(SecretService.class));
		projectEntity = new ProjectEntity();
		projectEntity.setId(1L);
		projectEntity.setUuid(projectUuid);
		Mockito.when(projectServiceMock.retrieveProjectByUuid(projectUuid)).thenReturn(projectEntity);
	}

	private ProjectDeleteBlockersModel blockers() {
		ProjectDeleteBlockerModel blocker = new ProjectDeleteBlockerModel();
		blocker.setDestinationUuid(UUID.randomUUID());
		ProjectDeleteBlockersModel blockers = new ProjectDeleteBlockersModel();
		blockers.setProjectUuid(projectUuid);
		blockers.setBlockers(List.of(blocker));
		return blockers;
	}

	@Test
	void should_return_not_found_when_listing_blockers_of_unknown_project() {
		UUID unknown = UUID.randomUUID();
		Mockito.when(projectServiceMock.retrieveProjectByUuid(unknown)).thenReturn(null);

		ResponseEntity<ProjectDeleteBlockersModel> response = projectController.listDeleteBlockers(unknown);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	void should_list_delete_blockers() {
		ProjectDeleteBlockersModel blockers = blockers();
		Mockito.when(projectServiceMock.collectDeleteBlockers(projectEntity)).thenReturn(blockers);

		ResponseEntity<ProjectDeleteBlockersModel> response = projectController.listDeleteBlockers(projectUuid);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertSame(blockers, response.getBody());
	}

	@Test
	void should_return_not_found_when_deleting_unknown_project() {
		UUID unknown = UUID.randomUUID();
		Mockito.when(projectServiceMock.retrieveProjectByUuid(unknown)).thenReturn(null);

		ResponseEntity<?> response = projectController.deleteProject(unknown);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		Mockito.verify(projectServiceMock, Mockito.never()).remove(Mockito.any());
	}

	@Test
	void should_delete_project() {
		ResponseEntity<?> response = projectController.deleteProject(projectUuid);

		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
		Mockito.verify(projectServiceMock).remove(projectEntity);
	}

	@Test
	void should_return_conflict_with_blockers_when_delete_is_blocked() {
		ProjectDeleteBlockersModel blockers = blockers();
		Mockito.doThrow(new ProjectDeleteBlockedException(blockers)).when(projectServiceMock).remove(projectEntity);

		ResponseEntity<?> response = projectController.deleteProject(projectUuid);

		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertSame(blockers, response.getBody());
	}

}
