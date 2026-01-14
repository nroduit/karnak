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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.model.event.NodeEvent;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

class ForwardNodeAPIServiceTest {

	// Application Event Publisher
	private final ApplicationEventPublisher applicationEventPublisherMock = Mockito
		.mock(ApplicationEventPublisher.class);

	// Service
	private final ForwardNodeService forwardNodeServiceMock = Mockito.mock(ForwardNodeService.class);

	private final DestinationService destinationServiceMock = Mockito.mock(DestinationService.class);

	private ForwardNodeAPIService forwardNodeAPIService;

	@BeforeEach
	public void setUp() {

		// Build mocked service
		forwardNodeAPIService = new ForwardNodeAPIService(forwardNodeServiceMock, destinationServiceMock,
				applicationEventPublisherMock);
	}

	@Test
	void should_add_forward_node_node_type_add() {
		// Init data
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();

		// Call service
		forwardNodeAPIService.addForwardNode(forwardNodeEntity);

		// Test results
		Mockito.verify(forwardNodeServiceMock, Mockito.times(1)).getAllForwardNodes();
		Mockito.verify(forwardNodeServiceMock, Mockito.times(1)).save(Mockito.any(ForwardNodeEntity.class));
		Mockito.verify(applicationEventPublisherMock, Mockito.times(1)).publishEvent(Mockito.any(NodeEvent.class));
	}

	@Test
	void should_add_forward_node_node_type_update() {
		// Init data
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();
		forwardNodeEntity.setId(1L);

		// Call service
		forwardNodeAPIService.addForwardNode(forwardNodeEntity);

		// Test results
		Mockito.verify(forwardNodeServiceMock, Mockito.times(0)).getAllForwardNodes();
		Mockito.verify(forwardNodeServiceMock, Mockito.times(1)).save(Mockito.any(ForwardNodeEntity.class));
		Mockito.verify(applicationEventPublisherMock, Mockito.times(1)).publishEvent(Mockito.any(NodeEvent.class));
	}

	@Test
	void should_update_forward_node() {
		// Init data
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();
		forwardNodeEntity.setId(1L);

		// Call service
		forwardNodeAPIService.updateForwardNode(forwardNodeEntity);

		// Test results
		Mockito.verify(forwardNodeServiceMock, Mockito.times(1)).save(Mockito.any(ForwardNodeEntity.class));
		Mockito.verify(applicationEventPublisherMock, Mockito.times(1)).publishEvent(Mockito.any(NodeEvent.class));
	}

	@Test
	void should_delete_forward_node() {
		// Init data
		ForwardNodeEntity forwardNodeEntity = new ForwardNodeEntity();
		forwardNodeEntity.setId(1L);

		// Call service
		forwardNodeAPIService.deleteForwardNode(forwardNodeEntity);

		// Test results
		Mockito.verify(forwardNodeServiceMock, Mockito.times(1)).delete(Mockito.any(ForwardNodeEntity.class));
		Mockito.verify(applicationEventPublisherMock, Mockito.times(1)).publishEvent(Mockito.any(NodeEvent.class));
	}

	@Test
	void should_retrieve_forward_node_by_id() {
		// Call service
		forwardNodeAPIService.getForwardNodeById(1L);

		// Test results
		Mockito.verify(forwardNodeServiceMock, Mockito.times(1)).get(Mockito.anyLong());
	}

}
