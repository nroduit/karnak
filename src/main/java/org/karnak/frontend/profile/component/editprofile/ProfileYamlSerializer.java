/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.profile.component.editprofile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;

/**
 * Serializes a {@link ProfileEntity} to the YAML representation shared by the profile
 * download / import file and the raw YAML editor.
 */
@Slf4j
public final class ProfileYamlSerializer {

	private ProfileYamlSerializer() {
	}

	public static String toYaml(ProfileEntity profileEntity) {
		try {
			Set<ProfileElementEntity> profileElementEntities = profileEntity.getProfileElementEntities()
				.stream()
				.sorted(Comparator.comparing(ProfileElementEntity::getPosition))
				.collect(Collectors.toCollection(LinkedHashSet::new));
			profileEntity.setProfileElementEntities(profileElementEntities);

			// https://stackoverflow.com/questions/61506368/formatting-yaml-with-jackson
			ObjectMapper mapper = new ObjectMapper(
					new YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER));

			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(profileEntity);
		}
		catch (final Exception e) {
			log.error("Cannot create the StreamResource for downloading the yaml profile", e);
		}
		return "";
	}

}
