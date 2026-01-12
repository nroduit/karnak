/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.profilepipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.data.repo.ProfileRepo;
import org.karnak.backend.model.profilebody.MaskBody;
import org.karnak.backend.model.profilebody.ProfileElementBody;
import org.karnak.backend.model.profilebody.ProfilePipeBody;
import org.karnak.frontend.profile.component.errorprofile.ProfileError;
import org.mockito.Mockito;

class ProfilePipeServiceTest {

	// Repositories
	private final ProfileRepo profileRepositoryMock = Mockito.mock(ProfileRepo.class);

	// Service
	private ProfilePipeService profilePipeService;

	@BeforeEach
	public void setUp() {
		// Build mocked service
		profilePipeService = new ProfilePipeService(profileRepositoryMock);
	}

	@Test
	void should_retrieve_all_profiles() {

		// Call service
		List<ProfileEntity> allProfiles = profilePipeService.getAllProfiles();

		// Test results
		Mockito.verify(profileRepositoryMock, Mockito.times(1)).findAll();
	}

	// TODO: to reactivate
	// @Test
	void should_validate_profile() {
		// Init data
		ProfilePipeBody profilePipeBody = new ProfilePipeBody();
		profilePipeBody.setName("name");
		profilePipeBody.setVersion("version");
		profilePipeBody.setMinimumKarnakVersion("version");
		profilePipeBody.setDefaultIssuerOfPatientID("defaultIssuerOfPatientID");
		List<MaskBody> maskBodies = new ArrayList<>();
		MaskBody maskBody = new MaskBody();
		maskBody.setColor("white");
		maskBody.setStationName("stationName");
		maskBody.setRectangles(Arrays.asList("rectangle"));
		maskBodies.add(maskBody);
		profilePipeBody.setMasks(maskBodies);
		List<ProfileElementBody> profileElementBodies = new ArrayList<>();
		ProfileElementBody profileElementBody = new ProfileElementBody();
		profileElementBody.setName("name");
		profileElementBody.setCodename("basic.dicom.profile");
		profileElementBody.setCondition("condition");
		profileElementBody.setAction("action");
		profileElementBody.setOption("option");
		Map<String, String> arguments = new HashMap<>();
		arguments.putIfAbsent("key", "value");
		profileElementBody.setArguments(arguments);
		profileElementBody.setTags(Arrays.asList("tag"));
		profileElementBody.setExcludedTags(Arrays.asList("excludedTag"));
		profileElementBodies.add(profileElementBody);
		profilePipeBody.setProfileElements(profileElementBodies);

		// Call service
		List<ProfileError> profileErrors = profilePipeService.validateProfile(profilePipeBody);

		// Test results
		assertEquals(1, profileErrors.size());
		assertNull(profileErrors.get(0).getError());
	}

	@Test
	void should_update_profile() {

		// Init data
		ProfileEntity profileEntity = new ProfileEntity();

		// Call service
		profilePipeService.updateProfile(profileEntity);

		// Test results
		Mockito.verify(profileRepositoryMock, Mockito.times(1)).saveAndFlush(Mockito.any(ProfileEntity.class));
	}

	@Test
	void should_delete_profile() {

		// Init data
		ProfileEntity profileEntity = new ProfileEntity();
		profileEntity.setId(1L);

		// Call service
		profilePipeService.deleteProfile(profileEntity);

		// Test results
		Mockito.verify(profileRepositoryMock, Mockito.times(1)).deleteById(Mockito.anyLong());
	}

}
