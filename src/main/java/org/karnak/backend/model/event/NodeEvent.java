/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.event;

import java.util.Objects;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.DicomSourceNodeEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.enums.NodeEventType;
import org.springframework.context.ApplicationEvent;

public class NodeEvent extends ApplicationEvent {

	private static final long serialVersionUID = -15504960651765311L;

	private final NodeEventType eventType;

	private final ForwardNodeEntity forwardNodeEntity;

	public NodeEvent(ForwardNodeEntity fwdNode, NodeEventType eventType) {
		super(fwdNode);
		this.forwardNodeEntity = fwdNode;
		this.eventType = eventType;
	}

	public NodeEvent(DicomSourceNodeEntity srcNode, NodeEventType eventType) {
		super(srcNode);
		this.forwardNodeEntity = Objects.requireNonNull(srcNode.getForwardNodeEntity());
		this.eventType = eventType;
	}

	public NodeEvent(DestinationEntity dstNode, NodeEventType eventType) {
		super(dstNode);
		this.forwardNodeEntity = Objects.requireNonNull(dstNode.getForwardNodeEntity());
		this.eventType = eventType;
	}

	public ForwardNodeEntity getForwardNode() {
		return forwardNodeEntity;
	}

	public NodeEventType getEventType() {
		return eventType;
	}

}
