/*
 * Copyright (c) 2009-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.dicom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class CommonStorageSopClassesTest {

	// DICOM allows at most 128 presentation contexts per association.
	private static final int MAX_PRESENTATION_CONTEXTS = 128;

	private static final int TRANSFER_SYNTAXES_PER_SOP_CLASS = 2;

	@Test
	void list_is_not_empty() {
		assertFalse(CommonStorageSopClasses.UIDS.isEmpty());
	}

	@Test
	void contains_no_duplicate_uid() {
		Set<String> unique = new HashSet<>(CommonStorageSopClasses.UIDS);
		assertEquals(CommonStorageSopClasses.UIDS.size(), unique.size(), "the pre-negotiation list has duplicate UIDs");
	}

	@Test
	void every_uid_is_a_non_blank_dotted_string() {
		for (String uid : CommonStorageSopClasses.UIDS) {
			assertTrue(uid != null && uid.matches("[0-9.]+"), () -> "not a valid UID: " + uid);
		}
	}

	@Test
	void stays_within_the_presentation_context_budget_with_headroom() {
		int contexts = CommonStorageSopClasses.UIDS.size() * TRANSFER_SYNTAXES_PER_SOP_CLASS;
		// Keep a comfortable margin (< 85% of the limit) for on-demand and compressed
		// contexts.
		assertTrue(contexts <= MAX_PRESENTATION_CONTEXTS * 0.85, () -> contexts
				+ " pre-negotiated contexts leaves too little headroom under " + MAX_PRESENTATION_CONTEXTS);
	}

}