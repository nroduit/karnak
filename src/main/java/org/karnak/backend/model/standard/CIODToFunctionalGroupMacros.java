/*
 * Copyright (c) 2026 Karnak Team and other contributors.
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
import org.karnak.backend.model.dicominnolitics.JsonCIODtoFuncGroupMacro;
import org.karnak.backend.model.dicominnolitics.StandardCIODtoFuncGroupMacros;

public class CIODToFunctionalGroupMacros {

	/*
	 * <ciodId, macros>
	 */
	private final Map<String, List<FunctionalGroupMacro>> macrosByCiod;

	public CIODToFunctionalGroupMacros() {
		macrosByCiod = initialize(StandardCIODtoFuncGroupMacros.readJsonCIODToFuncGroupMacros());
	}

	private static Map<String, List<FunctionalGroupMacro>> initialize(JsonCIODtoFuncGroupMacro[] ciodToMacros) {
		Map<String, List<FunctionalGroupMacro>> map = new HashMap<>();
		for (JsonCIODtoFuncGroupMacro ciodToMacro : ciodToMacros) {
			map.computeIfAbsent(ciodToMacro.getCiodId(), k -> new ArrayList<>())
				.add(new FunctionalGroupMacro(ciodToMacro.getMacroId(), ciodToMacro.getUsage(),
						ciodToMacro.getConditionalStatement()));
		}
		map.replaceAll((ciodId, macros) -> List.copyOf(macros));
		return map;
	}

	/**
	 * The Functional Group Macros of a CIOD, empty when it is not multi-frame enhanced.
	 */
	public List<FunctionalGroupMacro> getMacros(String ciodId) {
		return macrosByCiod.getOrDefault(ciodId, List.of());
	}

}
