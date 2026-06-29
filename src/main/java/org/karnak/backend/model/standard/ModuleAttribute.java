/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.standard;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Getter
public class ModuleAttribute {

	private static final List<String> STRICTER_TYPES = List.of("1", "1C", "2", "2C", "3");

	private final String moduleTagPath;

	private final String tagPath;

	private final String type;

	private final String moduleId;

	public ModuleAttribute(String moduleTagPath, String type, String moduleId) {
		this.moduleTagPath = moduleTagPath;
		this.type = type;
		this.moduleId = moduleId;
		this.tagPath = generateTagPath(moduleTagPath, moduleId);
	}

	public static @Nullable String getStricterType(List<ModuleAttribute> moduleAttributes) {
		for (String stricterType : STRICTER_TYPES) {
			if (moduleAttributes.stream().anyMatch(attribute -> stricterType.equals(attribute.getType()))) {
				return stricterType;
			}
		}
		return null;
	}

	private static String generateTagPath(String tagPath, String moduleId) {
		return Arrays.stream(tagPath.split(":"))
			.filter(value -> !value.equals(moduleId))
			.collect(Collectors.joining(":"));
	}

}
