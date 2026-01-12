/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.repo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.DicomSourceNodeEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.enums.DestinationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

// @AutoConfigureTestDatabase(replace = Replace.NONE)
@DataJpaTest
@Slf4j
class ForwardNodeRepoTest {

	private final Consumer<ForwardNodeEntity> forwardNodeConsumer = //
			x -> assertThat(x) //
				.hasFieldOrPropertyWithValue("fwdDescription", "description") //
				.hasFieldOrPropertyWithValue("fwdAeTitle", "fwdAeTitle") //
				.extracting(Object::toString)
				.asString()
				.matches("^ForwardNode \\[.*");

	private final Consumer<DicomSourceNodeEntity> sourceNodeConsumer = //
			x -> assertThat(x) //
				.hasFieldOrPropertyWithValue("description", "description") //
				.hasFieldOrPropertyWithValue("aeTitle", "aeTitle") //
				.hasFieldOrPropertyWithValue("hostname", "hostname") //
				.hasFieldOrPropertyWithValue("checkHostname", true) //
				.extracting(Object::toString)
				.asString()
				.matches("^DicomSourceNode \\[.*");

	private final Consumer<DestinationEntity> destinationDicomConsumer = //
			x -> assertThat(x) //
				.hasFieldOrPropertyWithValue("description", "description") //
				.hasFieldOrPropertyWithValue("type", DestinationType.dicom) //
				.hasFieldOrPropertyWithValue("aeTitle", "aeTitle") //
				.hasFieldOrPropertyWithValue("hostname", "hostname") //
				.hasFieldOrPropertyWithValue("port", 123) //
				.extracting(Object::toString)
				.asString()
				.matches("^Destination \\[.*");

	private final Consumer<DestinationEntity> destinationStowConsumer = //
			x -> assertThat(x) //
				.hasFieldOrPropertyWithValue("description", "description") //
				.hasFieldOrPropertyWithValue("type", DestinationType.stow) //
				.hasFieldOrPropertyWithValue("url", "url") //
				.hasFieldOrPropertyWithValue("urlCredentials", "urlCredentials") //
				.hasFieldOrPropertyWithValue("headers", "headers") //
				.extracting(Object::toString)
				.asString()
				.matches("^Destination \\[.*");

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private ForwardNodeRepo repository;

	@Test
	void testInvalidForwardNode_Mandatory() {
		ForwardNodeEntity forwardNodeEntity = ForwardNodeEntity.ofEmpty();
		forwardNodeEntity.setFwdAeTitle(null);
		String expectedMessage = "Forward AETitle is mandatory";
		Exception exception = assertThrows(ConstraintViolationException.class, () -> {
			entityManager.persistAndFlush(forwardNodeEntity);
		});
		String actualMessage = exception.getMessage();
		assertTrue(actualMessage.contains(expectedMessage));
	}

	@Test
	void testInvalidForwardNode_Size() {
		ForwardNodeEntity forwardNodeEntity = ForwardNodeEntity.ofEmpty();
		forwardNodeEntity.setFwdAeTitle("ABCDEFGHIJ-ABCDEFGHIJ");
		String expectedMessage = "Forward AETitle has more than 16 characters";
		Exception exception = assertThrows(ConstraintViolationException.class, () -> {
			entityManager.persistAndFlush(forwardNodeEntity);
		});
		String actualMessage = exception.getMessage();
		assertTrue(actualMessage.contains(expectedMessage));
	}

	@Test
	void testInvalidSourceNode_AETitle_mandatory() {
		ForwardNodeEntity forwardNodeEntity = ForwardNodeEntity.ofEmpty();
		forwardNodeEntity.setFwdDescription("description");
		forwardNodeEntity.setFwdAeTitle("fwdAeTitle");
		DicomSourceNodeEntity sourceNode = DicomSourceNodeEntity.ofEmpty();
		sourceNode.setDescription("description");
		sourceNode.setAeTitle(null);
		sourceNode.setHostname("hostname");
		forwardNodeEntity.addSourceNode(sourceNode);

		String expectedMessage = "AETitle is mandatory";
		Exception exception = assertThrows(ConstraintViolationException.class, () -> {
			entityManager.persistAndFlush(forwardNodeEntity);
		});
		String actualMessage = exception.getMessage();
		assertTrue(actualMessage.contains(expectedMessage));
	}

	@Test
	void testInvalidDestinationDicom_AETitle_mandatory() {
		ForwardNodeEntity forwardNodeEntity = ForwardNodeEntity.ofEmpty();
		forwardNodeEntity.setFwdDescription("description");
		forwardNodeEntity.setFwdAeTitle("fwdAeTitle");
		DestinationEntity destinationEntity = DestinationEntity.ofDicom("description", null, "hostname", 123, null);
		forwardNodeEntity.addDestination(destinationEntity);

		String expectedMessage = "AETitle is mandatory";
		Exception exception = assertThrows(ConstraintViolationException.class, () -> {
			entityManager.persistAndFlush(forwardNodeEntity);
		});
		String actualMessage = exception.getMessage();
		assertTrue(actualMessage.contains(expectedMessage));
	}

