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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.karnak.backend.exception.ModuleNotFoundException;
import org.karnak.backend.model.dicominnolitics.JsonModuleToAttribute;
import org.karnak.backend.model.dicominnolitics.StandardModuleToAttributes;

public class ModuleToAttributes {

	/*
	 * <moduleID, <TagPath, Attribute>>
	 */
	private final Map<String, Map<String, ModuleAttribute>> HMapModuleAttributes;

	public ModuleToAttributes() {
		HMapModuleAttributes = initializeAttributes(StandardModuleToAttributes.readJsonModuleToAttributes());
	}

	private Map<String, Map<String, ModuleAttribute>> initializeAttributes(JsonModuleToAttribute[] moduleToAttributes) {
		Map<String, Map<String, ModuleAttribute>> HMapModuleAttributes = new HashMap<>();

		for (JsonModuleToAttribute moduleToAttribute : moduleToAttributes) {
			ModuleAttribute moduleAttribute = new ModuleAttribute(moduleToAttribute.getPath(),
					moduleToAttribute.getType(), moduleToAttribute.getModuleId());
			HMapModuleAttributes.computeIfAbsent(moduleToAttribute.getModuleId(), k -> new HashMap<>())
				.put(moduleAttribute.getTagPath(), moduleAttribute);
		}

		return HMapModuleAttributes;
	}

	public @Nullable Map<String, ModuleAttribute> getAttributesByModule(String moduleID) {
		return HMapModuleAttributes.get(moduleID);
	}

	/** The identifiers of every known module, sorted alphabetically. */
	public List<String> getModuleIds() {
		return HMapModuleAttributes.keySet().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
	}

	public Map<String, ModuleAttribute> getModuleAttributesByType(String moduleID, String type)
			throws ModuleNotFoundException {
		Map<String, ModuleAttribute> attributes = HMapModuleAttributes.get(moduleID);
		if (attributes == null) {
			throw new ModuleNotFoundException(
					String.format("Unable to get module attributes. Could not find the module %s", moduleID));
		}
		return attributes.entrySet()
			.stream()
			.filter(entry -> type.equals(entry.getValue().getType()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

}
