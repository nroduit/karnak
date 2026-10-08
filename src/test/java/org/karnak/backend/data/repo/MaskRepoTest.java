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

import java.awt.Rectangle;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.MaskEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
@Slf4j
class MaskRepoTest {

	@Autowired
	private MaskRepo repository;

	@Autowired
	private ProfileRepo profileRepo;

	/**
	 * Test save and find record.
	 */
	@Test
	void shouldSaveAndFindARecord() {
		// Create an entity to save
		MaskEntity entity = new MaskEntity();
		entity.setStationName("Name");

		// Save the entity
		log.info("Saving entity with name [{}]", entity.getStationName());
		entity = repository.save(entity);

		// Test Save
		assertEquals("Name", entity.getStationName());
		assertNotNull(entity.getId());
		log.info("Entity with name [{}] and id [{}] saved", entity.getStationName(), entity.getId());

		// Find By Id
		Optional<MaskEntity> foundByIdOpt = repository.findById(entity.getId());

		// Test Find by Id
		assertTrue(foundByIdOpt.isPresent());
		log.info("Entity found with name [{}] and id [{}]", foundByIdOpt.get().getStationName(),
				foundByIdOpt.get().getId());
		assertEquals(entity.getId(), foundByIdOpt.get().getId());
	}

	/**
	 * Test find all.
	 */
	@Test
	void shouldFindAllRecords() {
		// Create an entity to save
		// Profile
		ProfileEntity profileEntity = new ProfileEntity();
		profileEntity.setName("name");
		profileEntity = profileRepo.saveAndFlush(profileEntity);
		// Mask
		MaskEntity entity = new MaskEntity();
		entity.setStationName("Name");
		entity.addRectangle(new Rectangle());
		entity.setProfileEntity(profileEntity);

		// Save the entity
		log.info("Saving entity with name [{}]", entity.getStationName());
		repository.saveAndFlush(entity);

		// Find all
		List<MaskEntity> all = repository.findAll();

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
		MaskEntity entity = new MaskEntity();
		entity.setStationName(initialText);

		// Save the entity
		log.info("Saving entity with name [{}]", entity.getStationName());
		entity = repository.save(entity);
		log.info("Id of the entity with name [{}]", entity.getId());

		// Test Save
		assertNotNull(entity);
		assertEquals(initialText, entity.getStationName());

		// Modify the record
		entity.setStationName(modifiedText);
		log.info("Modify entity name [{}] to [{}]", initialText, modifiedText);
		MaskEntity entityModified = repository.save(entity);

		// Test Modify
		assertNotNull(entityModified);
		assertEquals(entity.getId(), entityModified.getId());
		assertEquals(modifiedText, entityModified.getStationName());
		log.info("Name of the entity with id [{}]: [{}]", entityModified.getId(), entityModified.getStationName());
	}

	/**
	 * Test delete record.
	 */
	@Test
	void shouldDeleteRecord() {
		// Create an entity to save
		MaskEntity entity = new MaskEntity();
		String name = "Name";
		entity.setStationName(name);
		entity.addRectangle(new Rectangle());

		// Save the entity
		log.info("Saving entity with name [{}]", entity.getStationName());
		entity = repository.save(entity);

		// Retrieve the entity
		Optional<MaskEntity> foundByIdOpt = repository.findById(entity.getId());

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
