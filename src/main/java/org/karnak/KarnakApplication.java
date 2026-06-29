/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
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
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.config.AppConfig;
import org.karnak.backend.enums.ApplicationProfile;
import org.karnak.backend.enums.EnvironmentVariable;
import org.opencv.osgi.OpenCVNativeLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.weasis.core.util.annotations.Generated;

@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
@EntityScan("org.karnak.backend.data.entity")
@EnableJpaRepositories("org.karnak.backend.data.repo")
@EnableVaadin(value = "org.karnak")
@EnableScheduling
@EnableAsync
@Slf4j
@NullMarked
@Generated
public class KarnakApplication implements CommandLineRunner {

	private final @Nullable AppConfig myConfig;

	private final Environment environment;

	private final int serverPort;

	public KarnakApplication(@Nullable AppConfig myConfig, Environment environment,
			@Value("${server.port}") int serverPort) {
		this.myConfig = myConfig;
		this.environment = environment;
		this.serverPort = serverPort;
	}

	public static void main(String[] args) {

		// Load open cv library
		log.info("Loading open cv native library");
		loadOpenCvNativeLibrary();

		SpringApplicationBuilder application = new SpringApplicationBuilder(KarnakApplication.class);

		// If environment variable IDP has value "oidc", activate the application-oidc.yml
		// profile
		String idp = System.getenv(EnvironmentVariable.IDP.getCode());
		if (ApplicationProfile.OIDC.getCode().equals(idp)) {
			application.profiles(ApplicationProfile.OIDC.getCode());
		}

		application.run(args);
	}

	// The embedded web server serves plain HTTP (no TLS for the UI), so the http:// URL
	// is intentional.
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
