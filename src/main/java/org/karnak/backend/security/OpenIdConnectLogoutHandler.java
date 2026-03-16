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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Handle IDP logout
 */
@Slf4j
public class OpenIdConnectLogoutHandler extends SecurityContextLogoutHandler {

	private static final String END_SESSION_ENDPOINT = "/protocol/openid-connect/logout";

	private static final String ID_TOKEN_HINT = "id_token_hint";

	@Override
	public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
		// Security context logout
		super.logout(request, response, authentication);

		if (authentication != null && authentication.getPrincipal() instanceof OidcUser) {
			// Idp logout
			propagateLogoutToIdp((OidcUser) authentication.getPrincipal());
		}
	}

	/**
	 * Propagate logout to IDP
	 * @param user OpenId Connect user
	 */
	private void propagateLogoutToIdp(OidcUser user) {
		RestTemplate restTemplate = new RestTemplate();

		// Build logout URI
		String endSessionEndpoint = user.getIssuer() + END_SESSION_ENDPOINT;
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endSessionEndpoint)
			.queryParam(ID_TOKEN_HINT, user.getIdToken().getTokenValue());

		// Call IDP logout endpoint
		ResponseEntity<String> logoutResponse = restTemplate.getForEntity(builder.toUriString(), String.class);
		log.info(logoutResponse.getStatusCode().is2xxSuccessful() ? "Successful IDP logout"
				: "Could not propagate logout to IDP");
	}

}
