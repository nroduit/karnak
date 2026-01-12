/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak;

import com.vaadin.flow.spring.annotation.EnableVaadin;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.karnak.backend.config.AppConfig;
import org.karnak.backend.enums.ApplicationProfile;
import org.karnak.backend.enums.EnvironmentVariable;
import org.opencv.osgi.OpenCVNativeLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
@EntityScan("org.karnak.backend.data.entity")
@EnableJpaRepositories("org.karnak.backend.data.repo")
@EnableVaadin(value = "org.karnak")
@EnableScheduling
@EnableAsync
@Slf4j
public class KarnakApplication implements CommandLineRunner {

	private final AppConfig myConfig;

	private final Environment environment;

	@Autowired
	public KarnakApplication(@Nullable AppConfig myConfig, Environment environment) {
		this.myConfig = myConfig;
		this.environment = environment;
	}

	@Value("${server.port}")
	private int serverPort;

	public static void main(String[] args) {

		// Load open cv library
		log.info("Loading open cv native library");
		loadOpenCvNativeLibrary();

		SpringApplicationBuilder application = new SpringApplicationBuilder(KarnakApplication.class);

		// If environment variable IDP exists and has value "oidc": activate the profile
		// application-oidc.yml
		if (System.getenv().containsKey(EnvironmentVariable.IDP.getCode()) && Objects
			.equals(System.getenv().get(EnvironmentVariable.IDP.getCode()), ApplicationProfile.OIDC.getCode())) {
			application.profiles(ApplicationProfile.OIDC.getCode());
		}

		// Run application
		application.run(args);
	}

	@Override
	public void run(String... args) {
		log.info("Karnak application started successfully");
		log.info("Environment: {}", myConfig != null ? myConfig.getEnvironment() : "not configured");
		log.info("Profile name: {}", myConfig != null ? myConfig.getName() : "default");
		try {
			String hostname;
			if (environment.acceptsProfiles(Profiles.of("portable"))) {
				hostname = "localhost";
			}
			else {
				hostname = InetAddress.getLocalHost().getHostName();
			}
			log.info("Web UI available at: http://{}:{}", hostname, serverPort);
		}
		catch (UnknownHostException e) {
			log.info("Web UI available at: http://localhost:{}", serverPort);
		}
	}

	/**
	 * Load Open CV library
	 */
	private static void loadOpenCvNativeLibrary() {
		OpenCVNativeLoader loader = new OpenCVNativeLoader();
		loader.init();
		log.info("Native OpenCV is activated");
	}

}
