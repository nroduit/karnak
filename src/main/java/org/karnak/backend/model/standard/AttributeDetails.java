/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
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

import org.jspecify.annotations.Nullable;
import org.karnak.backend.model.dicominnolitics.JsonAttributes;
import org.karnak.backend.model.dicominnolitics.StandardAttributes;

public class AttributeDetails {

	private final Map<String, AttributeDetail> mapAttributeDetail;

	public AttributeDetails() {
		mapAttributeDetail = initializeAttributes(StandardAttributes.readJsonAttributes());
	}

	private Map<String, AttributeDetail> initializeAttributes(JsonAttributes[] attributes) {
		Map<String, AttributeDetail> mapAttribute = new HashMap<>();

		for (JsonAttributes attribute : attributes) {
			AttributeDetail attributeDetail = new AttributeDetail(attribute.getId(), attribute.getKeyword(),
					attribute.getName(), attribute.getRetired(), attribute.getTag(), attribute.getValueMultiplicity(),
					attribute.getValueRepresentation());

			String attributeKey = attributeDetail.id();
			mapAttribute.put(attributeKey, attributeDetail);
		}

		return mapAttribute;
	}

	public @Nullable AttributeDetail getAttributeDetail(String id) {
		return mapAttributeDetail.get(id);
	}

	/**
	 * Search the standard attributes by keyword, name or tag (case-insensitive,
	 * substring). An empty query returns the first {@code limit} attributes so the UI can
	 * show a starting list.
	 * @param query the text to match against keyword / name / tag
	 * @param includeRetired when {@code false}, retired attributes are excluded
	 * @param limit the maximum number of results
	 * @return the matching attributes, sorted by keyword
	 */
	public List<AttributeDetail> search(String query, boolean includeRetired, int limit) {
		String q = query == null ? "" : query.trim().toLowerCase();
		String qTag = q.replaceAll("[^0-9a-fx]", "");
		return mapAttributeDetail.values()
			.stream()
			.filter(a -> includeRetired || !"Y".equalsIgnoreCase(a.retired()))
			.filter(a -> q.isEmpty() || contains(a.keyword(), q) || contains(a.name(), q) || contains(a.tag(), q)
					|| (!qTag.isEmpty() && contains(a.id(), qTag)))
			.sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(safe(a.keyword()), safe(b.keyword())))
			.limit(limit)
			.toList();
	}

	private static boolean contains(String value, String lowerCaseQuery) {
		return value != null && value.toLowerCase().contains(lowerCaseQuery);
	}

	private static String safe(String value) {
		return value == null ? "" : value;
	}

	public List<AttributeDetail> getListAttributeDetail(List<String> listId) {
		return mapAttributeDetail.entrySet()
			.stream()
			.filter(attributeDetail -> listId.contains(attributeDetail.getKey()))
			.map(Map.Entry::getValue)
			.toList();
	}

}
