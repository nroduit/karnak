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
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "dcmprofile")
public class DcmProfileConfig {

	private static @Nullable DcmProfileConfig instance;

	public static @Nullable DcmProfileConfig getInstance() {
		return instance;
	}

	public static void setInstance(DcmProfileConfig instance) {
		DcmProfileConfig.instance = instance;
	}

	@PostConstruct
	public void postConstruct() {
		instance = this;
	}

}
