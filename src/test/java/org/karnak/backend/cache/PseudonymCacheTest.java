/*
 * Copyright (c) 2025 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.patient.PatientModel;

/**
 * Exercises {@link PseudonymCache} through the real in-memory implementation (backed by a
 * Spring {@code ConcurrentMapCache}), so no cache mocking is needed.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class PseudonymCacheTest {

	private PseudonymCache cache;

	@BeforeEach
	void setUp() {
		cache = new InMemoryExternalIDCache();
	}

	private static PatientModel patient(String key) {
		return new PatientModel("pseudo-" + key, key, "John", "Doe", "PDA", 1L, UUID.randomUUID());
	}

	@Test
	void put_stores_the_patient_and_returns_null_when_the_key_is_absent() {
		var patient = patient("EREN");

		assertNull(cache.put("EREN", patient));
		assertSame(patient, cache.get("EREN"));
	}

	@Test
	void put_is_insert_if_absent_and_does_not_overwrite_an_existing_entry() {
		var first = patient("EREN");
		var second = patient("EREN");
		cache.put("EREN", first);

		PatientModel previous = cache.put("EREN", second);

		assertSame(first, previous);
		assertSame(first, cache.get("EREN"));
	}

	@Test
	void get_returns_null_for_an_unknown_key() {
		assertNull(cache.get("missing"));
	}

	@Test
	void remove_evicts_a_single_entry() {
		cache.put("EREN", patient("EREN"));

		cache.remove("EREN");

		assertNull(cache.get("EREN"));
	}

	@Test
	void get_all_is_empty_on_a_fresh_cache() {
		assertTrue(cache.getAll().isEmpty());
	}

	@Test
	void get_all_returns_every_stored_patient() {
		var eren = patient("EREN");
		var test = patient("TEST");
		cache.put("EREN", eren);
		cache.put("TEST", test);

		Collection<PatientModel> all = cache.getAll();

		assertEquals(2, all.size());
		assertTrue(all.contains(eren));
		assertTrue(all.contains(test));
	}

	@Test
	void remove_all_clears_the_whole_cache() {
		cache.put("EREN", patient("EREN"));
		cache.put("TEST", patient("TEST"));

		cache.removeAll();

		assertTrue(cache.getAll().isEmpty());
	}

}