	@Test
	void testInvalidDestinationStow_URL_mandatory() {
		ForwardNodeEntity forwardNodeEntity = ForwardNodeEntity.ofEmpty();
		forwardNodeEntity.setFwdDescription("description");
		forwardNodeEntity.setFwdAeTitle("fwdAeTitle");
		DestinationEntity destinationEntity = DestinationEntity.ofStow("description", null, "headers");
		forwardNodeEntity.addDestination(destinationEntity);

		String expectedMessage = "URL is mandatory";
		Exception exception = assertThrows(ConstraintViolationException.class, () -> {
			entityManager.persistAndFlush(forwardNodeEntity);
		});
		String actualMessage = exception.getMessage();
		assertTrue(actualMessage.contains(expectedMessage));
	}

	@Test
	void testForwardNode() {
		ForwardNodeEntity forwardNodeEntity = ForwardNodeEntity.ofEmpty();
		forwardNodeEntity.setFwdDescription("description");
		forwardNodeEntity.setFwdAeTitle("fwdAeTitle");
		entityManager.persistAndFlush(forwardNodeEntity);

		Iterable<ForwardNodeEntity> all = repository.findAll();
		assertThat(all) //
			.hasSize(1) //
			.first() //
			.satisfies(forwardNodeConsumer);
	}

	@Test
	void testWithSourceNode() {
		ForwardNodeEntity forwardNodeEntity = ForwardNodeEntity.ofEmpty();
		forwardNodeEntity.setFwdDescription("description");
		forwardNodeEntity.setFwdAeTitle("fwdAeTitle");
		DicomSourceNodeEntity sourceNode = DicomSourceNodeEntity.ofEmpty();
		sourceNode.setDescription("description");
		sourceNode.setAeTitle("aeTitle");
		sourceNode.setHostname("hostname");
		sourceNode.setCheckHostname(Boolean.TRUE);
		forwardNodeEntity.addSourceNode(sourceNode);
		entityManager.persistAndFlush(forwardNodeEntity);

		Iterable<ForwardNodeEntity> all = repository.findAll();
		assertThat(all) //
			.hasSize(1) //
			.first() //
			.satisfies(forwardNodeConsumer);

		assertThat(all) //
			.hasSize(1) //
			.flatExtracting(ForwardNodeEntity::getSourceNodes) //
			.hasSize(1) //
			.first() //
			.satisfies(sourceNodeConsumer);

		assertThat(all) //
			.hasSize(1) //
			.flatExtracting(ForwardNodeEntity::getDestinationEntities) //
			.isEmpty();
	}

	// @Test
	void testWithDestinationDicom() {
		ForwardNodeEntity forwardNodeEntity = ForwardNodeEntity.ofEmpty();
		forwardNodeEntity.setFwdDescription("description");
		forwardNodeEntity.setFwdAeTitle("fwdAeTitle");
		DestinationEntity destinationEntity = DestinationEntity.ofDicom("description", "aeTitle", "hostname", 123,
				null);
		forwardNodeEntity.addDestination(destinationEntity);
		entityManager.persistAndFlush(forwardNodeEntity);

		Iterable<ForwardNodeEntity> all = repository.findAll();
		assertThat(all) //
			.hasSize(1) //
			.first() //
			.satisfies(forwardNodeConsumer);

		assertThat(all) //
			.hasSize(1) //
			.flatExtracting(ForwardNodeEntity::getSourceNodes) //
			.isEmpty();

		assertThat(all) //
			.hasSize(1) //
			.flatExtracting(ForwardNodeEntity::getDestinationEntities) //
			.hasSize(1) //
			.first() //
			.satisfies(destinationDicomConsumer);
	}

	// @Test
	void testWithDestinationStow() {
		ForwardNodeEntity forwardNodeEntity = ForwardNodeEntity.ofEmpty();
		forwardNodeEntity.setFwdDescription("description");
		forwardNodeEntity.setFwdAeTitle("fwdAeTitle");
		DestinationEntity destinationEntity = DestinationEntity.ofStow("description", "url", "headers");
		forwardNodeEntity.addDestination(destinationEntity);
		entityManager.persistAndFlush(forwardNodeEntity);

		Iterable<ForwardNodeEntity> all = repository.findAll();
		assertThat(all) //
			.hasSize(1) //
			.first() //
			.satisfies(forwardNodeConsumer);

		assertThat(all) //
			.hasSize(1) //
			.flatExtracting(ForwardNodeEntity::getSourceNodes) //
			.isEmpty();

		assertThat(all) //
			.hasSize(1) //
			.flatExtracting(ForwardNodeEntity::getDestinationEntities) //
			.hasSize(1) //
			.first() //
			.satisfies(destinationStowConsumer);
	}

