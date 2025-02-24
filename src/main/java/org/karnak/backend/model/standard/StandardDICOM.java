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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.dcm4che3.util.TagUtils;
import org.karnak.backend.exception.ModuleNotFoundException;
import org.karnak.backend.exception.SOPNotFoundException;

public class StandardDICOM {

	private final SOPS sops;

	private final ModuleToAttributes moduleToAttributes;

	private final AttributeDetails attributeDetails;

	public StandardDICOM() {
		sops = new SOPS();
		moduleToAttributes = new ModuleToAttributes();
		attributeDetails = new AttributeDetails();
	}

	public static String cleanTagPath(String tagPath) {
		return tagPath.replaceAll("[(),]", "").toLowerCase();
	}

	public List<String> getAllSOPuids() {
		return sops.getAllUIDs();
	}

	public String getCIOD(String sopUID) throws SOPNotFoundException {
		return sops.getCIOD(sopUID);
	}

	public String getIdCIOD(String sopUID) throws SOPNotFoundException {
		return sops.getIdCIOD(sopUID);
	}

	public Optional<Module> getModuleByModuleID(String sopUID, String moduleId) throws SOPNotFoundException {
		return sops.getModuleByModuleID(sopUID, moduleId);
	}

	public boolean moduleIsPresent(String sopUID, String moduleId) throws SOPNotFoundException {
		return sops.moduleIsPresent(sopUID, moduleId);
	}

	public Map<Module, Map<String, ModuleAttribute>> getModulesBySOP(String sopUID) throws SOPNotFoundException {
		return sops.getModuleToAttribute(sopUID, moduleToAttributes);
	}

	public List<String> getModulesNameBySOP(String sopUID) throws SOPNotFoundException {
		return sops.getSopModulesName(sopUID);
	}

	public List<ModuleAttribute> getAttributesBySOP(String sopUID, int tagPath) throws SOPNotFoundException {
		return getAttributesBySOP(sopUID, TagUtils.toHexString(tagPath));
	}

	public List<ModuleAttribute> getAttributesBySOP(String sopUID, String tagPath) throws SOPNotFoundException {
		String tagPathCleaned = cleanTagPath(tagPath);
		Map<Module, Map<String, ModuleAttribute>> mapModuleAttributes = getModulesBySOP(sopUID);
		List<ModuleAttribute> moduleAttributes = new ArrayList<>();
		mapModuleAttributes.forEach((module, attr) -> {
			ModuleAttribute moduleAttribute = attr.get(tagPathCleaned);
			if (moduleAttribute != null) {
				moduleAttributes.add(moduleAttribute);
			}
		});
		return moduleAttributes;
	}

	public Map<String, ModuleAttribute> getAttributesByModule(String moduleId) {
		return moduleToAttributes.getAttributesByModule(moduleId);
	}

	public List<ModuleAttribute> getAttributeListByModule(String moduleId) {
		return new ArrayList<>(moduleToAttributes.getAttributesByModule(moduleId).values());
	}

	public Map<String, ModuleAttribute> getModuleAttributesByType(String moduleId, String type)
			throws ModuleNotFoundException {
		return moduleToAttributes.getModuleAttributesByType(moduleId, type);
	}

	public AttributeDetail getAttributeDetail(String tag) {
		return attributeDetails.getAttributeDetail(tag);
	}

	public List<AttributeDetail> getListAttributeDetail(List<String> tag) {
		return attributeDetails.getListAttributeDetail(tag);
	}

}
