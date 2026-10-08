/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import com.vaadin.flow.server.VaadinServletService;
import jakarta.servlet.ServletException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.karnak.backend.enums.SecurityRole;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public final class SecurityUtil {

	private SecurityUtil() {
	}

	/**
	 * Checks if the current user is logged in
	 * @return true if the current user is logged in
	 */
	public static boolean isUserLoggedIn() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && !(authentication instanceof AnonymousAuthenticationToken)
				&& authentication.isAuthenticated();
	}

	/**
	 * Checks if the user is logged and is an admin
	 * @return true if the user is logged and is an admin
	 */
	public static boolean isUserAdmin() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return isUserLoggedIn() && authentication != null
				&& authentication.getAuthorities()
					.stream()
					.anyMatch(ga -> Objects.equals(ga.getAuthority(), SecurityRole.ADMIN_ROLE.getRole()));
	}

	/**
	 * Sign out method
	 */
	public static void signOut() {
		var request = VaadinServletService.getCurrentServletRequest();
		if (request == null) {
			log.warn("Cannot sign out: no current servlet request available");
			return;
		}
		try {
			request.logout();
		}
		catch (ServletException e) {
			log.error("Error during logout");
		}
	}

}
