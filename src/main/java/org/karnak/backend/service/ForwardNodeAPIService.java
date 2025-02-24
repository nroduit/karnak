/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import java.io.Serializable;
import java.util.Optional;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.enums.NodeEventType;
import org.karnak.backend.model.event.NodeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Forward Node API Service
 */
@Service
public class ForwardNodeAPIService implements Serializable {

	// Services
	private final ForwardNodeService forwardNodeService;

	private final DestinationService destinationService;

	// Event publisher
	private final ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	public ForwardNodeAPIService(final ForwardNodeService forwardNodeService,
			final DestinationService destinationService, final ApplicationEventPublisher applicationEventPublisher) {
		this.forwardNodeService = forwardNodeService;
		this.destinationService = destinationService;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public ForwardNodeService getDataProvider() {
		return forwardNodeService;
	}

	public void addForwardNode(ForwardNodeEntity forwardNodeEntity) {
		NodeEventType eventType = forwardNodeEntity.getId() == null ? NodeEventType.ADD : NodeEventType.UPDATE;
		if (eventType == NodeEventType.ADD) {
			Optional<ForwardNodeEntity> val = forwardNodeService.getAllForwardNodes()
				.stream()
				.filter(f -> f.getFwdAeTitle().equals(forwardNodeEntity.getFwdAeTitle()))
				.findFirst();
			if (val.isPresent()) {
				// showError("Cannot add this new node because the AE-Title already
				// exists!");
				return;
			}
		}
		forwardNodeService.save(forwardNodeEntity);
		applicationEventPublisher.publishEvent(new NodeEvent(forwardNodeEntity, eventType));
	}

	public void updateForwardNode(ForwardNodeEntity forwardNodeEntity) {
		// Refresh last transfer and email last check
		forwardNodeEntity.getDestinationEntities().forEach(destinationService::refreshLastTransferEmailLastCheck);
		// Save forward entity
		forwardNodeService.save(forwardNodeEntity);
		applicationEventPublisher.publishEvent(new NodeEvent(forwardNodeEntity, NodeEventType.UPDATE));
	}

	public void deleteForwardNode(ForwardNodeEntity forwardNodeEntity) {
		forwardNodeService.delete(forwardNodeEntity);
		applicationEventPublisher.publishEvent(new NodeEvent(forwardNodeEntity, NodeEventType.REMOVE));
	}

	public ForwardNodeEntity getForwardNodeById(Long dataId) {
		return forwardNodeService.get(dataId);
	}

}
