/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import static org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.VR;
import org.karnak.backend.data.entity.AuthConfigEntity;
import org.karnak.backend.data.repo.AuthConfigRepo;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.expression.ExprAction;
import org.karnak.backend.model.expression.ExpressionError;
import org.karnak.backend.model.expression.ExpressionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Service
public class EndpointService {

	private AuthConfigRepo authConfigRepo;

	@Autowired
	public EndpointService(AuthConfigRepo authConfigRepo) {
		this.authConfigRepo = authConfigRepo;
	}

	OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository clientRegistrationRepository) {
		OAuth2AuthorizedClientService service = new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
		AuthorizedClientServiceOAuth2AuthorizedClientManager manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
				clientRegistrationRepository, service);
		OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
			.clientCredentials()
			.build();
		manager.setAuthorizedClientProvider(authorizedClientProvider);
		return manager;
	}

	RestClient restClient(OAuth2AuthorizedClientManager authorizedClientManager) {
		OAuth2ClientHttpRequestInterceptor requestInterceptor = new OAuth2ClientHttpRequestInterceptor(
				authorizedClientManager);
		return RestClient.builder().requestInterceptor(requestInterceptor).build();
	}

	public RestClient getAuthConfiguredRestClient(String authConfig) throws IllegalArgumentException {
		AuthConfigEntity ace = authConfigRepo.findByCode(authConfig);
		if (ace == null) {
			throw new IllegalArgumentException("Authentication Code " + authConfig + " is not defined");
		}
		String clientId = ace.getClientId();
		String clientSecret = ace.getClientSecret();
		String scope = ace.getScope();
		String tokenUrl = ace.getAccessTokenUrl();

		ClientRegistration registration = ClientRegistration.withRegistrationId(authConfig)
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.clientId(clientId)
			.clientSecret(clientSecret)
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
			.scope(scope.split(" "))
			.tokenUri(tokenUrl)
			.build();

		return restClient(authorizedClientManager(new InMemoryClientRegistrationRepository(List.of(registration))));
	}

	public RestClient getNoAuthRestClient() {
		return RestClient.builder().build();
	}

	@Cacheable(value = "endpoint.cache")
	public String get(String authConfig, String url) throws IllegalArgumentException, HttpClientErrorException {
		if (authConfig != null && !authConfig.isEmpty()) {
			return getAuthConfiguredRestClient(authConfig).get()
				.uri(url)
				.attributes(clientRegistrationId(authConfig))
				.retrieve()
				.body(String.class);
		}
		else {
			return get(url);
		}
	}

	@Cacheable(value = "endpoint.cache")
	public String post(String authConfig, String url, String body) {
		if (authConfig != null && !authConfig.isEmpty()) {
			return getAuthConfiguredRestClient(authConfig).post()
				.uri(url)
				.attributes(clientRegistrationId(authConfig))
				.body(body)
				.contentType(MediaType.APPLICATION_JSON)
				.retrieve()
				.body(String.class);
		}
		else {
			return post(url, body);
		}
	}

	@Cacheable(value = "endpoint.cache")
	public String get(String url) {
		return getNoAuthRestClient().get().uri(url).retrieve().body(String.class);

	}

	@Cacheable(value = "endpoint.cache")
	public String post(String url, String body) {
		return getNoAuthRestClient().post()
			.uri(url)
			.body(body)
			.contentType(MediaType.APPLICATION_JSON)
			.retrieve()
			.body(String.class);
	}

	public static String evaluateStringWithExpression(String url, Attributes dcm) {
		if (url != null && !url.isEmpty()) {
			Pattern p = Pattern.compile("\\{\\{([^\\}]+)\\}\\}");
			Matcher m = p.matcher(url);
			String replacedUrl = url;
			while (m.find()) {
				// The url contains parameters to replace
				String param = (String) ExpressionResult.get(m.group(1), new ExprAction(1, VR.AE, dcm, dcm),
						String.class);
				replacedUrl = replacedUrl.replaceFirst("\\{\\{[^\\}]+\\}\\}", param);
			}
			return replacedUrl;
		}
		return null;
	}

	public static String validateStringWithExpression(String url) {
		if (url != null && !url.isEmpty()) {
			Pattern p = Pattern.compile("\\{\\{([^\\}]+)\\}\\}");
			Matcher m = p.matcher(url);
			while (m.find()) {
				final ExpressionError expressionError = ExpressionResult.isValid(m.group(1),
						new ExprAction(1, VR.AE, new Attributes(), new Attributes()), ActionItem.class);
				if (!expressionError.isValid()) {
					return expressionError.getMsg();
				}
			}
		}
		return null;
	}

}
