/*
 * Copyright (c) 2021 Karnak Team and other contributors.
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
import org.karnak.backend.model.dicominnolitics.StandardAttributes;
import org.karnak.backend.model.dicominnolitics.jsonAttributes;

public class AttributeDetails {

	private final Map<String, AttributeDetail> hmapAttributeDetail;

	public AttributeDetails() {
		hmapAttributeDetail = initializeAttributes(StandardAttributes.readJsonAttributes());
	}

	private Map<String, AttributeDetail> initializeAttributes(jsonAttributes[] attributes) {
		Map<String, AttributeDetail> hmapAttribute = new HashMap<>();

		for (jsonAttributes attribute : attributes) {
			AttributeDetail attributeDetail = new AttributeDetail(attribute.getId(), attribute.getKeyword(),
					attribute.getName(), attribute.getRetired(), attribute.getTag(), attribute.getValueMultiplicity(),
					attribute.getValueRepresentation());

			String attributeKey = attributeDetail.getId();
			hmapAttribute.put(attributeKey, attributeDetail);
		}

		return hmapAttribute;
	}

	public AttributeDetail getAttributeDetail(String id) {
		return hmapAttributeDetail.get(id);
	}

	public List<AttributeDetail> getListAttributeDetail(List<String> listId) {
		return hmapAttributeDetail.entrySet()
			.stream()
			.filter(attributeDetail -> listId.contains(attributeDetail.getKey()))
			.map(Map.Entry::getValue)
			.toList();
	}

}
