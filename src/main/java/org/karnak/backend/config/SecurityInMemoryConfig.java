/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.config;

import org.karnak.backend.cache.RequestCache;
import org.karnak.backend.constant.EndPoint;
import org.karnak.backend.enums.SecurityRole;
import org.karnak.backend.security.DefaultIdpLoadCondition;
import org.karnak.backend.util.SecurityUtil;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.context.ShutdownEndpoint;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
@Conditional(value = DefaultIdpLoadCondition.class)
public class SecurityInMemoryConfig {

	private static final String LOGIN_FAILURE_URL = "/login?error";

	private static final String LOGIN_URL = "/login";

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			// Disables cross-site request forgery (CSRF) protection for main route
			.csrf(csrf -> csrf.ignoringRequestMatchers(EndPoint.ALL_REMAINING_PATH))
			// Turns on/off authorizations
			.authorizeHttpRequests(authorize -> authorize
				// Static resources - no authentication required
				.requestMatchers("/VAADIN/**", "/img/**", "/icons/**", "/sw.js", "/favicon.ico",
						"/manifest.webmanifest", "/offline.html", "/sw-runtime-resources-precache.js")
				.permitAll()
				// Actuator, health, info
				.requestMatchers("/actuator/**")
				.permitAll()
				.requestMatchers(EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class))
				.permitAll()
				// Allows all internal traffic from the Vaadin framework
				.requestMatchers(SecurityUtil::isFrameworkInternalRequest)
				.permitAll()
				// Allow endpoints
				.requestMatchers(HttpMethod.GET, "/api/echo/destinations")
				.permitAll()
				// Deny
				.requestMatchers(EndpointRequest.to(ShutdownEndpoint.class))
				.denyAll()
				// Allows all authenticated traffic
				// Allow admin role
				.requestMatchers("/*")
				.hasRole(SecurityRole.ADMIN_ROLE.getType())
				.anyRequest()
				.authenticated())
			// Enables form-based login and permits unauthenticated access to it
			// Configures the login page URLs
			.formLogin(formLogin -> formLogin.loginPage(LOGIN_URL)
				.permitAll()
				.loginProcessingUrl(LOGIN_URL)
				.failureUrl(LOGIN_FAILURE_URL))
			// Configures the logout URL
			.logout(logout -> logout.logoutSuccessUrl(LOGIN_URL))
			.exceptionHandling(exceptionHandling -> exceptionHandling.accessDeniedPage(LOGIN_URL));

		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
			throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

	@Bean
	public UserDetailsService userDetailsService() {
		// Configure users and roles in memory
		UserDetails userDetails = User.builder()
			.username(AppConfig.getInstance().getKarnakAdmin())
			.password("{noop}" + AppConfig.getInstance().getKarnakPassword())
			.roles(SecurityRole.ADMIN_ROLE.getType(), SecurityRole.INVESTIGATOR_ROLE.getType(),
					SecurityRole.USER_ROLE.getType())
			.build();

		return new InMemoryUserDetailsManager(userDetails);
	}

	@Bean
	public RequestCache requestCache() { //
		return new RequestCache();
	}

}
