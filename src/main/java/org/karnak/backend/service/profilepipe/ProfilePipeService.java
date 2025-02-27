/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.profilepipe;

import org.karnak.backend.data.entity.*;
import org.karnak.backend.data.repo.ProfileRepo;
import org.karnak.backend.enums.ProfileItemType;
import org.karnak.backend.model.profilebody.ProfilePipeBody;
import org.karnak.frontend.profile.component.errorprofile.ProfileError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ProfilePipeService {

	// Repositories
	private final ProfileRepo profileRepo;

	@Autowired
	public ProfilePipeService(final ProfileRepo profileRepo) {
		this.profileRepo = profileRepo;
	}

	public List<ProfileEntity> getAllProfiles() {
		List<ProfileEntity> list = new ArrayList<>();
		profileRepo.findAll() //
			.forEach(list::add);
		return list;
	}

	public ArrayList<ProfileError> validateProfile(ProfilePipeBody profilePipeYml) {
		ProfileEntity newProfileEntity = createNewProfile(profilePipeYml, false);
		ArrayList<ProfileError> profileErrors = new ArrayList<>();
		for (ProfileElementEntity profileElementEntity : newProfileEntity.getProfileElementEntities()) {
			ProfileError profileError = new ProfileError(profileElementEntity);
			profileErrors.add(profileError);
			ProfileItemType t = ProfileItemType.getType(profileElementEntity.getCodename());
			if (t == null) {
				profileError.setError("Cannot find the profile codename: " + profileElementEntity.getCodename());
			}
			else {
				try {
					t.getProfileClass().getConstructor(ProfileElementEntity.class).newInstance(profileElementEntity);
				}
				catch (Exception e) {
					profileError.setError(e.getCause().getMessage());
					continue;
				}
			}
		}
		return profileErrors;
	}

	public ProfileEntity saveProfilePipe(ProfilePipeBody profilePipeYml, Boolean byDefault) {
		ProfileEntity newProfileEntity = createNewProfile(profilePipeYml, byDefault);
		return profileRepo.saveAndFlush(newProfileEntity);
	}

	private ProfileEntity createNewProfile(ProfilePipeBody profilePipeYml, Boolean byDefault) {
		final ProfileEntity newProfileEntity = new ProfileEntity(profilePipeYml.getName(), profilePipeYml.getVersion(),
				profilePipeYml.getMinimumKarnakVersion(), null, byDefault);
		if (profilePipeYml.getMasks() != null) {
			profilePipeYml.getMasks().forEach(m -> {
				MaskEntity maskEntity = new MaskEntity(m.getStationName(), m.getImageWidth(), m.getImageHeight(), m.getColor(), newProfileEntity);
				m.getRectangles().forEach(maskEntity::addRectangle);
				newProfileEntity.addMask(maskEntity);
			});
		}

		AtomicInteger profilePosition = new AtomicInteger(0);
		profilePipeYml.getProfileElements().forEach(profileBody -> {
			ProfileElementEntity profileElementEntity = new ProfileElementEntity(profileBody.getName(),
					profileBody.getCodename(), profileBody.getCondition(), profileBody.getAction(),
					profileBody.getOption(), profilePosition.get(), newProfileEntity);

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

			newProfileEntity.addProfilePipe(profileElementEntity);
			profilePosition.getAndIncrement();
		});
		return newProfileEntity;
	}

	public ProfileEntity updateProfile(ProfileEntity profileEntity) {
		return profileRepo.saveAndFlush(profileEntity);
	}

	public void deleteProfile(ProfileEntity profileEntity) {
		profileRepo.deleteById(profileEntity.getId());
		profileRepo.flush();
	}

}
