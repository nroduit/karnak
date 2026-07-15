/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.profilepipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.ExcludedTagEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.MaskEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.data.entity.ProfileGroupEntity;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.repo.ProfileGroupRepo;
import org.karnak.backend.data.repo.ProfileRepo;
import org.karnak.backend.enums.NodeEventType;
import org.karnak.backend.enums.ProfileItemType;
import org.karnak.backend.model.event.NodeEvent;
import org.karnak.backend.model.profilebody.ProfilePipeBody;
import org.karnak.frontend.profile.component.errorprofile.ProfileError;
import org.karnak.frontend.util.CollatorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.weasis.core.util.annotations.Generated;

@Service
@Generated()
public class ProfilePipeService {

	// Repositories
	private final ProfileRepo profileRepo;

	private final ProfileGroupRepo profileGroupRepo;

	// Event publisher: used to notify the DICOM gateway that a profile changed so it
	// reloads its in-memory de-identification pipeline instead of keeping the old one.
	private final ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	public ProfilePipeService(final ProfileRepo profileRepo, final ProfileGroupRepo profileGroupRepo,
			final ApplicationEventPublisher applicationEventPublisher) {
		this.profileRepo = profileRepo;
		this.profileGroupRepo = profileGroupRepo;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public List<ProfileEntity> getAllProfiles() {
		return new ArrayList<>(profileRepo.findAll());
	}

	/** Retrieve all profile groups, sorted by name. */
	public List<ProfileGroupEntity> getAllGroups() {
		List<ProfileGroupEntity> groups = profileGroupRepo.findAll();
		groups.sort(CollatorUtils.comparing(ProfileGroupEntity::getName));
		return groups;
	}

	/** Create and persist a new profile group. */
	public ProfileGroupEntity saveGroup(String name) {
		return profileGroupRepo.saveAndFlush(new ProfileGroupEntity(name));
	}

	/** Rename an existing profile group. */
	public void renameGroup(ProfileGroupEntity group, String name) {
		if (group == null || group.getId() == null) {
			return;
		}
		profileGroupRepo.findById(group.getId()).ifPresent(persisted -> {
			persisted.setName(name);
			profileGroupRepo.saveAndFlush(persisted);
		});
	}

	/**
	 * Delete a profile group. Members are detached first (their group is set to null) so
	 * they fall back to the root of the list instead of being deleted.
	 */
	public void deleteGroup(ProfileGroupEntity group) {
		if (group == null || group.getId() == null) {
			return;
		}
		profileRepo.findAll().forEach(profile -> {
			if (profile.getGroup() != null && Objects.equals(profile.getGroup().getId(), group.getId())) {
				profile.setGroup(null);
				profileRepo.saveAndFlush(profile);
			}
		});
		profileGroupRepo.deleteById(group.getId());
		profileGroupRepo.flush();
	}

	/** Assign a profile to a group (null group removes it from any group). */
	public void assignToGroup(ProfileEntity profile, ProfileGroupEntity group) {
		if (profile == null || profile.getId() == null) {
			return;
		}
		profileRepo.findById(profile.getId()).ifPresent(persisted -> {
			persisted.setGroup(group);
			profileRepo.saveAndFlush(persisted);
		});
	}

	public List<ProfileError> validateProfile(ProfilePipeBody profilePipeYml) {
		ProfileEntity newProfileEntity = createNewProfile(profilePipeYml, false);
		ArrayList<ProfileError> profileErrors = new ArrayList<>();
		for (ProfileElementEntity profileElementEntity : newProfileEntity.getProfileElementEntities()) {
			profileErrors.add(validateElement(profileElementEntity));
		}
		return profileErrors;
	}

	/**
	 * Validate a single profile element by instantiating its {@link ProfileItemType}
	 * profile class (the canonical way: the constructor runs {@code profileValidation}).
	 * @param profileElementEntity the element to validate (its tags/arguments must
	 * back-reference it)
	 * @return a {@link ProfileError} whose {@code error} is {@code null} when the element
	 * is valid, or the validation message otherwise
	 */
	public ProfileError validateElement(ProfileElementEntity profileElementEntity) {
		ProfileError profileError = new ProfileError(profileElementEntity);
		ProfileItemType t = ProfileItemType.getType(profileElementEntity.getCodename());
		if (t == null) {
			profileError.setError("Cannot find the profile codename: " + profileElementEntity.getCodename());
		}
		else {
			try {
				t.getProfileClass().getConstructor(ProfileElementEntity.class).newInstance(profileElementEntity);
			}
			catch (Exception e) {
				Throwable cause = e.getCause() != null ? e.getCause() : e;
				profileError.setError(cause != null && cause.getMessage() != null ? cause.getMessage() : "");
			}
		}
		return profileError;
	}

	public ProfileEntity saveProfilePipe(ProfilePipeBody profilePipeYml, Boolean byDefault) {
		ProfileEntity newProfileEntity = createNewProfile(profilePipeYml, byDefault);
		return profileRepo.saveAndFlush(newProfileEntity);
	}

	/** Create and persist a new, empty (no element) editable profile. */
	public ProfileEntity createEmptyProfile(String name, String version, String minimumKarnakVersion) {
		return profileRepo.saveAndFlush(new ProfileEntity(name, version, minimumKarnakVersion, null, false));
	}

	/**
	 * Add a new element (when {@code element.getId() == null}) or replace an existing one
	 * (matched by id) in the given profile, then persist. The element keeps its position
	 * when edited, or is appended at the end when added. Default profiles are left
	 * untouched.
	 * @param profileId the profile to mutate
	 * @param element the element to save (its tags/arguments back-references are fixed
	 * here)
	 * @return the reloaded, persisted profile (or the unchanged profile when it is a
	 * default one or no longer exists)
	 */
	public @Nullable ProfileEntity saveElement(Long profileId, ProfileElementEntity element) {
		ProfileEntity profile = profileRepo.findById(profileId).orElse(null);
		if (profile == null || Boolean.TRUE.equals(profile.getByDefault())) {
			return profile;
		}
		// A unique type (basic.dicom.profile, clean.pixel.data, defacing) may appear
		// once.
		if (ProfileItemType.isUnique(element.getCodename()) && profile.getProfileElementEntities()
			.stream()
			.anyMatch(e -> element.getCodename().equals(e.getCodename())
					&& !Objects.equals(e.getId(), element.getId()))) {
			return profile;
		}
		int position = profile.getProfileElementEntities().size();
		if (element.getId() != null) {
			ProfileElementEntity existing = findElement(profile, element.getId());
			if (existing != null) {
				position = existing.getPosition();
				// orphanRemoval deletes the previous element (and its tags/arguments)
				profile.getProfileElementEntities().remove(existing);
			}
		}
		attachElement(profile, element, position);
		profile.addProfilePipe(element);
		enforceBasicProfileLast(profile);
		ProfileEntity saved = profileRepo.saveAndFlush(profile);
		notifyGatewayProfileChanged(saved);
		return saved;
	}

	/** Delete an element from a profile and re-index the remaining positions (0..n-1). */
	public @Nullable ProfileEntity deleteElement(Long profileId, Long elementId) {
		ProfileEntity profile = profileRepo.findById(profileId).orElse(null);
		if (profile == null || Boolean.TRUE.equals(profile.getByDefault())) {
			return profile;
		}
		profile.getProfileElementEntities().removeIf(e -> Objects.equals(e.getId(), elementId));
		enforceBasicProfileLast(profile);
		ProfileEntity saved = profileRepo.saveAndFlush(profile);
		notifyGatewayProfileChanged(saved);
		return saved;
	}

	/**
	 * Reorder a profile's elements: each element's position is set to its index in the
	 * given id list (which must contain all of the profile's element ids). The Basic
	 * DICOM confidentiality profile is always forced back to the last position.
	 */
	public @Nullable ProfileEntity reorderElements(Long profileId, List<Long> orderedElementIds) {
		ProfileEntity profile = profileRepo.findById(profileId).orElse(null);
		if (profile == null || Boolean.TRUE.equals(profile.getByDefault())) {
			return profile;
		}
		for (int i = 0; i < orderedElementIds.size(); i++) {
			int position = i;
			ProfileElementEntity element = findElement(profile, orderedElementIds.get(i));
			if (element != null) {
				element.setPosition(position);
			}
		}
		enforceBasicProfileLast(profile);
		ProfileEntity saved = profileRepo.saveAndFlush(profile);
		notifyGatewayProfileChanged(saved);
		return saved;
	}

	private static @Nullable ProfileElementEntity findElement(ProfileEntity profile, Long elementId) {
		return profile.getProfileElementEntities()
			.stream()
			.filter(e -> Objects.equals(e.getId(), elementId))
			.findFirst()
			.orElse(null);
	}

	/** Make {@code element} a fresh child of {@code profile} at the given position. */
	private static void attachElement(ProfileEntity profile, ProfileElementEntity element, int position) {
		element.setId(null);
		element.setPosition(position);
		element.setProfileEntity(profile);
		element.getIncludedTagEntities().forEach(t -> {
			t.setId(null);
			t.setProfileElementEntity(element);
		});
		element.getExcludedTagEntities().forEach(t -> {
			t.setId(null);
			t.setProfileElementEntity(element);
		});
		element.getArgumentEntities().forEach(a -> {
			a.setId(null);
			a.setProfileElementEntity(element);
		});
	}

	/**
	 * Re-index positions to a contiguous 0..n-1 sequence following the current order,
	 * while forcing the Basic DICOM confidentiality profile (if present) to the last
	 * position: a de-identification baseline must be applied after every other element.
	 */
	private static void enforceBasicProfileLast(ProfileEntity profile) {
		List<ProfileElementEntity> ordered = new ArrayList<>(profile.getProfileElementEntities());
		ordered.sort(Comparator.comparing(ProfileElementEntity::getPosition,
				Comparator.nullsLast(Comparator.naturalOrder())));
		ordered.stream()
			.filter(e -> ProfileItemType.BASIC_DICOM_ALIAS.equals(e.getCodename()))
			.findFirst()
			.ifPresent(basic -> {
				ordered.remove(basic);
				ordered.add(basic);
			});
		int position = 0;
		for (ProfileElementEntity element : ordered) {
			element.setPosition(position++);
		}
	}

	private ProfileEntity createNewProfile(ProfilePipeBody profilePipeYml, Boolean byDefault) {
		final ProfileEntity newProfileEntity = new ProfileEntity(profilePipeYml.getName(), profilePipeYml.getVersion(),
				profilePipeYml.getMinimumKarnakVersion(), null, byDefault);
		populateProfile(newProfileEntity, profilePipeYml);
		return newProfileEntity;
	}

	/**
	 * Replace an existing (non-default) profile's content in place from an edited YAML
	 * body, keeping its id, group and {@code byDefault} flag so referencing projects are
	 * not broken. Elements and masks are cleared (orphan-removed) and rebuilt from the
	 * body; the Basic DICOM confidentiality profile is forced back to the last position.
	 * @param profileId the profile to update
	 * @param profilePipeYml the parsed YAML body
	 * @return the reloaded, persisted profile (or the unchanged profile when it is a
	 * default one or no longer exists)
	 */
	public @Nullable ProfileEntity updateProfileFromYaml(Long profileId, ProfilePipeBody profilePipeYml) {
		ProfileEntity profile = profileRepo.findById(profileId).orElse(null);
		if (profile == null || Boolean.TRUE.equals(profile.getByDefault())) {
			return profile;
		}
		profile.setName(profilePipeYml.getName());
		profile.setVersion(profilePipeYml.getVersion());
		profile.setMinimumKarnakVersion(profilePipeYml.getMinimumKarnakVersion());
		profile.getProfileElementEntities().clear();
		profile.getMaskEntities().clear();
		populateProfile(profile, profilePipeYml);
		enforceBasicProfileLast(profile);
		ProfileEntity saved = profileRepo.saveAndFlush(profile);
		notifyGatewayProfileChanged(saved);
		return saved;
	}

	/** Build mask and (ordered) element children from a YAML body and attach them. */
	private void populateProfile(ProfileEntity target, ProfilePipeBody profilePipeYml) {
		if (profilePipeYml.getMasks() != null) {
			profilePipeYml.getMasks().forEach(m -> {
				MaskEntity maskEntity = new MaskEntity(m.getStationName(), m.getImageWidth(), m.getImageHeight(),
						m.getColor(), target);
				m.getRectangles().forEach(maskEntity::addRectangle);
				target.addMask(maskEntity);
			});
		}

		AtomicInteger profilePosition = new AtomicInteger(0);
		profilePipeYml.getProfileElements().forEach(profileBody -> {
			ProfileElementEntity profileElementEntity = new ProfileElementEntity(profileBody.getName(),
					profileBody.getCodename(), profileBody.getCondition(), profileBody.getAction(),
					profileBody.getOption(), profilePosition.get(), target);

			if (profileBody.getArguments() != null) {
				profileBody.getArguments().forEach((key, value) -> {
					final ArgumentEntity argumentEntity = new ArgumentEntity(key, value, profileElementEntity);
					profileElementEntity.addArgument(argumentEntity);
				});
			}

			if (profileBody.getTags() != null) {
				profileBody.getTags().forEach(tag -> {
					final IncludedTagEntity includedTagEntityValue = new IncludedTagEntity(tag, profileElementEntity);
					profileElementEntity.addIncludedTag(includedTagEntityValue);
				});
			}

			if (profileBody.getExcludedTags() != null) {
				profileBody.getExcludedTags().forEach(excludedTag -> {
					final ExcludedTagEntity excludedTagEntityValue = new ExcludedTagEntity(excludedTag,
							profileElementEntity);
					profileElementEntity.addExceptedtags(excludedTagEntityValue);
				});
			}

			target.addProfilePipe(profileElementEntity);
			profilePosition.getAndIncrement();
		});
	}

	public ProfileEntity updateProfile(ProfileEntity profileEntity) {
		ProfileEntity saved = profileRepo.saveAndFlush(profileEntity);
		notifyGatewayProfileChanged(saved);
		return saved;
	}

	public void deleteProfile(ProfileEntity profileEntity) {
		profileRepo.deleteById(profileEntity.getId());
		profileRepo.flush();
	}

	/**
	 * Notify the DICOM gateway that a profile's content changed so it rebuilds its
	 * in-memory de-identification pipeline. A {@link NodeEvent} is published for every
	 * destination whose project references the edited profile; the gateway listens for
	 * these events, bumps its configuration version and reloads on the next scheduled
	 * check. Without this, edits to a profile are persisted but the gateway keeps
	 * applying the previously loaded (stale) profile until it is restarted.
	 * @param profileEntity the edited, persisted profile (ignored when {@code null})
	 */
	private void notifyGatewayProfileChanged(@Nullable ProfileEntity profileEntity) {
		if (profileEntity == null || profileEntity.getProjectEntities() == null) {
			return;
		}
		for (ProjectEntity projectEntity : profileEntity.getProjectEntities()) {
			if (projectEntity.getDestinationEntities() == null) {
				continue;
			}
			for (DestinationEntity destinationEntity : projectEntity.getDestinationEntities()) {
				applicationEventPublisher.publishEvent(new NodeEvent(destinationEntity, NodeEventType.UPDATE));
			}
		}
	}

}
