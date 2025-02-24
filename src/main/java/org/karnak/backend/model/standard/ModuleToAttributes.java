/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.standard;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.karnak.backend.exception.ModuleNotFoundException;
import org.karnak.backend.model.dicominnolitics.StandardModuleToAttributes;
import org.karnak.backend.model.dicominnolitics.jsonModuleToAttribute;

public class ModuleToAttributes {

	/*
	 * <moduleID, <TagPath, Attribute>>
	 */
	private final Map<String, Map<String, ModuleAttribute>> HMapModuleAttributes;

	public ModuleToAttributes() {
		HMapModuleAttributes = initializeAttributes(StandardModuleToAttributes.readJsonModuleToAttributes());
	}

	private Map<String, Map<String, ModuleAttribute>> initializeAttributes(jsonModuleToAttribute[] moduleToAttributes) {
		Map<String, Map<String, ModuleAttribute>> HMapModuleAttributes = new HashMap<>();

		for (jsonModuleToAttribute moduleToAttribute : moduleToAttributes) {
			ModuleAttribute moduleAttribute = new ModuleAttribute(moduleToAttribute.getPath(),
					moduleToAttribute.getType(), moduleToAttribute.getModuleId());

			String moduleKey = moduleToAttribute.getModuleId();
			if (!HMapModuleAttributes.containsKey(moduleKey)) {
				HMapModuleAttributes.put(moduleKey, new HashMap<>());
			}
			HMapModuleAttributes.get(moduleKey).put(moduleAttribute.getTagPath(), moduleAttribute);
		}

		return HMapModuleAttributes;
	}

	public Map<String, ModuleAttribute> getAttributesByModule(String moduleID) {
		return HMapModuleAttributes.get(moduleID);
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
