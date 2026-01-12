/*
 * Copyright (c) 2022 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.entity.SecretEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@Slf4j
class SecretRepoTest {

	@Autowired
	private SecretRepo repository;

	@Autowired
	private ProjectRepo projectRepo;

	/**
	 * Add a Secret entity in Db
	 * @param projectEntity Project to link
	 * @return Secret saved
	 */
	private SecretEntity addSecretEntityInDb(ProjectEntity projectEntity) {
		SecretEntity entity = new SecretEntity();
		entity.setProjectEntity(projectEntity);

		// Save the entity
		log.info("Saving entity with Project [{}]",
				"%d|%s".formatted(entity.getProjectEntity().getId(), entity.getProjectEntity().getName()));
		entity = repository.saveAndFlush(entity);
		return entity;
	}

	/**
	 * Add project in Db
	 * @return project saved
	 */
	private ProjectEntity addProjectEntityInDb() {
		ProjectEntity projectEntity = new ProjectEntity();
		projectEntity.setName("projectName");
		return projectRepo.saveAndFlush(projectEntity);
	}

	/**
	 * Test save and find record.
	 */
	@Test
	void shouldSaveAndFindARecord() {
		// Create an entity to save
		ProjectEntity projectEntitySaved = addProjectEntityInDb();
		SecretEntity entity = addSecretEntityInDb(projectEntitySaved);

		// Test Save
		assertEquals(projectEntitySaved.getId(), entity.getProjectEntity().getId());
		assertEquals("projectName", entity.getProjectEntity().getName());
		assertNotNull(entity.getId());
		log.info("Entity with Project [{}] and id [{}] saved",
				"%d|%s".formatted(entity.getProjectEntity().getId(), entity.getProjectEntity().getName()),
				entity.getId());

		// Find By Id
		Optional<SecretEntity> foundByIdOpt = repository.findById(entity.getId());

		// Test Find by Id
		assertTrue(foundByIdOpt.isPresent());
		SecretEntity secretEntityFound = foundByIdOpt.get();
		log.info("Entity found with Project [{}] and id [{}]", "%d|%s"
			.formatted(secretEntityFound.getProjectEntity().getId(), secretEntityFound.getProjectEntity().getName()),
				secretEntityFound.getId());
		assertEquals(entity.getId(), secretEntityFound.getId());
	}

	/**
	 * Test find all.
	 */
	@Test
	void shouldFindAllRecords() {
		// Create an entity to save
		ProjectEntity projectEntitySaved = addProjectEntityInDb();
		SecretEntity entity = addSecretEntityInDb(projectEntitySaved);

		// Find all
		List<SecretEntity> all = repository.findAll();

		// Test find all
		assertNotNull(all);
		assertTrue(all.size() > 0);
		assertEquals(1, all.size());
		log.info("Number of entities found [{}]", all.size());
	}

	/**
	 * Test modification of a record.
	 */
	@Test
	void shouldModifyRecord() {

		String initialText = "InitialText";
		String modifiedText = "ModifiedText";

		// Create an entity to save
		ProjectEntity projectEntitySaved = addProjectEntityInDb();
		SecretEntity entity = addSecretEntityInDb(projectEntitySaved);
		entity.getProjectEntity().setName(initialText);

		// Save the entity
		log.info("Saving entity with project name [{}]", entity.getProjectEntity().getName());
		entity = repository.save(entity);
		log.info("Id of the entity with project name [{}]", entity.getProjectEntity().getName());

		// Test Save
		assertNotNull(entity);
		assertEquals(initialText, entity.getProjectEntity().getName());

		// Modify the record
		entity.getProjectEntity().setName(modifiedText);
		log.info("Modify entity project name [{}] to [{}]", initialText, modifiedText);
		SecretEntity entityModified = repository.save(entity);

		// Test Modify
		assertNotNull(entityModified);
		assertEquals(entity.getId(), entityModified.getId());
		assertEquals(modifiedText, entityModified.getProjectEntity().getName());
		log.info("Project name of the entity with id [{}]: [{}]", entityModified.getId(),
				entityModified.getProjectEntity().getName());
	}

	/**
	 * Test delete record.
	 */
	@Test
	void shouldDeleteRecord() {
		// Create an entity to save
		ProjectEntity projectEntitySaved = addProjectEntityInDb();
		SecretEntity entity = addSecretEntityInDb(projectEntitySaved);

		// Retrieve the entity
		Optional<SecretEntity> foundByIdOpt = repository.findById(entity.getId());

		// Test Find by Id
		assertTrue(foundByIdOpt.isPresent());

		// Delete the entity
		entity = foundByIdOpt.get();
		Long id = entity.getId();
		log.info("Deleting entity with id [{}]", id);
		repository.delete(entity);

		// Test Delete
		foundByIdOpt = repository.findById(id);
		log.info("Is deleted entity with id [{}] present: [{}]", id, foundByIdOpt.isPresent());
		assertFalse(foundByIdOpt.isPresent());
	}

}
