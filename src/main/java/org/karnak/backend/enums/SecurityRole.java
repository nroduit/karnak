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
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Getter
public enum SecurityRole {

	ADMIN_ROLE("ROLE_admin", "admin"), INVESTIGATOR_ROLE("ROLE_investigator", "investigator"),
	USER_ROLE("ROLE_user", "user");

	private final String role;

	private final String type;

	SecurityRole(final String role, final String type) {
		this.role = role;
		this.type = type;
	}

	/**
	 * Get the enum from the role in parameter
	 * @param role Role of the enum
	 * @return SecurityRole found
	 */
	public static @Nullable SecurityRole fromCode(final String role) {
		if (role != null) {
			return Arrays.stream(SecurityRole.values())
				.filter(r -> role.trim().equalsIgnoreCase(r.getRole()))
				.findFirst()
				.orElse(null);
		}
		return null;
	}

	/**
	 * Get the enum from the type in parameter
	 * @param type Type of the enum
	 * @return SecurityRole found
	 */
	public static @Nullable SecurityRole fromType(final String type) {
		if (type != null) {
			return Arrays.stream(SecurityRole.values())
				.filter(r -> type.trim().equalsIgnoreCase(r.getType()))
				.findFirst()
				.orElse(null);
		}
		return null;
	}

	@Override
	public String toString() {
		return "SecurityRole{role='" + role + "', type='" + type + "'}";
	}

}
