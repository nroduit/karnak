/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.dicom;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.data.entity.AuthConfigEntity;
import org.karnak.backend.data.repo.AuthConfigRepo;
import org.karnak.backend.model.dicom.result.AuthCheckResult;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Service;

/**
 * Verifies that a client-credentials OAuth 2.0 access token can actually be obtained for
 * a DICOMweb destination's auth configuration, by performing the token request against
 * the identity provider (no DICOMweb call is made). Mirrors the registration that
 * {@code EndpointService} uses for real STOW uploads. Never throws.
 */
@Service
@Slf4j
@NullUnmarked
public class WebTokenService {

	private static final String PRINCIPAL = "dicom-tools";

	private final AuthConfigRepo authConfigRepo;

	public WebTokenService(AuthConfigRepo authConfigRepo) {
		this.authConfigRepo = authConfigRepo;
	}

	/**
	 * The token check result paired with the access token value (null when not obtained).
	 */
	public record TokenResult(AuthCheckResult check, @Nullable String token) {
	}

	public AuthCheckResult checkToken(String authConfig) {
		return authorize(authConfig).check();
	}

	/**
	 * Performs the client-credentials token request and returns both the displayable
	 * check result and the raw access token (for an authenticated follow-up probe). Never
	 * throws.
	 */
	public TokenResult authorize(String authConfig) {
		AuthConfigEntity entity = authConfigRepo.findByCode(authConfig);
		if (entity == null) {
			return new TokenResult(
					new AuthCheckResult(authConfig, false, "Auth configuration '" + authConfig + "' is not defined"),
					null);
		}

		try {
			OAuth2AuthorizedClient client = authorizeClient(authConfig, entity);
			if (client != null && client.getAccessToken() != null) {
				return new TokenResult(new AuthCheckResult(authConfig, true, null),
						client.getAccessToken().getTokenValue());
			}
			return new TokenResult(
					new AuthCheckResult(authConfig, false, "No access token returned by the identity provider"), null);
		}
		catch (Exception ex) {
			log.info("Token request failed for auth configuration '{}': {}", authConfig, ex.getMessage());
			return new TokenResult(new AuthCheckResult(authConfig, false, ex.getMessage()), null);
		}
	}

	private static OAuth2AuthorizedClient authorizeClient(String authConfig, AuthConfigEntity entity) {
		String scope = entity.getScope();
		ClientRegistration registration = ClientRegistration.withRegistrationId(authConfig)
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.clientId(entity.getClientId())
			.clientSecret(entity.getClientSecret())
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
			.scope((scope != null) ? scope.split(" ") : new String[0])
			.tokenUri(entity.getAccessTokenUrl())
			.build();

		var registrationRepository = new InMemoryClientRegistrationRepository(List.of(registration));
		var clientService = new InMemoryOAuth2AuthorizedClientService(registrationRepository);
		var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrationRepository, clientService);
		manager
			.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());

		OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest.withClientRegistrationId(authConfig)
			.principal(PRINCIPAL)
			.build();
		return manager.authorize(request);
	}

}
