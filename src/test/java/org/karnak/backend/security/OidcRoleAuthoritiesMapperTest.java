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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

@DisplayNameGeneration(ReplaceUnderscores.class)
class OidcRoleAuthoritiesMapperTest {

	private final OidcRoleAuthoritiesMapper mapper = new OidcRoleAuthoritiesMapper();

	@Test
	void should_map_karnak_client_roles_of_access_token_to_karnak_roles() {
		// Init data
		Jwt accessToken = jwtWithClaims(Map.of("resource_access",
				Map.of("karnak", Map.of("roles", List.of("admin", "investigator", "offline_access")))));

		// Call service
		Set<GrantedAuthority> mapped = mapper.mapAuthorities(accessToken);

		// Test results
		assertTrue(containsAuthority(mapped, "ROLE_admin"));
		assertTrue(containsAuthority(mapped, "ROLE_investigator"));
		// Roles unknown to Karnak are not mapped
		assertEquals(2, mapped.size());
	}

	@Test
	void when_no_resource_access_claim_should_return_no_authorities() {
		// Init data
		Jwt accessToken = jwtWithClaims(Map.of());

		// Call service
		Set<GrantedAuthority> mapped = mapper.mapAuthorities(accessToken);

		// Test results
		assertTrue(mapped.isEmpty());
	}

	@Test
	void when_no_role_claim_should_not_add_authorities() {
		// Init data
		Jwt accessToken = jwtWithClaims(Map.of("resource_access", Map.of()));

		// Call service
		Set<GrantedAuthority> mapped = mapper.mapAuthorities(accessToken);

		// Test results
		assertTrue(mapped.isEmpty());
	}

	@Test
	void should_only_map_karnak_client_roles_ignoring_realm_and_other_clients() {
		// Init data
		Jwt accessToken = jwtWithClaims(Map.of("realm_access", Map.of("roles", List.of("admin")), "resource_access", Map
			.of("karnak", Map.of("roles", List.of("user")), "other-client", Map.of("roles", List.of("investigator")))));

		// Call service
		Set<GrantedAuthority> mapped = mapper.mapAuthorities(accessToken);

		// Test results
		assertTrue(containsAuthority(mapped, "ROLE_user"));
		assertEquals(1, mapped.size());
	}

	@Test
	void should_ignore_non_string_role_entries() {
		// Init data
		Jwt accessToken = jwtWithClaims(
				Map.of("resource_access", Map.of("karnak", Map.of("roles", List.of("admin", 42, true)))));

		// Call service
		Set<GrantedAuthority> mapped = mapper.mapAuthorities(accessToken);

		// Test results
		assertTrue(containsAuthority(mapped, "ROLE_admin"));
		assertEquals(1, mapped.size());
	}

	@Test
	void should_ignore_a_malformed_resource_access_claim() {
		// Init data
		Jwt accessToken = jwtWithClaims(Map.of("resource_access", "not-a-map"));

		// Call service
		Set<GrantedAuthority> mapped = mapper.mapAuthorities(accessToken);

		// Test results
		assertTrue(mapped.isEmpty());
	}

	@Test
	void should_ignore_a_malformed_karnak_client_entry() {
		// Init data
		Jwt accessToken = jwtWithClaims(Map.of("resource_access", Map.of("karnak", "not-a-map")));

		// Call service
		Set<GrantedAuthority> mapped = mapper.mapAuthorities(accessToken);

		// Test results
		assertTrue(mapped.isEmpty());
	}

	private static Jwt jwtWithClaims(Map<String, Object> claims) {
		Jwt.Builder builder = Jwt.withTokenValue("token")
			.header("alg", "none")
			.subject("user-id")
			.issuedAt(Instant.now())
			.expiresAt(Instant.now().plusSeconds(60));
		claims.forEach(builder::claim);
		return builder.build();
	}

	private static boolean containsAuthority(Set<GrantedAuthority> authorities, String authority) {
		return authorities.stream().anyMatch(ga -> ga.getAuthority().equals(authority));
	}

}
