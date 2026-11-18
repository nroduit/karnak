/*
 * Copyright (c) 2026 Karnak Team and other contributors.
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

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.model.profile.ProfileDeleteBlockerModel;
import org.karnak.backend.model.profile.ProfileDeleteBlockersModel;
import org.karnak.backend.service.profilepipe.ProfilePipeService;
import org.karnak.backend.service.profilepipe.ProfilePipeService.DeleteProfileResult;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ProfileControllerTest {

	private final ProfilePipeService profilePipeServiceMock = Mockito.mock(ProfilePipeService.class);

	private ProfileController profileController;

	private ProfileEntity profileEntity;

	private UUID profileUuid;

	@BeforeEach
	void setUp() {
		profileController = new ProfileController(profilePipeServiceMock);
		profileEntity = new ProfileEntity();
		profileEntity.setId(1L);
		profileUuid = profileEntity.getUuid();
		Mockito.when(profilePipeServiceMock.retrieveProfileByUuid(profileUuid)).thenReturn(profileEntity);
	}

	private ProfileDeleteBlockersModel blockers() {
		ProfileDeleteBlockerModel blocker = new ProfileDeleteBlockerModel();
		blocker.setProjectUuid(UUID.randomUUID());
		blocker.setProjectName("Study A");
		ProfileDeleteBlockersModel blockers = new ProfileDeleteBlockersModel();
		blockers.setProfileUuid(profileUuid);
		blockers.setBlockers(List.of(blocker));
		return blockers;
	}

	@Test
	void should_return_not_found_when_listing_blockers_of_unknown_profile() {
		ResponseEntity<ProfileDeleteBlockersModel> response = profileController.listDeleteBlockers(UUID.randomUUID());

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	void should_list_delete_blockers() {
		ProfileDeleteBlockersModel blockers = blockers();
		Mockito.when(profilePipeServiceMock.collectDeleteBlockers(profileEntity)).thenReturn(blockers);

		ResponseEntity<ProfileDeleteBlockersModel> response = profileController.listDeleteBlockers(profileUuid);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertSame(blockers, response.getBody());
	}

	@Test
	void should_return_not_found_when_deleting_unknown_profile() {
		ResponseEntity<?> response = profileController.deleteProfile(UUID.randomUUID());

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		Mockito.verify(profilePipeServiceMock, Mockito.never()).deleteProfile(Mockito.any());
	}

	@Test
	void should_delete_profile() {
		Mockito.when(profilePipeServiceMock.deleteProfile(profileEntity))
			.thenReturn(new DeleteProfileResult(true, null, null));

		ResponseEntity<?> response = profileController.deleteProfile(profileUuid);

		assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
	}

	@Test
	void should_return_conflict_with_blockers_when_delete_is_blocked() {
		ProfileDeleteBlockersModel blockers = blockers();
		Mockito.when(profilePipeServiceMock.deleteProfile(profileEntity))
			.thenReturn(new DeleteProfileResult(false, "Cannot delete profile", blockers));

		ResponseEntity<?> response = profileController.deleteProfile(profileUuid);

		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertSame(blockers, response.getBody());
	}

}
