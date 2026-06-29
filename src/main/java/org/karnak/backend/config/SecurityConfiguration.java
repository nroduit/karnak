/*
 * Copyright (c) 2024-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.config;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.karnak.backend.security.OidcRoleAuthoritiesMapper;
import org.karnak.backend.security.OpenIdConnectLogoutHandler;
import org.springframework.boot.actuate.context.ShutdownEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;
import org.weasis.core.util.annotations.Generated;

@EnableWebSecurity
@Configuration
@ConditionalOnProperty(value = "IDP", havingValue = "oidc")
@Generated
public class SecurityConfiguration {

	// Spring Security default authorization request URL of the "keycloak" client
	// registration (see application-oidc.yml)
	private static final String OAUTH2_LOGIN_PAGE = "/oauth2/authorization/keycloak";

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// Turns on/off authorizations
			.authorizeHttpRequests(authorize -> authorize
				// Application static resources - no authentication required
				// (/sw.js is the tombstone service worker that unregisters any stale
				// PWA worker left over from older builds)
				.requestMatchers("/img/**", "/sw.js")
				.permitAll()
				// Deny the shutdown endpoint before permitting the other actuator
				// endpoints
				.requestMatchers(EndpointRequest.to(ShutdownEndpoint.class))
				.denyAll()
				// Actuator, health, info
				.requestMatchers("/actuator/**")
				.permitAll()
				.requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class))
				.permitAll()
				// Allow endpoints
				.requestMatchers(HttpMethod.GET, "/api/echo/destinations")
				.permitAll())
			// OpenId connect login: map the IDP realm/client roles to the Karnak roles
			// so that @RolesAllowed annotations on the views work with OIDC users. The
			// login page points to the authorization URL so that unauthenticated users
			// are sent directly to the IDP instead of the generated OAuth2 login page.
			.oauth2Login(oauth2 -> oauth2.loginPage(OAUTH2_LOGIN_PAGE)
				.userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(new OidcRoleAuthoritiesMapper())))
			// Vaadin/Spring Security integration: permits the framework internal
			// requests and the @AnonymousAllowed views, scopes CSRF, configures the
			// request cache, redirects unauthenticated users to the IDP and requires
			// authentication for any other request. Logout is propagated to the IDP.
			// It also enables the navigation access control which enforces the
			// @RolesAllowed annotations of the views.
			.with(VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin.oauth2LoginPage(OAUTH2_LOGIN_PAGE)
				.addLogoutHandler(new OpenIdConnectLogoutHandler()));

		return http.build();
	}

	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {
		// Access to static resources, bypassing Spring security.
		return web -> web.ignoring()
			.requestMatchers("/VAADIN/**", "/img/**", "/sw.js", "/favicon.ico", "/manifest.webmanifest",
					"/offline.html", "/sw-runtime-resources-precache.js");
	}

}