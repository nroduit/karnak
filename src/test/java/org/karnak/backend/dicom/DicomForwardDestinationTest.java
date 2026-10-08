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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.dicom.DicomForwardDestination.ScuLease;
import org.weasis.dicom.param.DicomNode;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DicomForwardDestinationTest {

	private static DicomForwardDestination destination(int poolSize) throws IOException {
		ForwardDicomNode source = new ForwardDicomNode("SOURCE");
		DicomNode dest = new DicomNode("DEST", "localhost", 104);
		// No network is opened by the constructor; associations connect lazily on first
		// use.
		return new DicomForwardDestination(1L, null, source, dest, false, null, List.of(), null, true, poolSize);
	}

	@Test
	void single_connection_pool_always_leases_the_same_scu() throws IOException {
		DicomForwardDestination destination = destination(1);

		ScuLease first = destination.acquire();
		ScuLease second = destination.acquire();

		assertSame(first.scu(), second.scu(), "pool of 1 must reuse the one association");
		assertTrue(first.exclusive());
		// The second concurrent lease shares the only connection (pool exhausted).
		assertFalse(second.exclusive());
	}

	@Test
	void pool_hands_out_distinct_connections_until_exhausted() throws IOException {
		DicomForwardDestination destination = destination(2);

		ScuLease a = destination.acquire();
		ScuLease b = destination.acquire();
		ScuLease c = destination.acquire();

		assertNotSame(a.scu(), b.scu(), "distinct leases must use distinct associations");
		assertTrue(a.exclusive());
		assertTrue(b.exclusive());
		// Pool exhausted: the third lease shares an existing association rather than
		// blocking.
		assertFalse(c.exclusive());
	}

	@Test
	void releasing_a_lease_frees_the_slot_for_reuse() throws IOException {
		DicomForwardDestination destination = destination(2);

		ScuLease a = destination.acquire();
		ScuLease b = destination.acquire();
		destination.release(a);

		ScuLease reused = destination.acquire();
		assertTrue(reused.exclusive(), "a freed slot must be leased exclusively again");
		assertSame(a.scu(), reused.scu(), "the released association should be the one reused");
		assertNotSame(b.scu(), reused.scu());
	}

}