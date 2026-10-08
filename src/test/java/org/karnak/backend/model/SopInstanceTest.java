/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class SopInstanceTest {

	@Test
	void rejects_a_null_uid() {
		assertThrows(NullPointerException.class, () -> new SopInstance(null));
	}

	@Test
	void exposes_its_uid_and_mutable_metadata() {
		SopInstance sop = new SopInstance("1.2.3");
		sop.setInstanceNumber(7);
		sop.setSopClassUID("1.2.840.10008");
		sop.setSent(true);

		assertEquals("1.2.3", sop.getSopInstanceUID());
		assertEquals(7, sop.getInstanceNumber());
		assertEquals("1.2.840.10008", sop.getSopClassUID());
		assertTrue(sop.isSent());
	}

	@Nested
	class MapHelpers {

		@Test
		void adds_an_instance_keyed_by_its_uid() {
			Map<String, SopInstance> map = new HashMap<>();
			SopInstance sop = new SopInstance("1.2.3");

			SopInstance.addSopInstance(map, sop);

			assertSame(sop, map.get("1.2.3"));
		}

		@Test
		void ignores_null_instance_or_null_map_on_add() {
			Map<String, SopInstance> map = new HashMap<>();

			SopInstance.addSopInstance(map, null);
			SopInstance.addSopInstance(null, new SopInstance("1.2.3"));

			assertTrue(map.isEmpty());
		}

		@Test
		void gets_an_instance_or_null_for_unknown_uid() {
			Map<String, SopInstance> map = new HashMap<>();
			SopInstance sop = new SopInstance("1.2.3");
			SopInstance.addSopInstance(map, sop);

			assertSame(sop, SopInstance.getSopInstance(map, "1.2.3"));
			assertNull(SopInstance.getSopInstance(map, "9.9.9"));
		}

		@Test
		void returns_null_when_get_arguments_are_null() {
			Map<String, SopInstance> map = new HashMap<>();

			assertNull(SopInstance.getSopInstance(map, null));
			assertNull(SopInstance.getSopInstance(null, "1.2.3"));
		}

		@Test
		void removes_an_instance_and_returns_it() {
			Map<String, SopInstance> map = new HashMap<>();
			SopInstance sop = new SopInstance("1.2.3");
			SopInstance.addSopInstance(map, sop);

			assertSame(sop, SopInstance.removeSopInstance(map, "1.2.3"));
			assertFalse(map.containsKey("1.2.3"));
		}

		@Test
		void returns_null_when_remove_arguments_are_null() {
			Map<String, SopInstance> map = new HashMap<>();

			assertNull(SopInstance.removeSopInstance(map, null));
			assertNull(SopInstance.removeSopInstance(null, "1.2.3"));
		}

	}

}