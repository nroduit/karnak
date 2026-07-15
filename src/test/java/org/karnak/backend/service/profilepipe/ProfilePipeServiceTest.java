/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.profilepipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.data.entity.ProfileGroupEntity;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.repo.ProfileGroupRepo;
import org.karnak.backend.data.repo.ProfileRepo;
import org.karnak.backend.model.event.NodeEvent;
import org.karnak.backend.model.profilebody.MaskBody;
import org.karnak.backend.model.profilebody.ProfileElementBody;
import org.karnak.backend.model.profilebody.ProfilePipeBody;
import org.karnak.frontend.profile.component.errorprofile.ProfileError;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ProfilePipeServiceTest {

	// Repositories
	private final ProfileRepo profileRepositoryMock = Mockito.mock(ProfileRepo.class);

	private final ProfileGroupRepo profileGroupRepositoryMock = Mockito.mock(ProfileGroupRepo.class);

	// Event publisher
	private final ApplicationEventPublisher applicationEventPublisherMock = Mockito
		.mock(ApplicationEventPublisher.class);

	// Service
	private ProfilePipeService profilePipeService;

	@BeforeEach
	public void setUp() {
		// Build mocked service
		profilePipeService = new ProfilePipeService(profileRepositoryMock, profileGroupRepositoryMock,
				applicationEventPublisherMock);
		// saveAndFlush returns the entity it was given, like the real repository
		Mockito.when(profileRepositoryMock.saveAndFlush(Mockito.any(ProfileEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));
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

	@Test
	void should_assign_profile_to_group() {
		// Init data
		ProfileEntity profileEntity = new ProfileEntity();
		profileEntity.setId(1L);
		ProfileGroupEntity group = new ProfileGroupEntity("Group A");
		group.setId(10L);
		Mockito.when(profileRepositoryMock.findById(1L)).thenReturn(Optional.of(profileEntity));

		// Call service
		profilePipeService.assignToGroup(profileEntity, group);

		// Test results
		assertEquals(group, profileEntity.getGroup());
		Mockito.verify(profileRepositoryMock, Mockito.times(1)).saveAndFlush(profileEntity);
	}

	@Test
	void creates_an_empty_non_default_profile() {
		ProfileEntity created = profilePipeService.createEmptyProfile("New", "1.0", "1.2.0");

		assertEquals("New", created.getName());
		assertFalse(created.getByDefault());
		assertTrue(created.getProfileElementEntities().isEmpty());
		Mockito.verify(profileRepositoryMock).saveAndFlush(Mockito.any(ProfileEntity.class));
	}

	@Test
	void add_element_appends_it_at_the_last_position() {
		ProfileEntity profile = profileWithElements(1L, 1);
		Mockito.when(profileRepositoryMock.findById(1L)).thenReturn(Optional.of(profile));
		ProfileElementEntity newElement = actionTagsElement(null, "Keep name", 0);

		profilePipeService.saveElement(1L, newElement);

		assertEquals(2, profile.getProfileElementEntities().size());
		assertEquals(1, newElement.getPosition());
	}

	@Test
	void update_element_keeps_its_position_and_replaces_the_old_one() {
		ProfileEntity profile = profileWithElements(1L, 2);
		Mockito.when(profileRepositoryMock.findById(1L)).thenReturn(Optional.of(profile));
		// Edit the element currently at position 1 (id 101)
		ProfileElementEntity edited = actionTagsElement(101L, "Edited", 999);

		profilePipeService.saveElement(1L, edited);

		assertEquals(2, profile.getProfileElementEntities().size());
		ProfileElementEntity atPositionOne = elementAt(profile, 1);
		assertEquals("Edited", atPositionOne.getName());
		assertTrue(profile.getProfileElementEntities().stream().noneMatch(e -> Long.valueOf(101L).equals(e.getId())));
	}

	@Test
	void delete_element_reindexes_remaining_positions() {
		ProfileEntity profile = profileWithElements(1L, 3);
		Mockito.when(profileRepositoryMock.findById(1L)).thenReturn(Optional.of(profile));

		profilePipeService.deleteElement(1L, 101L);

		List<Integer> positions = profile.getProfileElementEntities()
			.stream()
			.map(ProfileElementEntity::getPosition)
			.sorted()
			.toList();
		assertEquals(List.of(0, 1), positions);
	}

	@Test
	void reorder_elements_assigns_positions_from_the_given_order() {
		ProfileEntity profile = profileWithElements(1L, 2);
		Mockito.when(profileRepositoryMock.findById(1L)).thenReturn(Optional.of(profile));

		profilePipeService.reorderElements(1L, List.of(101L, 100L));

		assertEquals(0, element(profile, 101L).getPosition());
		assertEquals(1, element(profile, 100L).getPosition());
	}

	@Test
	void basic_dicom_profile_is_forced_to_the_last_position() {
		ProfileEntity profile = profileWithElements(1L, 2);
		Mockito.when(profileRepositoryMock.findById(1L)).thenReturn(Optional.of(profile));

		// Add the basic profile, then another element afterwards
		profilePipeService.saveElement(1L, basicProfileElement());
		profilePipeService.saveElement(1L, actionTagsElement(null, "Another", 0));

		ProfileElementEntity last = profile.getProfileElementEntities()
			.stream()
			.max(Comparator.comparing(ProfileElementEntity::getPosition))
			.orElseThrow();
		assertEquals("basic.dicom.profile", last.getCodename());
		assertEquals(4, profile.getProfileElementEntities().size());
	}

	@Test
	void a_unique_type_cannot_be_added_twice() {
		ProfileEntity profile = new ProfileEntity("P", "1", "1", null, false);
		profile.setId(1L);
		ProfileElementEntity basic = basicProfileElement();
		basic.setId(200L);
		basic.setProfileEntity(profile);
		profile.addProfilePipe(basic);
		Mockito.when(profileRepositoryMock.findById(1L)).thenReturn(Optional.of(profile));

		profilePipeService.saveElement(1L, basicProfileElement());

		assertEquals(1, profile.getProfileElementEntities().size());
	}

	@Test
	void does_not_mutate_a_default_profile() {
		ProfileEntity profile = profileWithElements(1L, 1);
		profile.setByDefault(true);
		Mockito.when(profileRepositoryMock.findById(1L)).thenReturn(Optional.of(profile));

		profilePipeService.saveElement(1L, actionTagsElement(null, "ignored", 0));

		assertEquals(1, profile.getProfileElementEntities().size());
		Mockito.verify(profileRepositoryMock, Mockito.never()).saveAndFlush(Mockito.any(ProfileEntity.class));
	}

	@Test
	void editing_a_profile_notifies_the_gateway_for_each_linked_destination() {
		ProfileEntity profile = profileWithElements(1L, 1);
		linkProjectWithDestinations(profile, 2);
		Mockito.when(profileRepositoryMock.findById(1L)).thenReturn(Optional.of(profile));

		profilePipeService.saveElement(1L, actionTagsElement(null, "Keep name", 0));

		// One NodeEvent per destination so the gateway reloads the edited profile
		Mockito.verify(applicationEventPublisherMock, Mockito.times(2)).publishEvent(Mockito.any(NodeEvent.class));
	}

	@Test
	void editing_a_profile_without_projects_publishes_no_event() {
		ProfileEntity profile = profileWithElements(1L, 1);
		Mockito.when(profileRepositoryMock.findById(1L)).thenReturn(Optional.of(profile));

		profilePipeService.saveElement(1L, actionTagsElement(null, "Keep name", 0));

		Mockito.verify(applicationEventPublisherMock, Mockito.never()).publishEvent(Mockito.any());
	}

	/** Attach a project owning {@code destinationCount} destinations to the profile. */
	private static void linkProjectWithDestinations(ProfileEntity profile, int destinationCount) {
		ProjectEntity project = new ProjectEntity();
		ForwardNodeEntity forwardNode = new ForwardNodeEntity("FWD_AET");
		List<DestinationEntity> destinations = new ArrayList<>();
		for (int i = 0; i < destinationCount; i++) {
			DestinationEntity destination = new DestinationEntity();
			destination.setForwardNodeEntity(forwardNode);
			destinations.add(destination);
		}
		project.setDestinationEntities(destinations);
		profile.setProjectEntities(List.of(project));
	}

	@Test
	void validate_element_flags_an_unknown_codename() {
		ProfileElementEntity element = new ProfileElementEntity("name", "does.not.exist", null, "K", null, 0, null);

		ProfileError error = profilePipeService.validateElement(element);

		assertNotNull(error.getError());
		assertTrue(error.getError().contains("does.not.exist"));
	}

	@Test
	void validate_element_flags_an_action_tags_without_action() {
		ProfileElementEntity element = actionTagsElement(null, "name", 0);
		element.setAction(null);

		ProfileError error = profilePipeService.validateElement(element);

		assertNotNull(error.getError());
	}

	@Test
	void validate_element_accepts_a_well_formed_action_tags() {
		ProfileError error = profilePipeService.validateElement(actionTagsElement(null, "Keep", 0));

		assertNull(error.getError());
	}

	/**
	 * Build a profile with {@code count} action.on.specific.tags elements (ids 100..).
	 */
	private static ProfileEntity profileWithElements(Long profileId, int count) {
		ProfileEntity profile = new ProfileEntity("P", "1", "1", null, false);
		profile.setId(profileId);
		for (int i = 0; i < count; i++) {
			ProfileElementEntity element = actionTagsElement(100L + i, "Element " + i, i);
			element.setProfileEntity(profile);
			profile.addProfilePipe(element);
		}
		return profile;
	}

	private static ProfileElementEntity actionTagsElement(Long id, String name, int position) {
		ProfileElementEntity element = new ProfileElementEntity(name, "action.on.specific.tags", null, "K", null,
				position, null);
		element.setId(id);
		element.addIncludedTag(new IncludedTagEntity("(0010,0010)", element));
		return element;
	}

	private static ProfileElementEntity basicProfileElement() {
		return new ProfileElementEntity("Basic", "basic.dicom.profile", null, null, null, 0, null);
	}

	private static ProfileElementEntity elementAt(ProfileEntity profile, int position) {
		return profile.getProfileElementEntities()
			.stream()
			.filter(e -> Integer.valueOf(position).equals(e.getPosition()))
			.findFirst()
			.orElseThrow();
	}

	private static ProfileElementEntity element(ProfileEntity profile, Long id) {
		return profile.getProfileElementEntities()
			.stream()
			.filter(e -> id.equals(e.getId()))
			.min(Comparator.comparing(ProfileElementEntity::getPosition))
			.orElseThrow();
	}

	@Test
	void should_delete_group_and_detach_only_its_members() {
		// Init data
		ProfileGroupEntity group = new ProfileGroupEntity("Group A");
		group.setId(10L);
		ProfileEntity member = new ProfileEntity();
		member.setId(1L);
		member.setGroup(group);
		ProfileEntity other = new ProfileEntity();
		other.setId(2L);
		Mockito.when(profileRepositoryMock.findAll()).thenReturn(List.of(member, other));

		// Call service
		profilePipeService.deleteGroup(group);

		// Test results
		assertNull(member.getGroup());
		Mockito.verify(profileRepositoryMock, Mockito.times(1)).saveAndFlush(member);
		Mockito.verify(profileRepositoryMock, Mockito.never()).saveAndFlush(other);
		Mockito.verify(profileGroupRepositoryMock, Mockito.times(1)).deleteById(10L);
	}

}
