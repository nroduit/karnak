/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import java.util.Collection;
import org.karnak.backend.data.entity.DicomSourceNodeEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.repo.DicomSourceNodeRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class SourceNodeService {

	// Repositories
	private final DicomSourceNodeRepo dicomSourceNodeRepo;

	// Services
	private final ForwardNodeService forwardNodeService;

	// Event publisher
	private final ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	public SourceNodeService(final DicomSourceNodeRepo dicomSourceNodeRepo, final ForwardNodeService forwardNodeService,
			final ApplicationEventPublisher applicationEventPublisher) {
		this.dicomSourceNodeRepo = dicomSourceNodeRepo;
		this.forwardNodeService = forwardNodeService;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public Collection<DicomSourceNodeEntity> retrieveSourceNodes(ForwardNodeEntity forwardNodeEntity) {
		return forwardNodeService.getAllSourceNodes(forwardNodeEntity);
	}

	/**
	 * Store given DicomSourceNodeEntity.
	 * @param forwardNodeEntity
	 * @param dicomSourceNodeEntity the updated or new dicomSourceNodeEntity
	 */
	public DicomSourceNodeEntity save(ForwardNodeEntity forwardNodeEntity,
			DicomSourceNodeEntity dicomSourceNodeEntity) {
		return this.forwardNodeService.updateSourceNode(forwardNodeEntity, dicomSourceNodeEntity);
	}

	/**
	 * Delete given data from the backing data service.
	 * @param dicomSourceNodeEntity the data to be deleted
	 */
	public void delete(DicomSourceNodeEntity dicomSourceNodeEntity) {
		ForwardNodeEntity forwardNodeEntityOfDest = dicomSourceNodeEntity.getForwardNodeEntity();
		if (forwardNodeEntityOfDest != null) {
			forwardNodeService.deleteSourceNode(forwardNodeEntityOfDest, dicomSourceNodeEntity);
		}
	}

	public ApplicationEventPublisher getApplicationEventPublisher() {
		return applicationEventPublisher;
	}

}
