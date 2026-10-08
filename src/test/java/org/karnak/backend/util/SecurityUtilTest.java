/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.enums.SecurityRole;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * {@link SecurityUtil} reads the current authentication from the
 * {@link SecurityContextHolder}; each test installs an authentication and clears it
 * afterwards.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class SecurityUtilTest {

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	private static void authenticate(GrantedAuthority... authorities) {
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken("user", "pw", List.of(authorities)));
	}

	@Test
	void no_authentication_means_not_logged_in() {
		assertFalse(SecurityUtil.isUserLoggedIn());
	}

	@Test
	void an_anonymous_token_is_not_logged_in() {
		SecurityContextHolder.getContext()
			.setAuthentication(new AnonymousAuthenticationToken("key", "anonymous",
					List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

		assertFalse(SecurityUtil.isUserLoggedIn());
	}

	@Test
	void an_authenticated_user_is_logged_in() {
		authenticate(new SimpleGrantedAuthority(SecurityRole.USER_ROLE.getRole()));

		assertTrue(SecurityUtil.isUserLoggedIn());
	}

	@Test
	void an_admin_authority_is_recognised_as_admin() {
		authenticate(new SimpleGrantedAuthority(SecurityRole.ADMIN_ROLE.getRole()));

		assertTrue(SecurityUtil.isUserAdmin());
	}

	@Test
	void a_plain_user_is_not_admin() {
		authenticate(new SimpleGrantedAuthority(SecurityRole.USER_ROLE.getRole()));

		assertFalse(SecurityUtil.isUserAdmin());
	}

	@Test
	void a_logged_out_user_is_not_admin() {
		assertFalse(SecurityUtil.isUserAdmin());
	}

}