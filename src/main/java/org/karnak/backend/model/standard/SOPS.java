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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.karnak.backend.exception.SOPNotFoundException;
import org.karnak.backend.model.dicominnolitics.JsonCIOD;
import org.karnak.backend.model.dicominnolitics.JsonCIODtoModule;
import org.karnak.backend.model.dicominnolitics.JsonSOP;
import org.karnak.backend.model.dicominnolitics.StandardCIODS;
import org.karnak.backend.model.dicominnolitics.StandardCIODtoModules;
import org.karnak.backend.model.dicominnolitics.StandardSOPS;

public class SOPS {

	private final Map<String, SOP> sopByUid;

	public SOPS() {
		HashMap<String, String> mapCIODS = initializeCIODS(StandardCIODS.readJsonCIODS());
		HashMap<String, ArrayList<Module>> mapCIODModules = initializeHMapCIODModules(
				StandardCIODtoModules.readJsonCIODToModules());
		sopByUid = initializeSOPS(StandardSOPS.readJsonSOPS(), mapCIODS, mapCIODModules);
	}

	private HashMap<String, SOP> initializeSOPS(JsonSOP[] sops, HashMap<String, String> mapCIODS,
			HashMap<String, ArrayList<Module>> mapCIODModules) {
		HashMap<String, SOP> mapSOPS = new HashMap<>();
		for (JsonSOP sop : sops) {
			String ciodId = mapCIODS.get(sop.getCiod());
			ArrayList<Module> modules = mapCIODModules.get(ciodId);
			SOP newSOP = new SOP(sop.getId(), sop.getName(), sop.getCiod(), ciodId, modules);
			mapSOPS.put(sop.getId(), newSOP);
		}
		return mapSOPS;
	}

	private HashMap<String, String> initializeCIODS(JsonCIOD[] ciods) {
		HashMap<String, String> mapCIODS = new HashMap<>();
		for (JsonCIOD ciod : ciods) {
			mapCIODS.put(ciod.getName(), ciod.getId());
		}
		return mapCIODS;
	}

	private HashMap<String, ArrayList<Module>> initializeHMapCIODModules(JsonCIODtoModule[] ciodToModules) {
		HashMap<String, ArrayList<Module>> mapCIODModules = new HashMap<>();
		for (JsonCIODtoModule ciodToModule : ciodToModules) {
			Module module = new Module(ciodToModule.getModuleId(), ciodToModule.getUsage(),
					ciodToModule.getInformationEntity());
			mapCIODModules.computeIfAbsent(ciodToModule.getCiodId(), k -> new ArrayList<>()).add(module);
		}
		return mapCIODModules;
	}

	// Look up the SOP or fail with a message describing the attempted action.
	private SOP requireSop(String uid, String action) throws SOPNotFoundException {
		SOP sop = sopByUid.get(uid);
		if (sop == null) {
			throw new SOPNotFoundException(
					String.format("Unable to get %s. Could not find the SOP UID %s", action, uid));
		}
		return sop;
	}

	public List<String> getAllUIDs() {
		return new ArrayList<>(sopByUid.keySet());
	}

	public @Nullable SOP getSOP(String uid) {
		return sopByUid.get(uid);
	}

	public String getName(String uid) throws SOPNotFoundException {
		return requireSop(uid, "name").getName();
	}

	public String getCIOD(String uid) throws SOPNotFoundException {
		return requireSop(uid, "CIOD").getCiod();
	}

	public String getIdCIOD(String uid) throws SOPNotFoundException {
		return requireSop(uid, "ID CIOD").getCiod_id();
	}

	public Optional<Module> getModuleByModuleID(String uid, String moduleId) throws SOPNotFoundException {
		return requireSop(uid, "if module " + moduleId + " is present").getModules()
			.stream()
			.filter(module -> moduleId.equals(module.id()))
			.findFirst();
	}

	public List<Module> getSopModules(String uid) throws SOPNotFoundException {
		return requireSop(uid, "SOP modules").getModules();
	}

	public List<String> getSopModulesName(String uid) throws SOPNotFoundException {
		return requireSop(uid, "SOP modules name").getModules().stream().map(Module::id).toList();
	}

	public Boolean moduleIsPresent(String uid, String moduleId) throws SOPNotFoundException {
		return requireSop(uid, "if module " + moduleId + " is present").getModules()
			.stream()
			.anyMatch(module -> moduleId.equals(module.id()));
	}

	public Map<Module, Map<String, ModuleAttribute>> getModuleToAttribute(String uid,
			ModuleToAttributes moduleToAttributes) throws SOPNotFoundException {
		Map<Module, Map<String, ModuleAttribute>> mapModuleAttributes = new HashMap<>();
		requireSop(uid, "module attributes").getModules()
			.forEach(module -> mapModuleAttributes.put(module, moduleToAttributes.getAttributesByModule(module.id())));
		return mapModuleAttributes;
	}

}
