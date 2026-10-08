/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.echo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class DestinationEchosTest {

	@Test
	void exposes_the_wrapped_destination_list() {
		List<DestinationEcho> echos = List.of(new DestinationEcho("aet", "url", 200));
		DestinationEchos destinationEchos = new DestinationEchos(echos);

		assertSame(echos, destinationEchos.destinationEchos());
		assertEquals(1, destinationEchos.destinationEchos().size());
	}

}