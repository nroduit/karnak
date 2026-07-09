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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.karnak.backend.enums.SecurityRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Maps the roles carried by the OpenID Connect provider's Bearer/access token to the
 * Karnak security roles ({@link SecurityRole}).
 *
 * <p>
 * Roles are read exclusively from the Keycloak claim "resource_access.karnak.roles" of
 * the decoded <b>access token (Bearer token)</b>, not from the ID token. Per OAuth2/OIDC
 * semantics, the ID token authenticates the user to the client (its audience is the
 * client id) while the access token is the credential presented to resource servers and
 * is the standard place for authorization data. Its presence in the access token does
 * not depend on an "Add to ID token" toggle in the IDP client scope configuration,
 * unlike the ID token, whose content can legitimately vary between environments
 * (e.g. present in a dev realm, absent in a cert realm) without this being a
 * misconfiguration. Only the roles matching a {@link SecurityRole} type (admin,
 * investigator, user) are mapped, as "ROLE_"-prefixed granted authorities. The IDP must
 * therefore be configured to include the roles of the "karnak" client in the access
 * token (in Keycloak: client scope "roles", mapper "Add to access token" enabled).
 */
@Component
@Slf4j
public class OidcRoleAuthoritiesMapper {

	private static final String RESOURCE_ACCESS_CLAIM = "resource_access";

	private static final String KARNAK_CLIENT = "karnak";

	private static final String ROLES_CLAIM = "roles";

	/**
	 * Maps the roles of the "karnak" client found in the decoded access token to known
	 * Karnak granted authorities.
	 * @param accessToken the decoded Bearer/access token
	 * @return the granted authorities matching a known {@link SecurityRole}
	 */
	@NonNull
	public Set<GrantedAuthority> mapAuthorities(Jwt accessToken) {
		Set<String> roleNames = extractRoleNames(accessToken.getClaims());
		Set<GrantedAuthority> mappedAuthorities = roleNames.stream()
			.map(SecurityRole::fromType)
			.filter(Objects::nonNull)
			.map(securityRole -> new SimpleGrantedAuthority(securityRole.getRole()))
			.collect(Collectors.toCollection(HashSet::new));
		Set<String> unknownRoleNames = roleNames.stream()
			.filter(roleName -> SecurityRole.fromType(roleName) == null)
			.collect(Collectors.toSet());
		if (!unknownRoleNames.isEmpty()) {
			log.warn(
					"OIDC (access token): role(s) {} on the \"{}\" client do not match any known Karnak role "
							+ "(expected one of: admin, investigator, user) and are ignored.",
					unknownRoleNames, KARNAK_CLIENT);
		}
		return mappedAuthorities;
	}

	/**
	 * Returns the role names of the "karnak" entry of the "resource_access" claim of
	 * the access token.
	 */
	private static Set<String> extractRoleNames(Map<String, Object> claims) {
		Object resourceAccessClaim = claims.get(RESOURCE_ACCESS_CLAIM);
		if (!(resourceAccessClaim instanceof Map<?, ?> resourceAccess)) {
			log.warn(
					"OIDC (access token): no \"{}\" claim found. The IDP must include the client roles of the "
							+ "\"{}\" client in the access token (Keycloak: client scope \"roles\", mapper "
							+ "\"Add to access token\" enabled).",
					RESOURCE_ACCESS_CLAIM, KARNAK_CLIENT);
			return Set.of();
		}
		if (!resourceAccess.containsKey(KARNAK_CLIENT)) {
			log.warn(
					"OIDC (access token): no entry \"{}\" found in the \"{}\" claim (found clients: {}). Check "
							+ "that the OIDC client id configured in Keycloak is exactly \"{}\".",
					KARNAK_CLIENT, RESOURCE_ACCESS_CLAIM, resourceAccess.keySet(), KARNAK_CLIENT);
		}
		if (resourceAccess.get(KARNAK_CLIENT) instanceof Map<?, ?> resource
				&& resource.get(ROLES_CLAIM) instanceof Collection<?> roles) {
			return roles.stream()
				.filter(String.class::isInstance)
				.map(String.class::cast)
				.collect(Collectors.toSet());
		}
		return Set.of();
	}

}


