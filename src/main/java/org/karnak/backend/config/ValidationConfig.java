/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Publishes the Bean Validation {@link jakarta.validation.Validator} as a Spring bean.
 *
 * <p>
 * Spring Boot's validation auto-configuration lives in its {@code spring-boot-validation}
 * module, which this application does not depend on: only hibernate-validator is on the
 * classpath. Spring MVC still validates {@code @Valid} request bodies with its own
 * validator, but nothing is injectable. This bean fills that gap for code that validates
 * objects itself, such as the per-row validation of the pseudonym bulk import, and Spring
 * MVC reuses it as its request validator, so both paths share one validator and its
 * lifecycle is managed by the context.
 */
@Configuration
public class ValidationConfig {

	@Bean
	public LocalValidatorFactoryBean validator() {
		return new LocalValidatorFactoryBean();
	}

}
