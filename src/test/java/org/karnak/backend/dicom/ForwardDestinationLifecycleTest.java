/*
 * Copyright (c) 2026 Karnak Team and other contributors.
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

import org.junit.jupiter.api.Test;
import org.weasis.dicom.param.DicomState;

class ForwardDestinationLifecycleTest {

	@Test
	void retiring_an_idle_destination_stops_it_once() {
		CountingDestination destination = new CountingDestination();

		destination.retire();
		destination.retire();

		assertEquals(1, destination.stops);
	}

	@Test
	void retiring_a_busy_destination_stops_it_after_its_last_transfer() {
		CountingDestination destination = new CountingDestination();
		assertTrue(destination.beginTransfer());
		assertTrue(destination.beginTransfer());

		destination.retire();
		assertEquals(0, destination.stops);

		destination.endTransfer();
		assertEquals(0, destination.stops);
		destination.endTransfer();
		assertEquals(1, destination.stops);
	}

	@Test
	void a_retired_destination_refuses_new_transfers() {
		CountingDestination destination = new CountingDestination();
		assertTrue(destination.beginTransfer());

		destination.retire();

		assertFalse(destination.beginTransfer());
		destination.endTransfer();
		assertEquals(1, destination.stops);
	}

	@Test
	void transfers_without_retirement_never_stop_the_destination() {
		CountingDestination destination = new CountingDestination();
		assertTrue(destination.beginTransfer());
		destination.endTransfer();

		assertEquals(0, destination.stops);
	}

	private static final class CountingDestination extends ForwardDestination {

		private int stops;

		CountingDestination() {
			super(1L, null);
		}

		@Override
		public ForwardDicomNode getForwardDicomNode() {
			return new ForwardDicomNode("FWD");
		}

		@Override
		public void stop() {
			stops++;
		}

		@Override
		public DicomState getState() {
			return new DicomState();
		}

	}

}
