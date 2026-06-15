/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.echo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DestinationEchoTest {

	@Test
	void when_same_values_should_be_equal() {
		// Init data
		DestinationEcho destinationEcho = new DestinationEcho("aet", "url", 111);
		DestinationEcho destinationEchoToCompare = new DestinationEcho();
		destinationEchoToCompare.setAet("aet");
		destinationEchoToCompare.setStatus(111);
		destinationEchoToCompare.setUrl("url");

		// Test results
		assertNotNull(destinationEcho);
		assertEquals(destinationEcho, destinationEcho);
		assertEquals(destinationEchoToCompare, destinationEcho);
		assertEquals(destinationEcho.hashCode(), destinationEchoToCompare.hashCode());
	}

}
