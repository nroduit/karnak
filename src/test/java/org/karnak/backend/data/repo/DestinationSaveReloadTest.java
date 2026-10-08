/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.repo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

/**
 * Reproduces the exact runtime destination save path used by the UI: the parent forward
 * node is detached (OSIV is disabled in the app), a destination is attached to it and the
 * *child* is saved via its own repository, then the forward node is reloaded by id (as
 * the view does).
 */
@DataJpaTest
class DestinationSaveReloadTest {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private ForwardNodeRepo forwardNodeRepo;

	@Autowired
	private DestinationRepo destinationRepo;

	@Test
	void destinationSavedViaChildRepo_withDetachedParent_appearsOnReload() {
		// Persist a forward node
		ForwardNodeEntity fwd = ForwardNodeEntity.ofEmpty();
		fwd.setFwdAeTitle("fwdAeTitle");
		fwd.setFwdDescription("description");
		fwd = forwardNodeRepo.saveAndFlush(fwd);
		Long fwdId = fwd.getId();

		// Detach everything (simulate the closed session / OSIV=false)
		entityManager.flush();
		entityManager.clear();

		// Reload the forward node as the view does (retrieveForwardNodeById), then detach
		ForwardNodeEntity detached = forwardNodeRepo.findById(fwdId).orElseThrow();
		entityManager.clear();

		// Real save path: attach destination to the DETACHED parent and save the CHILD
		DestinationEntity dest = DestinationEntity.ofDicom("description", "aeTitle", "hostname", 123, null);
		detached.addDestination(dest);
		destinationRepo.saveAndFlush(dest);

		entityManager.flush();
		entityManager.clear();

		// Reload and assert the destination is associated to the forward node
		ForwardNodeEntity reloaded = forwardNodeRepo.findById(fwdId).orElseThrow();
		assertThat(reloaded.getDestinationEntities()).as("destination must be linked to the forward node after save")
			.hasSize(1);
	}

}
