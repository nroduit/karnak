/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.enums.ApplicationProfile;
import org.karnak.backend.enums.EnvironmentVariable;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link DefaultIdpLoadCondition} matches whenever the {@code IDP} property is anything
 * other than {@code oidc}, so the in-memory identity provider is loaded.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class DefaultIdpLoadConditionTest {

	private final DefaultIdpLoadCondition condition = new DefaultIdpLoadCondition();

	private final AnnotatedTypeMetadata metadata = mock(AnnotatedTypeMetadata.class);

	private static ConditionContext contextWithIdp(String idpValue) {
		Environment environment = mock(Environment.class);
		when(environment.getProperty(EnvironmentVariable.IDP.getCode())).thenReturn(idpValue);
		ConditionContext context = mock(ConditionContext.class);
		when(context.getEnvironment()).thenReturn(environment);
		return context;
	}

	@Test
	void matches_when_the_idp_property_is_absent() {
		assertTrue(condition.matches(contextWithIdp(null), metadata));
	}

	@Test
	void matches_when_the_idp_property_is_not_oidc() {
		assertTrue(condition.matches(contextWithIdp("undefined"), metadata));
	}

	@Test
	void does_not_match_when_the_idp_property_is_oidc() {
		assertFalse(condition.matches(contextWithIdp(ApplicationProfile.OIDC.getCode()), metadata));
	}

}