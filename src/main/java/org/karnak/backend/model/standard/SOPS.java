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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.karnak.backend.exception.SOPNotFoundException;
import org.karnak.backend.model.dicominnolitics.StandardCIODS;
import org.karnak.backend.model.dicominnolitics.StandardCIODtoModules;
import org.karnak.backend.model.dicominnolitics.StandardSOPS;
import org.karnak.backend.model.dicominnolitics.jsonCIOD;
import org.karnak.backend.model.dicominnolitics.jsonCIODtoModule;
import org.karnak.backend.model.dicominnolitics.jsonSOP;

public class SOPS {

	private static HashMap<String, SOP> HMapSOPS;

	public SOPS() {
		HashMap<String, String> mapCIODS = initializeCIODS(StandardCIODS.readJsonCIODS());
		HashMap<String, ArrayList<Module>> mapCIODModules = initializeHMapCIODModules(
				StandardCIODtoModules.readJsonCIODToModules());
		HMapSOPS = initializeSOPS(StandardSOPS.readJsonSOPS(), mapCIODS, mapCIODModules);
	}

	private HashMap<String, SOP> initializeSOPS(jsonSOP[] sops, HashMap<String, String> mapCIODS,
			HashMap<String, ArrayList<Module>> mapCIODModules) {
		HashMap<String, SOP> mapSOPS = new HashMap<>();
		for (jsonSOP sop : sops) {
			String ciodId = mapCIODS.get(sop.getCiod());
			ArrayList<Module> modules = mapCIODModules.get(ciodId);
			SOP newSOP = new SOP(sop.getId(), sop.getName(), sop.getCiod(), ciodId, modules);
			mapSOPS.put(sop.getId(), newSOP);
		}
		return mapSOPS;
	}

	private HashMap<String, String> initializeCIODS(jsonCIOD[] ciods) {
		HashMap<String, String> mapCIODS = new HashMap<>();
		for (jsonCIOD ciod : ciods) {
			mapCIODS.put(ciod.getName(), ciod.getId());
		}
		return mapCIODS;
	}

	private HashMap<String, ArrayList<Module>> initializeHMapCIODModules(jsonCIODtoModule[] ciodToModules) {
		HashMap<String, ArrayList<Module>> mapCIODModules = new HashMap<>();

		for (jsonCIODtoModule ciodToModule : ciodToModules) {
			String ciodId = ciodToModule.getCiodId();
			Module module = new Module(ciodToModule.getModuleId(), ciodToModule.getUsage(),
					ciodToModule.getInformationEntity());
			if (mapCIODModules.containsKey(ciodId)) {
				ArrayList<Module> modules = mapCIODModules.get(ciodId);
				modules.add(module);
			}
			else {
				ArrayList<Module> modules = new ArrayList<>();
				modules.add(module);
				mapCIODModules.put(ciodId, modules);
			}
		}
		return mapCIODModules;
	}

	public List<String> getAllUIDs() {
		return new ArrayList<>(HMapSOPS.keySet());
	}

	public SOP getSOP(String uid) {
		return HMapSOPS.get(uid);
	}

	public String getName(String uid) throws SOPNotFoundException {
		SOP sop = HMapSOPS.get(uid);
		if (sop == null) {
			throw new SOPNotFoundException(String.format("Unable to get name. Could not find the SOP UID %s", uid));
		}
		return sop.getName();
	}

	public String getCIOD(String uid) throws SOPNotFoundException {
		SOP sop = HMapSOPS.get(uid);
		if (sop == null) {
			throw new SOPNotFoundException(String.format("Unable to get CIOD. Could not find the SOP UID %s", uid));
		}
		return sop.getCiod();
	}

	public String getIdCIOD(String uid) throws SOPNotFoundException {
		SOP sop = HMapSOPS.get(uid);
		if (sop == null) {
			throw new SOPNotFoundException(String.format("Unable to get ID CIOD. Could not find the SOP UID %s", uid));
		}
		return sop.getCiod_id();
	}

	public Optional<Module> getModuleByModuleID(String uid, String moduleId) throws SOPNotFoundException {
		SOP sop = HMapSOPS.get(uid);
		if (sop == null) {
			throw new SOPNotFoundException(String
				.format("Unable to get if module %s is present. Could not find the SOP UID %s", moduleId, uid));
		}
		return sop.getModules().stream().filter(module -> moduleId.equals(module.getId())).findFirst();
	}

	public List<Module> getSopModules(String uid) throws SOPNotFoundException {
		SOP sop = HMapSOPS.get(uid);
		if (sop == null) {
			throw new SOPNotFoundException(
					String.format("Unable to get SOP modules. Could not find the SOP UID %s", uid));
		}
		return sop.getModules();
	}

	public List<String> getSopModulesName(String uid) throws SOPNotFoundException {
		SOP sop = HMapSOPS.get(uid);
		if (sop == null) {
			throw new SOPNotFoundException(
					String.format("Unable to get SOP modules name. Could not find the SOP UID %s", uid));
		}
		return sop.getModules().stream().map(Module::getId).collect(Collectors.toList());
	}

	public Boolean moduleIsPresent(String uid, String moduleId) throws SOPNotFoundException {
		SOP sop = HMapSOPS.get(uid);
		if (sop == null) {
			throw new SOPNotFoundException(String
				.format("Unable to get if module %s is present. Could not find the SOP UID %s", moduleId, uid));
		}
		Predicate<Module> modulePredicate = module -> moduleId.equals(module.getId());
		return sop.getModules().stream().anyMatch(modulePredicate);
	}

	public Map<Module, Map<String, ModuleAttribute>> getModuleToAttribute(String uid,
			ModuleToAttributes moduleToAttributes) throws SOPNotFoundException {
		Map<Module, Map<String, ModuleAttribute>> mapModuleAttributes = new HashMap<>();
		try {
			getSopModules(uid).forEach(module -> mapModuleAttributes.put(module,
					moduleToAttributes.getAttributesByModule(module.getId())));
		}
		catch (SOPNotFoundException sopNotFoundException) {
			throw new SOPNotFoundException(
					String.format("Unable to get module attributes. Could not find the SOP UID %s", uid));
		}
		return mapModuleAttributes;
	}

}
