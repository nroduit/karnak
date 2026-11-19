/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DocumentationLinksTest {

	@Test
	void appendsVersionAsQueryParameter() {
		assertEquals("https://karnak.weasis.org/en/profiles/?v=2.0.1",
				DocumentationLinks.withVersion("https://karnak.weasis.org/en/profiles/", "2.0.1"));
	}

	@Test
	void insertsVersionBeforeFragment() {
		assertEquals("https://host/doc/index.html?v=2.0.1#5-project-secret",
				DocumentationLinks.withVersion("https://host/doc/index.html#5-project-secret", "2.0.1"));
	}

	@Test
	void appendsToExistingQuery() {
		assertEquals("https://host/doc/?lang=en&v=2.1.0-SNAPSHOT",
				DocumentationLinks.withVersion("https://host/doc/?lang=en", "2.1.0-SNAPSHOT"));
	}

	@Test
	void leavesUrlUnchangedWithoutVersion() {
		assertEquals("https://host/doc/", DocumentationLinks.withVersion("https://host/doc/", null));
		assertEquals("https://host/doc/", DocumentationLinks.withVersion("https://host/doc/", " "));
	}

	@Test
	void resolvesTopicAgainstBaseUrl() {
		String url = DocumentationLinks.url("en/profiles/");
		assertEquals(true, url.startsWith(DocumentationLinks.BASE_URL + "en/profiles/"));
	}

}
