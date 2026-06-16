/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.repo.ProjectRepo;
import org.karnak.backend.model.event.NodeEvent;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

class ProjectServiceTest {

	// Application Event Publisher
	private final ApplicationEventPublisher applicationEventPublisherMock = Mockito
		.mock(ApplicationEventPublisher.class);

	// Repositories
	private final ProjectRepo projectRepositoryMock = Mockito.mock(ProjectRepo.class);

	// Service
	private ProjectService projectService;

	@BeforeEach
	public void setUp() {
		// Init data
		ProjectEntity projectEntity = new ProjectEntity();
		projectEntity.setId(1L);
		projectEntity.setName("projectEntityName");

		// Mock repositories
		Mockito.when(projectRepositoryMock.findAll()).thenReturn(Collections.singletonList(projectEntity));
		Mockito.when(projectRepositoryMock.findById(Mockito.anyLong())).thenReturn(Optional.of(projectEntity));

		// Build mocked service
		projectService = new ProjectService(projectRepositoryMock, applicationEventPublisherMock);
	}

	@Test
	void should_call_save_from_repository() {
		// Init data
		ProjectEntity projectEntity = new ProjectEntity();

		// Call service
		projectService.save(projectEntity);

		// Test results
		Mockito.verify(projectRepositoryMock, Mockito.times(1)).saveAndFlush(Mockito.any(ProjectEntity.class));
	}

	@Test
	void should_save_project_and_publish_event_for_each_destinations() {
		// Init data
		// Create project
		ProjectEntity projectEntity = new ProjectEntity();
		projectEntity.setId(1L);
		// Set destinations
		DestinationEntity destinationEntityFirst = new DestinationEntity();
		DestinationEntity destinationEntitySecond = new DestinationEntity();
		ForwardNodeEntity forwardNodeEntityFirst = new ForwardNodeEntity();
		ForwardNodeEntity forwardNodeEntitySecond = new ForwardNodeEntity();
		destinationEntityFirst.setForwardNodeEntity(forwardNodeEntityFirst);
		destinationEntitySecond.setForwardNodeEntity(forwardNodeEntitySecond);
		projectEntity.setDestinationEntities(Arrays.asList(destinationEntityFirst, destinationEntitySecond));

		// Call service
		projectService.update(projectEntity);

		// Test results
		Mockito.verify(projectRepositoryMock, Mockito.times(1)).saveAndFlush(Mockito.any(ProjectEntity.class));
		Mockito.verify(applicationEventPublisherMock, Mockito.times(2)).publishEvent(Mockito.any(NodeEvent.class));
	}

	@Test
	void should_call_delete_from_repository() {
		// Init data
		ProjectEntity projectEntity = new ProjectEntity();
		projectEntity.setId(1L);

		// Call service
		projectService.remove(projectEntity);

		// Test results
		Mockito.verify(projectRepositoryMock, Mockito.times(1)).deleteById(Mockito.anyLong());
	}

	@Test
	void should_retrieve_all_projects() {
		// Init data
		ProjectEntity projectEntity = new ProjectEntity();
		projectEntity.setId(1L);

		// Call service
		List<ProjectEntity> projects = projectService.getAllProjects();

		// Test results
		Mockito.verify(projectRepositoryMock, Mockito.times(1)).findAll();
		assertNotNull(projects);
		assertEquals(1, projects.size());
		assertEquals("projectEntityName", projects.get(0).getName());
	}

	@Test
	void should_retrieve_project_by_id() {
		// Call service
		ProjectEntity projectEntity = projectService.retrieveProject(1L);

		// Test results
		Mockito.verify(projectRepositoryMock, Mockito.times(1)).findById(Mockito.anyLong());
		assertNotNull(projectEntity);
		assertEquals(Long.valueOf(1), projectEntity.getId());
		assertEquals("projectEntityName", projectEntity.getName());
	}

}
