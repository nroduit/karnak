/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.enums;

import java.util.Arrays;

public enum SecurityRole {

	// Role admin
	ADMIN_ROLE("ROLE_admin", "admin"),
	// Role Investigator
	INVESTIGATOR_ROLE("ROLE_investigator", "investigator"),
	// Role user
	USER_ROLE("ROLE_user", "user");

	/**
	 * Role of the enum
	 */
	private final String role;

	/**
	 * Type of the enum
	 */
	private final String type;

	/**
	 * Constructor
	 * @param role Role of the enum
	 * @param type Type of the enum
	 */
	SecurityRole(final String role, final String type) {
		this.role = role;
		this.type = type;
	}

	/**
	 * Get the enum from the role in parameter
	 * @param role Role of the enum
	 * @return SecurityRole found
	 */
	public static SecurityRole fromCode(final String role) {
		if (role != null) {
			return Arrays.stream(SecurityRole.values())
				.filter(r -> role.trim().equalsIgnoreCase(r.getRole()))
				.findFirst()
				.orElse(null);
		}
		return null;
	}

	/**
	 * Getter for code
	 * @return Role of the enum
	 */
	public String getRole() {
		return role;
	}

	/**
	 * Getter for type
	 * @return Type of the enum
	 */
	public String getType() {
		return type;
	}

	@Override
	public String toString() {
		return "SecurityRole{" + "role='" + role + '\'' + "type='" + type + '\'' + '}';
	}

}
