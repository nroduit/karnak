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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.enums.SecurityRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;

/**
 * Maps the roles provided by the OpenID Connect identity provider to the Karnak security
 * roles ({@link SecurityRole}).
 *
 * <p>
 * Roles are read exclusively from the Keycloak claim "resource_access.karnak.roles" of
 * the ID token and the userinfo endpoint. Only the roles matching a {@link SecurityRole}
 * type (admin, investigator, user) are mapped, as "ROLE_"-prefixed granted authorities.
 * The IDP must therefore be configured to include the roles of the "karnak" client in the
 * ID token or in the userinfo response (in Keycloak: client scope "roles").
 */
public class OidcRoleAuthoritiesMapper implements GrantedAuthoritiesMapper {

	private static final String RESOURCE_ACCESS_CLAIM = "resource_access";

	private static final String KARNAK_CLIENT = "karnak";

	private static final String ROLES_CLAIM = "roles";

	@Override
	@NonNull public Collection<? extends GrantedAuthority> mapAuthorities(
			@NonNull Collection<? extends GrantedAuthority> authorities) {
		Set<GrantedAuthority> mappedAuthorities = new HashSet<>(authorities);
		for (GrantedAuthority authority : authorities) {
			if (authority instanceof OidcUserAuthority oidcUserAuthority) {
				mappedAuthorities.addAll(extractRoles(oidcUserAuthority.getIdToken().getClaims()));
				if (oidcUserAuthority.getUserInfo() != null) {
					mappedAuthorities.addAll(extractRoles(oidcUserAuthority.getUserInfo().getClaims()));
				}
			}
			else if (authority instanceof OAuth2UserAuthority oauth2UserAuthority) {
				mappedAuthorities.addAll(extractRoles(oauth2UserAuthority.getAttributes()));
			}
		}
		return mappedAuthorities;
	}

	/**
	 * Maps the roles of the "karnak" client found in the claims to known Karnak
	 * authorities.
	 */
	private static Set<GrantedAuthority> extractRoles(Map<String, Object> claims) {
		Set<String> roleNames = new HashSet<>();
		if (claims.get(RESOURCE_ACCESS_CLAIM) instanceof Map<?, ?> resourceAccess) {
			roleNames.addAll(retrieveRoles(resourceAccess.get(KARNAK_CLIENT)));
		}
		return roleNames.stream()
			.map(SecurityRole::fromType)
			.filter(Objects::nonNull)
			.map(securityRole -> new SimpleGrantedAuthority(securityRole.getRole()))
			.collect(Collectors.toSet());
	}

	/**
	 * Returns the role names of the "karnak" entry of the "resource_access" claim.
	 */
	private static Collection<String> retrieveRoles(@Nullable Object accessClaim) {
		if (accessClaim instanceof Map<?, ?> access && access.get(ROLES_CLAIM) instanceof Collection<?> roles) {
			return roles.stream().filter(String.class::isInstance).map(String.class::cast).toList();
		}
		return List.of();
	}

}
