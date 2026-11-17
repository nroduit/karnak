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
import org.weasis.dicom.util.StoreFromStreamSCU;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DicomForwardDestinationTest {

	private static DicomForwardDestination destination(int poolSize) throws IOException {
		ForwardDicomNode source = new ForwardDicomNode("SOURCE");
		DicomNode dest = new DicomNode("DEST", "localhost", 104);
		// No network is opened by the constructor; associations connect lazily on first
		// use.
		return new DicomForwardDestination(1L, null, source, dest, null, List.of(), null, true, poolSize);
	}

	/**
	 * The AE Title the SCU calls with (set on the association request only once opened).
	 */
	private static String callingAet(StoreFromStreamSCU scu) {
		return scu.getDevice().getApplicationEntities().iterator().next().getAETitle();
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

	@Test
	void source_aet_is_ignored_when_the_option_is_off() throws IOException {
		DicomForwardDestination destination = destination(1);

		ScuLease lease = destination.acquire("MODALITY");

		assertSame(destination.getStreamSCU(), lease.scu());
		assertEquals("SOURCE", callingAet(lease.scu()));
	}

	@Test
	void each_source_aet_gets_its_own_association_calling_with_it() throws IOException {
		DicomForwardDestination destination = destination(1);
		destination.setUseSourceAet(true);

		ScuLease modality1 = destination.acquire("MODALITY1");
		ScuLease modality2 = destination.acquire("MODALITY2");

		assertEquals("MODALITY1", callingAet(modality1.scu()));
		assertEquals("MODALITY2", callingAet(modality2.scu()));
		assertEquals("DEST", modality1.scu().getCalledNode().getAet());
		assertTrue(modality1.exclusive());
		assertTrue(modality2.exclusive(), "another source must not share the first source's association");

		destination.release(modality1);
		ScuLease again = destination.acquire("MODALITY1");
		assertSame(modality1.scu(), again.scu(), "the source's association must be reused");
		assertTrue(again.exclusive());
	}

	@Test
	void unknown_source_aet_falls_back_to_the_forward_node_aet() throws IOException {
		DicomForwardDestination destination = destination(1);
		destination.setUseSourceAet(true);

		assertEquals("SOURCE", callingAet(destination.acquire(null).scu()));
	}

	@Test
	void least_recently_used_idle_source_pool_is_closed_beyond_the_bound() throws IOException {
		DicomForwardDestination destination = destination(1);
		destination.setUseSourceAet(true);
		ScuLease oldest = destination.acquire("SRC0");
		destination.release(oldest);
		for (int i = 1; i <= DicomForwardDestination.MAX_SOURCE_POOLS; i++) {
			destination.release(destination.acquire("SRC" + i));
		}

		ScuLease again = destination.acquire("SRC0");

		assertNotSame(oldest.scu(), again.scu(), "the evicted source gets a new pool");
		assertEquals("SRC0", callingAet(again.scu()));
	}

	@Test
	void source_pool_with_a_lease_out_is_not_evicted() throws IOException {
		DicomForwardDestination destination = destination(1);
		destination.setUseSourceAet(true);
		ScuLease busy = destination.acquire("SRC0");
		// A shared lease on the exhausted pool also keeps it alive once the exclusive one
		// is returned
		ScuLease shared = destination.acquire("SRC0");
		destination.release(busy);
		assertFalse(shared.exclusive());
		for (int i = 1; i <= DicomForwardDestination.MAX_SOURCE_POOLS; i++) {
			destination.release(destination.acquire("SRC" + i));
		}

		assertSame(shared.scu(), destination.acquire("SRC0").scu(), "a pool in use must not be closed");
	}

	@Test
	void scu_initializer_also_applies_to_source_pools_created_later() throws IOException {
		DicomForwardDestination destination = destination(1);
		destination.setUseSourceAet(true);
		destination.setScuInitializer(scu -> scu.addData("1.2.840.10008.5.1.4.1.1.2", "1.2.840.10008.1.2.1"));

		ScuLease lease = destination.acquire("MODALITY");

		assertTrue(lease.scu().getAAssociateRQ().containsPresentationContextFor("1.2.840.10008.5.1.4.1.1.2"));
	}

}
