/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.dcm4che3.net.ApplicationEntity;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.karnak.backend.dicom.ForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.mockito.Mockito;

class StoreScpForwardServiceTest {

	@Test
	void calling_aets_follow_the_live_routing_table() {
		ForwardDicomNode fwdNode = new ForwardDicomNode("FWD", null, 1L);
		fwdNode.addAcceptedSourceNode(1L, "SRC1", null, false);
		Map<ForwardDicomNode, List<ForwardDestination>> destinations = new ConcurrentHashMap<>();
		destinations.put(fwdNode, List.of());
		CStoreSCPService cStoreSCPService = Mockito.mock(CStoreSCPService.class);
		Mockito.when(cStoreSCPService.getSOPClasses()).thenReturn(new String[] { "*" });
		StoreScpForwardService service = new StoreScpForwardService(cStoreSCPService);
		service.init(destinations);
		ApplicationEntity ae = service.getApplicationEntity();

		assertTrue(ae.isAcceptedCallingAETitle("SRC1"));
		assertFalse(ae.isAcceptedCallingAETitle("SRC2"));

		// Source added at runtime: accepted without restarting the listener
		fwdNode.addAcceptedSourceNode(2L, "SRC2", null, false);
		assertTrue(ae.isAcceptedCallingAETitle("SRC2"));

		// Source removed at runtime: refused
		fwdNode.getAcceptedSourceNodes().removeIf(n -> "SRC1".equals(n.getAet()));
		assertFalse(ae.isAcceptedCallingAETitle("SRC1"));

		// A forward node without source restriction accepts any calling AET
		destinations.put(new ForwardDicomNode("OPEN", null, 2L), List.of());
		assertTrue(ae.isAcceptedCallingAETitle("ANY"));
	}

}
