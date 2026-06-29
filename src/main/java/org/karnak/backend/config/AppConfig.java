/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.config;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URL;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.cache.PatientClient;
import org.karnak.backend.data.repo.ProfileRepo;
import org.karnak.backend.model.profilebody.ProfilePipeBody;
import org.karnak.backend.model.standard.ConfidentialityProfiles;
import org.karnak.backend.model.standard.StandardDICOM;
import org.karnak.backend.service.profilepipe.Profile;
import org.karnak.backend.service.profilepipe.ProfilePipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties
@EnableCaching
@Slf4j
@NullUnmarked
public class AppConfig {

	@Getter
	@NonNull private static AppConfig instance;

	@Getter
	@Setter
	private String environment;

	@Getter
	@Setter
	private String name;

	@Setter
	private String karnakAdmin;

	@Setter
	private String karnakPassword;

	private final ProfileRepo profileRepo;

	private final ProfilePipeService profilePipeService;

	@Getter
	private final PatientClient externalIDCache;

	@Autowired
	public AppConfig(ProfileRepo profileRepo, ProfilePipeService profilePipeService,
			@Qualifier("patientClient") PatientClient externalIDCache) {
		this.profileRepo = profileRepo;
		this.profilePipeService = profilePipeService;
		this.externalIDCache = externalIDCache;
	}

	@PostConstruct
	public void postConstruct() {
		instance = this;
	}

	public String getKarnakAdmin() {
		return karnakAdmin != null ? karnakAdmin : "admin";
	}

	public String getKarnakPassword() {
		return karnakPassword != null ? karnakPassword : "karnak";
	}

	@Bean("ConfidentialityProfiles")
	public ConfidentialityProfiles getConfidentialityProfile() {
		return new ConfidentialityProfiles();
	}

	// https://stackoverflow.com/questions/27405713/running-code-after-spring-boot-starts
	@EventListener(ApplicationReadyEvent.class)
	public void setProfilesByDefault() {
		URL profileURL = Profile.class.getResource("profileByDefault.yml");
		if (!profileRepo.existsByNameAndByDefault("Dicom Basic Profile", true)) {
			try (InputStream inputStream = profileURL.openStream()) {
				final Yaml yaml = new Yaml(new Constructor(ProfilePipeBody.class, new LoaderOptions()));
				final ProfilePipeBody profilePipeYml = yaml.load(inputStream);
				profilePipeService.saveProfilePipe(profilePipeYml, true);
			}
			catch (final Exception e) {
				log.error("Cannot persist default profile {}", profileURL, e);
			}
		}
	}

	@Bean("StandardDICOM")
	public StandardDICOM getStandardDICOM() {
		return new StandardDICOM();
	}

}
