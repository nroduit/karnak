/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpRequest.BodyPublisher;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class UtilsTest {

	@Test
	void empty_map_produces_an_empty_body() {
		BodyPublisher publisher = Utils.buildDataFromMap(Map.of());

		assertEquals(0, publisher.contentLength());
	}

	@Test
	void single_entry_is_url_encoded_as_key_value() {
		// "key=a b" url-encodes the space as '+' -> "key=a+b" (7 bytes).
		BodyPublisher publisher = Utils.buildDataFromMap(Map.of("key", "a b"));

		assertEquals("key=a+b".length(), publisher.contentLength());
	}

	@Test
	void multiple_entries_are_joined_with_an_ampersand() {
		Map<Object, Object> data = new LinkedHashMap<>();
		data.put("a", "1");
		data.put("b", "2");

		BodyPublisher publisher = Utils.buildDataFromMap(data);

		// "a=1&b=2" -> 7 bytes.
		assertEquals("a=1&b=2".length(), publisher.contentLength());
	}

}