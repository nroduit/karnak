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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * {@link OpenIdConnectLogoutHandler} always performs the local logout, then makes a
 * best-effort attempt to propagate the logout to the OIDC identity provider for an
 * {@link OidcUser} principal.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class OpenIdConnectLogoutHandlerTest {

	private final OpenIdConnectLogoutHandler handler = new OpenIdConnectLogoutHandler();

	private final HttpServletRequest request = mock(HttpServletRequest.class);

	private final HttpServletResponse response = mock(HttpServletResponse.class);

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void clears_the_local_security_context() {
		Authentication authentication = new UsernamePasswordAuthenticationToken("user", "pw");
		SecurityContextHolder.getContext().setAuthentication(authentication);

		handler.logout(request, response, authentication);

		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	void does_not_fail_without_an_authentication() {
		assertDoesNotThrow(() -> handler.logout(request, response, null));
	}

	@Test
	void does_not_propagate_for_a_non_oidc_principal() {
		Authentication authentication = new UsernamePasswordAuthenticationToken("plain-user", "pw");

		assertDoesNotThrow(() -> handler.logout(request, response, authentication));
	}

	@Test
	void swallows_a_failure_to_reach_the_identity_provider() throws Exception {
		OidcUser oidcUser = mock(OidcUser.class);
		URL issuer = URI.create("http://localhost:1/realms/test").toURL();
		when(oidcUser.getIssuer()).thenReturn(issuer);
		when(oidcUser.getIdToken())
			.thenReturn(OidcIdToken.withTokenValue("id-token-value").claim("sub", "user").build());
		Authentication authentication = new UsernamePasswordAuthenticationToken(oidcUser, null);

		// The IDP at localhost:1 is unreachable; the handler must not propagate the
		// error.
		assertDoesNotThrow(() -> handler.logout(request, response, authentication));
	}

}