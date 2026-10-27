/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.DicomSourceNodeEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.entity.KheopsAlbumsEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.entity.VersionEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.data.repo.ForwardNodeRepo;
import org.karnak.backend.data.repo.VersionRepo;
import org.karnak.backend.dicom.DicomForwardDestination;
import org.karnak.backend.dicom.ForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.karnak.backend.enums.DestinationType;
import org.karnak.backend.enums.NodeEventType;
import org.karnak.backend.model.event.NodeEvent;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.weasis.dicom.param.DicomNode;

@SpringBootTest
class GatewaySetUpServiceTest {

	// Repositories
	final ForwardNodeRepo forwardNodeRepoMock = Mockito.mock(ForwardNodeRepo.class);

	final VersionRepo versionRepoMock = Mockito.mock(VersionRepo.class);

	final DestinationRepo destinationRepoMock = Mockito.mock(DestinationRepo.class);

	// Service
	private GatewaySetUpService gatewaySetUpService;

	@BeforeEach
	public void setUp() throws Exception {

		// Build mocked service
		gatewaySetUpService = new GatewaySetUpService(forwardNodeRepoMock, versionRepoMock, destinationRepoMock, null);
	}

	@Test
	void should_parse_every_header_pair() {
		Map<String, String> headers = GatewaySetUpService.parseHeaders(
				"<key>Authorization</key>\n<value>Bearer token</value>\n<key>X-Tenant</key>\n<value>test</value>");

		assertEquals(2, headers.size());
		assertEquals("Bearer token", headers.get("Authorization"));
		assertEquals("test", headers.get("X-Tenant"));
		assertTrue(GatewaySetUpService.parseHeaders(null).isEmpty());
		assertTrue(GatewaySetUpService.parseHeaders("  ").isEmpty());
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
	void should_only_validate_hostname_when_requested() {
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();
		forwardNodeEntity.setId(1L);
		forwardNodeEntity.setFwdAeTitle("fwdAeTitle");
		DicomSourceNodeEntity withoutCheck = new DicomSourceNodeEntity();
		withoutCheck.setId(1L);
		withoutCheck.setAeTitle("SRC1");
		withoutCheck.setHostname("");
		withoutCheck.setCheckHostname(false);
		DicomSourceNodeEntity withCheck = new DicomSourceNodeEntity();
		withCheck.setId(2L);
		withCheck.setAeTitle("SRC2");
		withCheck.setHostname("host2");
		withCheck.setCheckHostname(true);
		Set<DicomSourceNodeEntity> sourceNodes = new HashSet<>();
		sourceNodes.add(withoutCheck);
		sourceNodes.add(withCheck);
		forwardNodeEntity.setSourceNodes(sourceNodes);
		Mockito.when(forwardNodeRepoMock.findAll()).thenReturn(List.of(forwardNodeEntity));

		gatewaySetUpService.reloadGatewayPersistence();

		Set<DicomNode> accepted = gatewaySetUpService.getDestinationNode("fwdAeTitle").get().getAcceptedSourceNodes();
		assertEquals(2, accepted.size());
		assertFalse(accepted.stream().filter(n -> "SRC1".equals(n.getAet())).findFirst().get().isValidateHostname());
		assertTrue(accepted.stream().filter(n -> "SRC2".equals(n.getAet())).findFirst().get().isValidateHostname());
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

	@Test
	void should_retire_the_destinations_replaced_or_removed_by_an_update() {
		DestinationEntity destinationEntity = dicomDestination(1L, "aeTitle");
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();
		forwardNodeEntity.setFwdAeTitle("fwdAeTitle");
		forwardNodeEntity.setId(2L);
		destinationEntity.setForwardNodeEntity(forwardNodeEntity);
		gatewaySetUpService.update(new NodeEvent(destinationEntity, NodeEventType.ADD));
		List<ForwardDestination> live = gatewaySetUpService.getDestinations().values().iterator().next();
		ForwardDestination original = live.get(0);

		destinationEntity.setAeTitle("aeTitleModified");
		gatewaySetUpService.update(new NodeEvent(destinationEntity, NodeEventType.UPDATE));

		// Replaced in place, and the old one taken out of service
		assertEquals(1, live.size());
		ForwardDestination replacement = live.get(0);
		assertNotSame(original, replacement);
		assertFalse(original.beginTransfer());

		gatewaySetUpService.update(new NodeEvent(destinationEntity, NodeEventType.REMOVE));

		assertTrue(live.isEmpty());
		assertFalse(replacement.beginTransfer());
	}

	@Test
	void should_rebuild_the_destinations_with_the_new_aet_of_a_renamed_forward_node() {
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();
		forwardNodeEntity.setFwdAeTitle("fwdAeTitle");
		forwardNodeEntity.setId(1L);
		forwardNodeEntity.addDestination(dicomDestination(1L, "aeTitle"));
		gatewaySetUpService.update(new NodeEvent(forwardNodeEntity, NodeEventType.ADD));
		ForwardDestination original = gatewaySetUpService.getDestinations().values().iterator().next().get(0);

		forwardNodeEntity.setFwdAeTitle("aeTitleModified");
		gatewaySetUpService.update(new NodeEvent(forwardNodeEntity, NodeEventType.UPDATE));

		ForwardDicomNode renamed = gatewaySetUpService.getDestinationNode("aeTitleModified").orElseThrow();
		ForwardDestination rebuilt = gatewaySetUpService.getDestinations(renamed).get(0);
		assertEquals("aeTitleModified", rebuilt.getForwardDicomNode().getForwardAETitle());
		assertFalse(original.beginTransfer());
	}

	@Test
	void should_retire_the_destinations_replaced_by_a_reload() {
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();
		forwardNodeEntity.setFwdAeTitle("fwdAeTitle");
		forwardNodeEntity.setId(1L);
		forwardNodeEntity.addDestination(dicomDestination(1L, "aeTitle"));
		Mockito.when(forwardNodeRepoMock.findAll()).thenReturn(List.of(forwardNodeEntity));
		gatewaySetUpService.reloadGatewayPersistence();
		ForwardDestination original = gatewaySetUpService.getDestinations().values().iterator().next().get(0);

		gatewaySetUpService.reloadGatewayPersistence();

		ForwardDestination reloaded = gatewaySetUpService.getDestinations().values().iterator().next().get(0);
		assertNotSame(original, reloaded);
		assertFalse(original.beginTransfer());
		assertTrue(reloaded.beginTransfer());

		// A forward node dropped from the configuration retires its destinations too
		Mockito.when(forwardNodeRepoMock.findAll()).thenReturn(List.of());
		gatewaySetUpService.reloadGatewayPersistence();

		assertTrue(gatewaySetUpService.getDestinations().isEmpty());
		reloaded.endTransfer();
		assertFalse(reloaded.beginTransfer());
	}

	@Test
	void should_reload_a_change_of_another_instance_after_a_local_change() {
		VersionEntity version = new VersionEntity();
		Mockito.when(versionRepoMock.findTopByOrderByIdDesc()).thenReturn(version);
		Mockito.when(forwardNodeRepoMock.findAll()).thenReturn(List.of());
		// Forget the reload done by the constructor
		Mockito.clearInvocations(forwardNodeRepoMock);

		// Local change while up to date: nothing to reload
		gatewaySetUpService.refreshVersionGatewaySetUp();
		gatewaySetUpService.checkRefreshGatewaySetUp();
		Mockito.verify(forwardNodeRepoMock, Mockito.never()).findAll();

		// Another instance changes the setup, then a local change comes before the
		// scheduled check: the other change must still be reloaded
		version.setGatewaySetup(version.getGatewaySetup() + 1);
		gatewaySetUpService.refreshVersionGatewaySetUp();
		gatewaySetUpService.checkRefreshGatewaySetUp();
		Mockito.verify(forwardNodeRepoMock, Mockito.times(1)).findAll();
	}

	private static DestinationEntity dicomDestination(Long id, String aeTitle) {
		DestinationEntity destinationEntity = new DestinationEntity();
		destinationEntity.setFilterBySOPClasses(true);
		destinationEntity.setAeTitle(aeTitle);
		destinationEntity.setNotifyInterval(1);
		destinationEntity.setPort(11112);
		destinationEntity.setId(id);
		destinationEntity.setDestinationType(DestinationType.dicom);
		return destinationEntity;
	}

}
