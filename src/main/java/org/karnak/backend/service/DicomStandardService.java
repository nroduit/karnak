/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import java.util.List;
import org.karnak.backend.model.standard.AttributeDetail;
import org.karnak.backend.model.standard.ModuleAttribute;
import org.karnak.backend.model.standard.StandardDICOM;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * UI-facing read access to the DICOM standard dictionary, used by the profile element
 * editor to let users search and browse tags by module. Thin wrapper over the shared
 * {@link StandardDICOM} bean.
 */
@Service
public class DicomStandardService {

	/** Cap on the number of attributes returned by a single search. */
	public static final int MAX_SEARCH_RESULTS = 200;

	private final StandardDICOM standardDICOM;

	@Autowired
	public DicomStandardService(@Qualifier("StandardDICOM") StandardDICOM standardDICOM) {
		this.standardDICOM = standardDICOM;
	}

	/**
	 * Search the standard attributes by keyword, name or tag.
	 * @param query the text to match (an empty query returns a starting list)
	 * @param includeRetired whether retired attributes are included
	 * @return up to {@link #MAX_SEARCH_RESULTS} matching attributes, sorted by keyword
	 */
	public List<AttributeDetail> searchAttributes(String query, boolean includeRetired) {
		return standardDICOM.searchAttributes(query, includeRetired, MAX_SEARCH_RESULTS);
	}

	/** The identifiers of every known module, sorted alphabetically. */
	public List<String> listModuleIds() {
		return standardDICOM.getModuleIds();
	}

	/** The attributes of the given module (empty when the module is unknown). */
	public List<ModuleAttribute> moduleAttributes(String moduleId) {
		if (moduleId == null || standardDICOM.getAttributesByModule(moduleId) == null) {
			return List.of();
		}
		return standardDICOM.getAttributeListByModule(moduleId);
	}

	/**
	 * The standard detail (keyword, name, VR…) of a single tag, or {@code null} when
	 * unknown.
	 * @param tagHex the 8-character hexadecimal tag (e.g. {@code 00100010})
	 */
	public AttributeDetail attributeDetail(String tagHex) {
		return standardDICOM.getAttributeDetail(tagHex);
	}

}
