/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
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

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.DicomSourceNodeEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.enums.NodeEventType;
import org.karnak.backend.model.event.NodeEvent;
import org.karnak.backend.service.DestinationService;
import org.karnak.backend.service.ForwardNodeAPIService;
import org.karnak.backend.service.ForwardNodeService;
import org.karnak.backend.service.ProjectService;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * A destination or source node deleted over REST has to leave the gateway's routing table
 * too. Without the event it stayed there in the instance that served the call: a deleted
 * destination kept being forwarded to as well as the one that replaced it, and a deleted
 * source node kept being accepted.
 */
class ForwardNodeControllerTest {

	private ForwardNodeAPIService forwardNodeAPIService;

	private ForwardNodeService forwardNodeService;

	private DestinationService destinationService;

	private ApplicationEventPublisher publisher;

	private ForwardNodeController controller;

	@BeforeEach
	void setUp() {
		forwardNodeAPIService = Mockito.mock(ForwardNodeAPIService.class);
		forwardNodeService = Mockito.mock(ForwardNodeService.class);
		destinationService = Mockito.mock(DestinationService.class);
		publisher = Mockito.mock(ApplicationEventPublisher.class);
		controller = new ForwardNodeController(forwardNodeAPIService, forwardNodeService, destinationService,
				Mockito.mock(ProjectService.class), publisher);
	}

	@Test
	void should_publish_a_remove_event_for_the_destination_after_deleting_it() {
		UUID nodeUuid = UUID.randomUUID();
		UUID destinationUuid = UUID.randomUUID();
		ForwardNodeEntity node = new ForwardNodeEntity();
		DestinationEntity destination = new DestinationEntity();
		destination.setForwardNodeEntity(node);
		Mockito.when(forwardNodeAPIService.retrieveForwardNodeByUuid(nodeUuid)).thenReturn(node);
		Mockito.when(forwardNodeService.retrieveDestinationByUuid(node, destinationUuid)).thenReturn(destination);

		ResponseEntity<Void> response = controller.deleteDestination(nodeUuid, destinationUuid);

		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
		InOrder order = Mockito.inOrder(destinationService, publisher);
		order.verify(destinationService).delete(destination);
		ArgumentCaptor<NodeEvent> event = ArgumentCaptor.forClass(NodeEvent.class);
		order.verify(publisher).publishEvent(event.capture());
		assertEquals(NodeEventType.REMOVE, event.getValue().getEventType());
		assertSame(destination, event.getValue().getSource());
		assertSame(node, event.getValue().getForwardNode());
	}

	@Test
	void should_publish_nothing_when_the_destination_does_not_exist() {
		UUID nodeUuid = UUID.randomUUID();
		UUID destinationUuid = UUID.randomUUID();
		ForwardNodeEntity node = new ForwardNodeEntity();
		Mockito.when(forwardNodeAPIService.retrieveForwardNodeByUuid(nodeUuid)).thenReturn(node);
		Mockito.when(forwardNodeService.retrieveDestinationByUuid(node, destinationUuid)).thenReturn(null);

		ResponseEntity<Void> response = controller.deleteDestination(nodeUuid, destinationUuid);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		Mockito.verify(destinationService, Mockito.never()).delete(Mockito.any());
		Mockito.verifyNoInteractions(publisher);
	}

	@Test
	void should_publish_nothing_when_the_forward_node_does_not_exist() {
		UUID nodeUuid = UUID.randomUUID();
		Mockito.when(forwardNodeAPIService.retrieveForwardNodeByUuid(nodeUuid)).thenReturn(null);

		ResponseEntity<Void> response = controller.deleteDestination(nodeUuid, UUID.randomUUID());

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		Mockito.verifyNoInteractions(publisher);
	}

	@Test
	void should_publish_a_remove_event_for_the_source_node_after_deleting_it() {
		UUID nodeUuid = UUID.randomUUID();
		UUID sourceNodeUuid = UUID.randomUUID();
		ForwardNodeEntity node = new ForwardNodeEntity();
		DicomSourceNodeEntity sourceNode = new DicomSourceNodeEntity();
		sourceNode.setForwardNodeEntity(node);
		Mockito.when(forwardNodeAPIService.retrieveForwardNodeByUuid(nodeUuid)).thenReturn(node);
		Mockito.when(forwardNodeService.retrieveSourceNodeByUuid(node, sourceNodeUuid)).thenReturn(sourceNode);

		ResponseEntity<Void> response = controller.deleteSourceNode(nodeUuid, sourceNodeUuid);

		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
		InOrder order = Mockito.inOrder(forwardNodeService, publisher);
		order.verify(forwardNodeService).deleteSourceNode(node, sourceNode);
		ArgumentCaptor<NodeEvent> event = ArgumentCaptor.forClass(NodeEvent.class);
		order.verify(publisher).publishEvent(event.capture());
		assertEquals(NodeEventType.REMOVE, event.getValue().getEventType());
		assertSame(sourceNode, event.getValue().getSource());
		assertSame(node, event.getValue().getForwardNode());
	}

	@Test
	void should_publish_nothing_when_the_source_node_does_not_exist() {
		UUID nodeUuid = UUID.randomUUID();
		UUID sourceNodeUuid = UUID.randomUUID();
		ForwardNodeEntity node = new ForwardNodeEntity();
		Mockito.when(forwardNodeAPIService.retrieveForwardNodeByUuid(nodeUuid)).thenReturn(node);
		Mockito.when(forwardNodeService.retrieveSourceNodeByUuid(node, sourceNodeUuid)).thenReturn(null);

		ResponseEntity<Void> response = controller.deleteSourceNode(nodeUuid, sourceNodeUuid);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		Mockito.verify(forwardNodeService, Mockito.never()).deleteSourceNode(Mockito.any(), Mockito.any());
		Mockito.verifyNoInteractions(publisher);
	}

}
