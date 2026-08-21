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

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import java.util.List;
import org.karnak.backend.constant.EndPoint;
import org.karnak.backend.enums.SecurityRole;
import org.karnak.backend.security.DefaultIdpLoadCondition;
import org.karnak.frontend.authentication.LoginScreen;
import org.springframework.boot.actuate.context.ShutdownEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.weasis.core.util.annotations.Generated;

@EnableWebSecurity
@Configuration
@Conditional(value = DefaultIdpLoadCondition.class)
@Generated
public class SecurityInMemoryConfig {

	private static final String LOGIN_URL = "/login";

	// Fine-grained authorities
	private static final String API_READ_AUTHORITY = "karnak_read";

	private static final String API_SEARCH_AUTHORITY = "karnak_search";

	private static final String API_CREATE_AUTHORITY = "karnak_create";

	private static final String API_UPDATE_AUTHORITY = "karnak_update";

	private static final String API_DELETE_AUTHORITY = "karnak_delete";

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
				.permitAll()
				// Api endpoints
				.requestMatchers(EndPoint.API_PATH + EndPoint.ALL_REMAINING_PATH)
				.authenticated())
				.csrf(csrf -> csrf.ignoringRequestMatchers(EndPoint.API_PATH
						+ EndPoint.ALL_REMAINING_PATH))
				.httpBasic(Customizer.withDefaults())
			// Vaadin/Spring Security integration: permits the framework internal
			// requests and the @AnonymousAllowed views, scopes CSRF, configures the
			// request cache, the form login on the login view and requires
			// authentication for any other request. It also enables the navigation
			// access control which enforces the @RolesAllowed annotations of the views.
			.with(VaadinSecurityConfigurer.vaadin(), vaadin -> vaadin.loginView(LoginScreen.class, LOGIN_URL));

		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		// Configure users and roles in memory.
		// In addition to the navigation roles (used by the @RolesAllowed
		// annotations of the Vaadin views), the admin user is granted the
		// fine-grained authorities required by the @PreAuthorize annotations
		// of the REST API controllers.
		List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(SecurityRole.ADMIN_ROLE.getRole(),
				SecurityRole.INVESTIGATOR_ROLE.getRole(), SecurityRole.USER_ROLE.getRole(), API_READ_AUTHORITY,
				API_CREATE_AUTHORITY, API_UPDATE_AUTHORITY, API_DELETE_AUTHORITY, API_SEARCH_AUTHORITY);

		UserDetails userDetails = User.builder()
			.username(AppConfig.getInstance().getKarnakAdmin())
			.password("{noop}" + AppConfig.getInstance().getKarnakPassword())
			.authorities(authorities)
			.build();

		return new InMemoryUserDetailsManager(userDetails);
	}

}
