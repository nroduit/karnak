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
import java.util.List;
import java.util.Optional;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Service managing destinations
 */
@Service
public class DestinationService {

	// Repositories
	private final DestinationRepo destinationRepo;

	// Services
	private final ForwardNodeService forwardNodeService;

	private final KheopsAlbumsService kheopsAlbumsService;

	// Event publisher
	private final ApplicationEventPublisher applicationEventPublisher;

	/**
	 * Autowired constructor
	 * @param destinationRepo Destination repository
	 * @param forwardNodeService ForwardNode Service
	 * @param kheopsAlbumsService Kheops Albums Service
	 * @param applicationEventPublisher ApplicationEventPublisher
	 */
	@Autowired
	public DestinationService(final DestinationRepo destinationRepo, final ForwardNodeService forwardNodeService,
			final KheopsAlbumsService kheopsAlbumsService, final ApplicationEventPublisher applicationEventPublisher) {
		this.destinationRepo = destinationRepo;
		this.forwardNodeService = forwardNodeService;
		this.kheopsAlbumsService = kheopsAlbumsService;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	/**
	 * Store given Destination to the backing destinationEntity service.
	 * @param forwardNodeEntity ForwardNode Entity
	 * @param destinationEntity the updated or new destinationEntity
	 */
	public DestinationEntity save(ForwardNodeEntity forwardNodeEntity, DestinationEntity destinationEntity) {
		DestinationEntity dataUpdated = forwardNodeService.updateDestination(forwardNodeEntity, destinationEntity);

		if (destinationEntity.getId() != null) {
			dataUpdated = removeValuesOnDisabledDeIdentificationTagMorphing(destinationEntity);
		}

		// Refresh last transfer and email last check before saving
		refreshLastTransferEmailLastCheck(dataUpdated);

		destinationRepo.saveAndFlush(dataUpdated);
		kheopsAlbumsService.updateSwitchingAlbumsFromDestination(destinationEntity);
		return dataUpdated;
	}

	/**
	 * Refresh values from DB for email last check and last transfer
	 * @param destinationEntity Entity to update
	 */
	public void refreshLastTransferEmailLastCheck(DestinationEntity destinationEntity) {
		if (destinationEntity.getId() != null) {
			Optional<DestinationEntity> refreshedDestinationEntityOpt = destinationRepo
				.findById(destinationEntity.getId());
			if (refreshedDestinationEntityOpt.isPresent()) {
				DestinationEntity destinationEntityRefreshed = refreshedDestinationEntityOpt.get();
				destinationEntity.setLastTransfer(destinationEntityRefreshed.getLastTransfer());
				destinationEntity.setEmailLastCheck(destinationEntityRefreshed.getEmailLastCheck());
			}
		}
	}

	private DestinationEntity removeValuesOnDisabledDeIdentificationTagMorphing(DestinationEntity destinationEntity) {
		if (!destinationEntity.isDesidentification()) {
			destinationEntity.setDeIdentificationProjectEntity(null);
		}
		if (!destinationEntity.isActivateTagMorphing()) {
			destinationEntity.setTagMorphingProjectEntity(null);
		}
		return destinationEntity;
	}

	/**
	 * Delete given data from the backing data service.
	 * @param destinationEntity the data to be deleted
	 */
	public void delete(DestinationEntity destinationEntity) {
		ForwardNodeEntity forwardNodeEntityOfDest = destinationEntity.getForwardNodeEntity();
		if (forwardNodeEntityOfDest != null) {
			forwardNodeService.deleteDestination(forwardNodeEntityOfDest, destinationEntity);
		}
	}

	public ApplicationEventPublisher getApplicationEventPublisher() {
		return applicationEventPublisher;
	}

	/**
	 * Retrieve destinations of a forward node
	 * @param forwardNodeEntity forward node
	 * @return destinations found
	 */
	public Collection<DestinationEntity> retrieveDestinations(ForwardNodeEntity forwardNodeEntity) {
		return forwardNodeService.getAllDestinations(forwardNodeEntity);
	}

	public List<DestinationEntity> retrieveDestinationsFromIds(List<Long> ids) {
		return destinationRepo.findAllById(ids);
	}

}
