/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.cache.ExternalIDCache;
import org.karnak.backend.config.RedisConfiguration;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.DicomSourceNodeEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.entity.KheopsAlbumsEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.data.repo.ForwardNodeRepo;
import org.karnak.backend.data.repo.VersionRepo;
import org.karnak.backend.dicom.DicomForwardDestination;
import org.karnak.backend.dicom.ForwardDestination;
import org.karnak.backend.enums.DestinationType;
import org.karnak.backend.enums.NodeEventType;
import org.karnak.backend.model.event.NodeEvent;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.mockito.Mock;
import org.weasis.dicom.param.DicomNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class GatewaySetUpServiceTest {

	@Mock
	private ExternalIDCache externalIDCache;

	@Mock
	private RedisConfiguration redisConfiguration;

	// Repositories
	final ForwardNodeRepo forwardNodeRepoMock = Mockito.mock(ForwardNodeRepo.class);

	final VersionRepo versionRepoMock = Mockito.mock(VersionRepo.class);

	final DestinationRepo destinationRepoMock = Mockito.mock(DestinationRepo.class);

	// Service
	private GatewaySetUpService gatewaySetUpService;

	@BeforeEach
	public void setUp() throws Exception {

		// Build mocked service
		gatewaySetUpService = new GatewaySetUpService(forwardNodeRepoMock, versionRepoMock, destinationRepoMock);
	}

	@Test
	void should_reload_gateway_stow() {
		// Init data
		List<ForwardNodeEntity> forwardNodeEntities = new ArrayList<>();
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();
		forwardNodeEntity.setFwdAeTitle("fwdAeTitle");
		forwardNodeEntity.setId(1L);
		Set<DicomSourceNodeEntity> dicomSourceNodeEntities = new HashSet<>();
		DicomSourceNodeEntity dicomSourceNodeEntity = new DicomSourceNodeEntity();
		dicomSourceNodeEntity.setId(2L);
		dicomSourceNodeEntity.setAeTitle("aeTitle");
		dicomSourceNodeEntity.setHostname("hostName");
		forwardNodeEntity.setSourceNodes(dicomSourceNodeEntities);
		Set<DestinationEntity> destinationEntities = new HashSet<>();
		DestinationEntity destinationEntity = new DestinationEntity();
		destinationEntity.setFilterBySOPClasses(true);
		List<KheopsAlbumsEntity> kheopsAlbumsEntities = new ArrayList<>();
		KheopsAlbumsEntity kheopsAlbumsEntity = new KheopsAlbumsEntity();
		kheopsAlbumsEntities.add(kheopsAlbumsEntity);
		destinationEntity.setKheopsAlbumEntities(kheopsAlbumsEntities);
		destinationEntity.setDesidentification(true);
		ProjectEntity projectEntity = new ProjectEntity();
		ProfileEntity profileEntity = new ProfileEntity();
		projectEntity.setProfileEntity(profileEntity);
		destinationEntity.setDeIdentificationProjectEntity(projectEntity);
		destinationEntity.setNotifyInterval(1);
		destinationEntity.setDestinationType(DestinationType.stow);
		destinationEntities.add(destinationEntity);
		forwardNodeEntity.setDestinationEntities(destinationEntities);
		forwardNodeEntities.add(forwardNodeEntity);

		// Mock
		Mockito.when(forwardNodeRepoMock.findAll()).thenReturn(forwardNodeEntities);

		// Call service
		gatewaySetUpService.reloadGatewayPersistence();

		// Test results
		assertNotNull(gatewaySetUpService.getDestinations());
		assertEquals(1, gatewaySetUpService.getDestinations().size());
		assertTrue(gatewaySetUpService.getDestinationNode("fwdAeTitle").isPresent());
	}

	@Test
	void should_reload_gateway_dicom() {
		// Init data
		List<ForwardNodeEntity> forwardNodeEntities = new ArrayList<>();
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();
		forwardNodeEntity.setFwdAeTitle("fwdAeTitle");
		forwardNodeEntity.setId(1L);
		Set<DicomSourceNodeEntity> dicomSourceNodeEntities = new HashSet<>();
		DicomSourceNodeEntity dicomSourceNodeEntity = new DicomSourceNodeEntity();
		dicomSourceNodeEntity.setId(2L);
		dicomSourceNodeEntity.setAeTitle("aeTitle");
		dicomSourceNodeEntity.setHostname("hostName");
		forwardNodeEntity.setSourceNodes(dicomSourceNodeEntities);
		Set<DestinationEntity> destinationEntities = new HashSet<>();
		DestinationEntity destinationEntity = new DestinationEntity();
		destinationEntity.setFilterBySOPClasses(true);
		destinationEntity.setAeTitle("aeTitle");
		List<KheopsAlbumsEntity> kheopsAlbumsEntities = new ArrayList<>();
		KheopsAlbumsEntity kheopsAlbumsEntity = new KheopsAlbumsEntity();
		kheopsAlbumsEntities.add(kheopsAlbumsEntity);
		destinationEntity.setKheopsAlbumEntities(kheopsAlbumsEntities);
		destinationEntity.setDesidentification(true);
		ProjectEntity projectEntity = new ProjectEntity();
		ProfileEntity profileEntity = new ProfileEntity();
		projectEntity.setProfileEntity(profileEntity);
		destinationEntity.setDeIdentificationProjectEntity(projectEntity);
		destinationEntity.setNotifyInterval(1);
		destinationEntity.setPort(11112);
		destinationEntity.setDestinationType(DestinationType.dicom);
		destinationEntities.add(destinationEntity);
		forwardNodeEntity.setDestinationEntities(destinationEntities);
		forwardNodeEntities.add(forwardNodeEntity);

		// Mock
		Mockito.when(forwardNodeRepoMock.findAll()).thenReturn(forwardNodeEntities);

		// Call service
		gatewaySetUpService.reloadGatewayPersistence();

		// Retrieve values
		List<List<ForwardDestination>> values = new ArrayList<>(gatewaySetUpService.getDestinations().values());

		// Test results
		assertNotNull(gatewaySetUpService.getDestinations());
		assertEquals(1, gatewaySetUpService.getDestinations().size());
		assertTrue(gatewaySetUpService.getDestinationNode("fwdAeTitle").isPresent());
		assertEquals("aeTitle", ((DicomForwardDestination) values.get(0).get(0)).getDestinationNode().getAet());
	}

	@Test
	void should_update_dicom_source_node() {
		// Init data
		DicomSourceNodeEntity dicomSourceNodeEntity = new DicomSourceNodeEntity();
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();
		forwardNodeEntity.setFwdAeTitle("fwdAeTitle");
		forwardNodeEntity.setId(2L);
		dicomSourceNodeEntity.setForwardNodeEntity(forwardNodeEntity);
		dicomSourceNodeEntity.setId(1L);
		dicomSourceNodeEntity.setAeTitle("aeTitle");
		dicomSourceNodeEntity.setHostname("hostName");

		NodeEvent nodeEvent = new NodeEvent(dicomSourceNodeEntity, NodeEventType.ADD);

		// Call service ADD
		gatewaySetUpService.update(nodeEvent);

		// Test results
		assertTrue(gatewaySetUpService.getDestinationNode("fwdAeTitle").isPresent());
		DicomNode dicomNode = gatewaySetUpService.getDestinationNode("fwdAeTitle")
			.get()
			.getAcceptedSourceNodes()
			.iterator()
			.next();
		assertEquals("aeTitle", dicomNode.getAet());

		// Modify aeTitle
		dicomSourceNodeEntity.setAeTitle("aeTitleModified");
		// Set Update
		nodeEvent = new NodeEvent(dicomSourceNodeEntity, NodeEventType.UPDATE);

		// Call service UPDATE
		gatewaySetUpService.update(nodeEvent);

		// Test results
		assertTrue(gatewaySetUpService.getDestinationNode("fwdAeTitle").isPresent());
		dicomNode = gatewaySetUpService.getDestinationNode("fwdAeTitle")
			.get()
			.getAcceptedSourceNodes()
			.iterator()
			.next();
		assertEquals("aeTitleModified", dicomNode.getAet());

		// Set Remove
		nodeEvent = new NodeEvent(dicomSourceNodeEntity, NodeEventType.REMOVE);

		// Call service REMOVE
		gatewaySetUpService.update(nodeEvent);

		// Test results
		assertTrue(gatewaySetUpService.getDestinationNode("fwdAeTitle").isPresent());
		assertEquals(0, gatewaySetUpService.getDestinationNode("fwdAeTitle").get().getAcceptedSourceNodes().size());
	}

	@Test
	void should_update_destination() {
		// Init data
		DestinationEntity destinationEntity = new DestinationEntity();
		destinationEntity.setFilterBySOPClasses(true);
		destinationEntity.setAeTitle("aeTitle");
		destinationEntity.setNotifyInterval(1);
		destinationEntity.setPort(11112);
		destinationEntity.setId(1L);
		destinationEntity.setDestinationType(DestinationType.dicom);
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();
		forwardNodeEntity.setFwdAeTitle("fwdAeTitle");
		forwardNodeEntity.setId(2L);
		destinationEntity.setForwardNodeEntity(forwardNodeEntity);

		NodeEvent nodeEvent = new NodeEvent(destinationEntity, NodeEventType.ADD);

		// Call service ADD
		gatewaySetUpService.update(nodeEvent);

		// Retrieve values
		List<List<ForwardDestination>> values = new ArrayList<>(gatewaySetUpService.getDestinations().values());

		// Test results
		assertTrue(gatewaySetUpService.getDestinationNode("fwdAeTitle").isPresent());
		assertEquals("aeTitle", ((DicomForwardDestination) values.get(0).get(0)).getDestinationNode().getAet());

		// Modify aeTitle
		destinationEntity.setAeTitle("aeTitleModified");
		// Set Update
		nodeEvent = new NodeEvent(destinationEntity, NodeEventType.UPDATE);

		// Call service UPDATE
		gatewaySetUpService.update(nodeEvent);

		// Test results
		assertTrue(gatewaySetUpService.getDestinationNode("fwdAeTitle").isPresent());
		assertEquals("aeTitleModified", ((DicomForwardDestination) values.get(0).get(0)).getDestinationNode().getAet());

		// Set Remove
		nodeEvent = new NodeEvent(destinationEntity, NodeEventType.REMOVE);

		// Call service REMOVE
		gatewaySetUpService.update(nodeEvent);

		// Test results
		assertTrue(gatewaySetUpService.getDestinationNode("fwdAeTitle").isPresent());
		assertEquals(0, values.get(0).size());
	}

	@Test
	void should_update_forward_node() {
		// Init data
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();
		forwardNodeEntity.setFwdAeTitle("fwdAeTitle");
		forwardNodeEntity.setId(1L);
		forwardNodeEntity.setFwdDescription("description");
		DestinationEntity destinationEntity = new DestinationEntity();
		destinationEntity.setFilterBySOPClasses(true);
		destinationEntity.setAeTitle("aeTitle");
		destinationEntity.setNotifyInterval(1);
		destinationEntity.setPort(11112);
		destinationEntity.setId(1L);
		destinationEntity.setDestinationType(DestinationType.dicom);
		forwardNodeEntity.addDestination(destinationEntity);

		NodeEvent nodeEvent = new NodeEvent(forwardNodeEntity, NodeEventType.ADD);

		// Call service ADD
		gatewaySetUpService.update(nodeEvent);

		// Retrieve values
		List<List<ForwardDestination>> values = new ArrayList<>(gatewaySetUpService.getDestinations().values());

		// Test results
		assertTrue(gatewaySetUpService.getDestinationNode("fwdAeTitle").isPresent());
		assertEquals("aeTitle", ((DicomForwardDestination) values.get(0).get(0)).getDestinationNode().getAet());

		// Modify forward nod
		forwardNodeEntity.setFwdAeTitle("aeTitleModified");

		// Set Update
		nodeEvent = new NodeEvent(forwardNodeEntity, NodeEventType.UPDATE);

		// Call service UPDATE
		gatewaySetUpService.update(nodeEvent);

		// Test results
		assertTrue(gatewaySetUpService.getDestinationNode("aeTitleModified").isPresent());
		assertEquals("aeTitle", ((DicomForwardDestination) values.get(0).get(0)).getDestinationNode().getAet());

		// Set Remove
		nodeEvent = new NodeEvent(forwardNodeEntity, NodeEventType.REMOVE);

		// Call service REMOVE
		gatewaySetUpService.update(nodeEvent);

		// Test results
		assertFalse(gatewaySetUpService.getDestinationNode("aeTitleModified").isPresent());
	}

}
