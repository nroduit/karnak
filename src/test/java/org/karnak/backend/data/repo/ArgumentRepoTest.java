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
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
@Slf4j
class ArgumentRepoTest {

	@Autowired
	private ArgumentRepo repository;

	@Autowired
	private ProfileElementRepo profileElementRepo;

	@Autowired
	private ProfileRepo profileRepo;

	/**
	 * Test save and find record.
	 */
	@Test
	void shouldSaveAndFindARecord() {
		// Create an entity to save
		ArgumentEntity entity = new ArgumentEntity();
		entity.setArgumentKey("Key");

		// Save the entity
		log.info("Saving entity with Key [{}]", entity.getArgumentKey());
		entity = repository.save(entity);

		// Test Save
		assertEquals("Key", entity.getArgumentKey());
		assertNotNull(entity.getId());
		log.info("Entity with Key [{}] and id [{}] saved", entity.getArgumentKey(), entity.getId());

		// Find By Id
		Optional<ArgumentEntity> foundByIdOpt = repository.findById(entity.getId());

		// Test Find by Id
		assertTrue(foundByIdOpt.isPresent());
		log.info("Entity found with Key [{}] and id [{}]", foundByIdOpt.get().getArgumentKey(),
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
		// Profile element
		ProfileElementEntity profileElementEntity = new ProfileElementEntity();
		profileElementEntity.setName("name");
		profileElementEntity.setProfileEntity(profileEntity);
		profileElementEntity = profileElementRepo.saveAndFlush(profileElementEntity);
		// Argument
		ArgumentEntity entity = new ArgumentEntity();
		entity.setArgumentKey("Key");
		entity.setProfileElementEntity(profileElementEntity);

		// Save the entity
		log.info("Saving entity with Key [{}]", entity.getArgumentKey());
		repository.saveAndFlush(entity);

		// Find all
		List<ArgumentEntity> all = repository.findAll();

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
		ArgumentEntity entity = new ArgumentEntity();
		entity.setArgumentKey(initialText);

		// Save the entity
		log.info("Saving entity with Key [{}]", entity.getArgumentKey());
		entity = repository.save(entity);
		log.info("Id of the entity with Key [{}]", entity.getId());

		// Test Save
		assertNotNull(entity);
		assertEquals(initialText, entity.getArgumentKey());

		// Modify the record
		entity.setArgumentKey(modifiedText);
		log.info("Modify entity Key [{}] to [{}]", initialText, modifiedText);
		ArgumentEntity entityModified = repository.save(entity);

		// Test Modify
		assertNotNull(entityModified);
		assertEquals(entity.getId(), entityModified.getId());
		assertEquals(modifiedText, entityModified.getArgumentKey());
		log.info("Key of the entity with id [{}]: [{}]", entityModified.getId(), entityModified.getArgumentKey());
	}

	/**
	 * Regression test for issue #258: the argument_value column used to be VARCHAR(255),
	 * so a long "expr" SpEL condition of an expression.on.tags element passed profile
	 * validation (which only checks SpEL syntax, not length) and then failed at save
	 * time. It is now mapped as unlimited text and must persist values well over 255
	 * characters.
	 */
	@Test
	void shouldPersistArgumentValueLongerThan255Characters() {
		// An argument requires its non-null parent profile element
		ProfileEntity profileEntity = new ProfileEntity();
		profileEntity.setName("name");
		profileEntity = profileRepo.saveAndFlush(profileEntity);
		ProfileElementEntity profileElementEntity = new ProfileElementEntity();
		profileElementEntity.setName("expression.on.tags");
		profileElementEntity.setProfileEntity(profileEntity);
		profileElementEntity = profileElementRepo.saveAndFlush(profileElementEntity);

		// Build an expr-like value longer than the former VARCHAR(255) limit
		String longExpr = "tagValueIsPresent(\"(0008,0060)\", \"CT\")"
			.concat(" || tagValueContains(\"(0008,1030)\", \"BRAIN\")".repeat(20));
		assertTrue(longExpr.length() > 255);

		ArgumentEntity entity = new ArgumentEntity("expr", longExpr, profileElementEntity);
		entity = repository.saveAndFlush(entity);

		// Reload from the database to confirm it round-trips untruncated
		Optional<ArgumentEntity> reloaded = repository.findById(entity.getId());
		assertTrue(reloaded.isPresent());
		assertEquals(longExpr, reloaded.get().getArgumentValue());
	}

	/**
	 * Test delete record.
	 */
	@Test
	void shouldDeleteRecord() {
		// Create an entity to save
		ArgumentEntity entity = new ArgumentEntity();
		String key = "Key";
		entity.setArgumentKey(key);

		// Save the entity
		log.info("Saving entity with Key [{}]", entity.getArgumentKey());
		entity = repository.save(entity);

		// Retrieve the entity
		Optional<ArgumentEntity> foundByIdOpt = repository.findById(entity.getId());

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