	// @Test
	void testWithSourceNodeAndDestinationDicom() {
		ForwardNodeEntity forwardNodeEntity = ForwardNodeEntity.ofEmpty();
		forwardNodeEntity.setFwdDescription("description");
		forwardNodeEntity.setFwdAeTitle("fwdAeTitle");
		DicomSourceNodeEntity sourceNode = DicomSourceNodeEntity.ofEmpty();
		sourceNode.setDescription("description");
		sourceNode.setAeTitle("aeTitle");
		sourceNode.setHostname("hostname");
		sourceNode.setCheckHostname(Boolean.TRUE);
		forwardNodeEntity.addSourceNode(sourceNode);
		DestinationEntity destinationEntity = DestinationEntity.ofDicom("description", "aeTitle", "hostname", 123,
				null);
		forwardNodeEntity.addDestination(destinationEntity);
		entityManager.persistAndFlush(forwardNodeEntity);

		Iterable<ForwardNodeEntity> all = repository.findAll();
		assertThat(all) //
			.hasSize(1) //
			.first() //
			.satisfies(forwardNodeConsumer);

		assertThat(all) //
			.hasSize(1) //
			.flatExtracting(ForwardNodeEntity::getSourceNodes) //
			.hasSize(1) //
			.first() //
			.satisfies(sourceNodeConsumer);

		assertThat(all) //
			.hasSize(1) //
			.flatExtracting(ForwardNodeEntity::getDestinationEntities) //
			.hasSize(1) //
			.first() //
			.satisfies(destinationDicomConsumer);
	}

	/**
	 * Test save and find record.
	 */
	@Test
	void shouldSaveAndFindARecord() {
		// Create an entity to save
		ForwardNodeEntity entity = new ForwardNodeEntity();
		entity.setFwdDescription("Description");

		// Save the entity
		log.info("Saving entity with Description [{}]", entity.getFwdDescription());
		entity = repository.save(entity);

		// Test Save
		assertEquals("Description", entity.getFwdDescription());
		assertNotNull(entity.getId());
		log.info("Entity with Description [{}] and id [{}] saved", entity.getFwdDescription(), entity.getId());

		// Find By Id
		Optional<ForwardNodeEntity> foundByIdOpt = repository.findById(entity.getId());

		// Test Find by Id
		assertTrue(foundByIdOpt.isPresent());
		log.info("Entity found with Description [{}] and id [{}]", foundByIdOpt.get().getFwdDescription(),
				foundByIdOpt.get().getId());
		assertEquals(entity.getId(), foundByIdOpt.get().getId());
	}

	/**
	 * Test find all.
	 */
	@Test
	void shouldFindAllRecords() {
		// Create an entity to save
		ForwardNodeEntity entity = new ForwardNodeEntity();
		entity.setFwdDescription("Description");
		entity.setFwdAeTitle("AeTitle");

		// Save the entity
		log.info("Saving entity with Description [{}]", entity.getFwdDescription());
		repository.saveAndFlush(entity);

		// Find all
		List<ForwardNodeEntity> all = repository.findAll();

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
		ForwardNodeEntity entity = new ForwardNodeEntity();
		entity.setFwdDescription(initialText);

		// Save the entity
		log.info("Saving entity with Description [{}]", entity.getFwdDescription());
		entity = repository.save(entity);
		log.info("Id of the entity with Description [{}]", entity.getId());

		// Test Save
		assertNotNull(entity);
		assertEquals(initialText, entity.getFwdDescription());

		// Modify the record
		entity.setFwdDescription(modifiedText);
		log.info("Modify entity Description [{}] to [{}]", initialText, modifiedText);
		ForwardNodeEntity entityModified = repository.save(entity);

		// Test Modify
		assertNotNull(entityModified);
		assertEquals(entity.getId(), entityModified.getId());
		assertEquals(modifiedText, entityModified.getFwdDescription());
		log.info("Description of the entity with id [{}]: [{}]", entityModified.getId(),
				entityModified.getFwdDescription());
	}

	/**
	 * Test delete record.
	 */
	@Test
	void shouldDeleteRecord() {
		// Create an entity to save
		ForwardNodeEntity entity = new ForwardNodeEntity();
		String description = "Description";
		entity.setFwdDescription(description);

		// Save the entity
		log.info("Saving entity with Description [{}]", entity.getFwdDescription());
		entity = repository.save(entity);

		// Retrieve the entity
		Optional<ForwardNodeEntity> foundByIdOpt = repository.findById(entity.getId());

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
