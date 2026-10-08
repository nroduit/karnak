/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
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
import org.karnak.backend.data.entity.SOPClassUIDEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
@Slf4j
class SOPClassUIDRepoTest {

	@Autowired
	private SOPClassUIDRepo repository;

	/**
	 * Test save and find record.
	 */
	@Test
	void shouldSaveAndFindARecord() {
		// Create an entity to save
		SOPClassUIDEntity entity = new SOPClassUIDEntity();
		entity.setName("Name");

		// Save the entity
		log.info("Saving entity with name [{}]", entity.getName());
		entity = repository.save(entity);

		// Test Save
		assertEquals("Name", entity.getName());
		assertNotNull(entity.getId());
		log.info("Entity with name [{}] and id [{}] saved", entity.getName(), entity.getId());

		// Find By Id
		Optional<SOPClassUIDEntity> foundByIdOpt = repository.findById(entity.getId());

		// Test Find by Id
		assertTrue(foundByIdOpt.isPresent());
		log.info("Entity found with name [{}] and id [{}]", foundByIdOpt.get().getName(), foundByIdOpt.get().getId());
		assertEquals(entity.getId(), foundByIdOpt.get().getId());
	}

	/**
	 * Test find all.
	 */
	@Test
	void shouldFindAllRecords() {
		// Create an entity to save
		SOPClassUIDEntity entity = new SOPClassUIDEntity();
		entity.setName("Name");

		// Save the entity
		log.info("Saving entity with name [{}]", entity.getName());
		repository.saveAndFlush(entity);

		// Find all
		List<SOPClassUIDEntity> all = repository.findAll();

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
		SOPClassUIDEntity entity = new SOPClassUIDEntity();
		entity.setName(initialText);

		// Save the entity
		log.info("Saving entity with name [{}]", entity.getName());
		entity = repository.save(entity);
		log.info("Id of the entity with name [{}]", entity.getId());

		// Test Save
		assertNotNull(entity);
		assertEquals(initialText, entity.getName());

		// Modify the record
		entity.setName(modifiedText);
		log.info("Modify entity name [{}] to [{}]", initialText, modifiedText);
		SOPClassUIDEntity entityModified = repository.save(entity);

		// Test Modify
		assertNotNull(entityModified);
		assertEquals(entity.getId(), entityModified.getId());
		assertEquals(modifiedText, entityModified.getName());
		log.info("Name of the entity with id [{}]: [{}]", entityModified.getId(), entityModified.getName());
	}

	/**
	 * Test delete record.
	 */
	@Test
	void shouldDeleteRecord() {
		// Create an entity to save
		SOPClassUIDEntity entity = new SOPClassUIDEntity();
		String name = "Name";
		entity.setName(name);

		// Save the entity
		log.info("Saving entity with name [{}]", entity.getName());
		entity = repository.save(entity);

		// Retrieve the entity
		Optional<SOPClassUIDEntity> foundByIdOpt = repository.findById(entity.getId());

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
