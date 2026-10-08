/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profilebody;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ProfileBodyTest {

	@Nested
	class ProfileElement {

		@Test
		void stores_and_exposes_all_fields() {
			ProfileElementBody element = new ProfileElementBody();
			element.setName("name");
			element.setCodename("action.on.tags");
			element.setCondition("tagIsPresent('00100010')");
			element.setAction("X");
			element.setOption("shift");
			element.setArgs("days=1");
			element.setTags(List.of("(0010,0010)"));
			element.setExcludedTags(List.of("(0010,0020)"));
			element.setArguments(Map.of("days", "1"));

			assertEquals("name", element.getName());
			assertEquals("action.on.tags", element.getCodename());
			assertEquals("tagIsPresent('00100010')", element.getCondition());
			assertEquals("X", element.getAction());
			assertEquals("shift", element.getOption());
			assertEquals("days=1", element.getArgs());
			assertEquals(List.of("(0010,0010)"), element.getTags());
			assertEquals(List.of("(0010,0020)"), element.getExcludedTags());
			assertEquals(Map.of("days", "1"), element.getArguments());
		}

	}

	@Nested
	class ProfilePipe {

		@Test
		void stores_and_exposes_all_fields() {
			ProfileElementBody element = new ProfileElementBody();
			MaskBody mask = new MaskBody();
			ProfilePipeBody pipe = new ProfilePipeBody();
			pipe.setName("profile");
			pipe.setVersion("1.0");
			pipe.setMinimumKarnakVersion("1.2");
			pipe.setDefaultIssuerOfPatientID("PDA");
			pipe.setProfileElements(List.of(element));
			pipe.setMasks(List.of(mask));

			assertEquals("profile", pipe.getName());
			assertEquals("1.0", pipe.getVersion());
			assertEquals("1.2", pipe.getMinimumKarnakVersion());
			assertEquals("PDA", pipe.getDefaultIssuerOfPatientID());
			assertEquals(List.of(element), pipe.getProfileElements());
			assertEquals(List.of(mask), pipe.getMasks());
		}

	}

}