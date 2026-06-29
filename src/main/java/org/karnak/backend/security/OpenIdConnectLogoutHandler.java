/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Logs out the local security context and propagates the logout to the OpenID Connect
 * identity provider.
 */
@Slf4j
public class OpenIdConnectLogoutHandler extends SecurityContextLogoutHandler {

	private static final String END_SESSION_ENDPOINT = "/protocol/openid-connect/logout";

	private static final String ID_TOKEN_HINT = "id_token_hint";

	private static final Duration IDP_TIMEOUT = Duration.ofSeconds(5);

	private final RestClient restClient;

	public OpenIdConnectLogoutHandler() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(IDP_TIMEOUT);
		requestFactory.setReadTimeout(IDP_TIMEOUT);
		this.restClient = RestClient.builder().requestFactory(requestFactory).build();
	}

	@Override
	public void logout(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@Nullable Authentication authentication) {
		super.logout(request, response, authentication);

		if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
			propagateLogoutToIdp(oidcUser);
		}
	}

	/**
	 * Best-effort IDP logout: the local logout already succeeded, so failures are only
	 * logged.
	 */
	private void propagateLogoutToIdp(OidcUser user) {
		String endSessionEndpoint = user.getIssuer() + END_SESSION_ENDPOINT;
		String logoutUri = UriComponentsBuilder.fromUriString(endSessionEndpoint)
			.queryParam(ID_TOKEN_HINT, user.getIdToken().getTokenValue())
			.toUriString();

		try {
			restClient.get().uri(logoutUri).retrieve().toBodilessEntity();
			log.info("Successful IDP logout");
		}
		catch (RuntimeException e) {
			log.warn("Could not propagate logout to IDP", e);
		}
	}

}
