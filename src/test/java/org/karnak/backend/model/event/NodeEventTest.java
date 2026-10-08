/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.DicomSourceNodeEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.enums.NodeEventType;

/**
 * {@link NodeEvent} carries the originating {@link ForwardNodeEntity} and an event type.
 * It can be built from a forward node directly, or from a source / destination node that
 * points back to its forward node.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class NodeEventTest {

	@Test
	void built_from_a_forward_node_exposes_it_as_source_and_forward_node() {
		ForwardNodeEntity fwd = new ForwardNodeEntity();

		NodeEvent event = new NodeEvent(fwd, NodeEventType.ADD);

		assertSame(fwd, event.getForwardNode());
		assertSame(fwd, event.getSource());
		assertEquals(NodeEventType.ADD, event.getEventType());
	}

	@Test
	void built_from_a_source_node_resolves_its_forward_node() {
		ForwardNodeEntity fwd = new ForwardNodeEntity();
		DicomSourceNodeEntity src = new DicomSourceNodeEntity();
		src.setForwardNodeEntity(fwd);

		NodeEvent event = new NodeEvent(src, NodeEventType.UPDATE);

		assertSame(fwd, event.getForwardNode());
		assertSame(src, event.getSource());
		assertEquals(NodeEventType.UPDATE, event.getEventType());
	}

	@Test
	void built_from_a_destination_node_resolves_its_forward_node() {
		ForwardNodeEntity fwd = new ForwardNodeEntity();
		DestinationEntity dst = new DestinationEntity();
		dst.setForwardNodeEntity(fwd);

		NodeEvent event = new NodeEvent(dst, NodeEventType.REMOVE);

		assertSame(fwd, event.getForwardNode());
		assertSame(dst, event.getSource());
		assertEquals(NodeEventType.REMOVE, event.getEventType());
	}

	@Test
	void source_node_without_a_forward_node_is_rejected() {
		DicomSourceNodeEntity src = new DicomSourceNodeEntity();

		assertThrows(NullPointerException.class, () -> new NodeEvent(src, NodeEventType.ADD));
	}

	@Test
	void destination_node_without_a_forward_node_is_rejected() {
		DestinationEntity dst = new DestinationEntity();

		assertThrows(NullPointerException.class, () -> new NodeEvent(dst, NodeEventType.ADD));
	}

}