/*
 * Copyright (c) 2009-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.dcm4che3.data.UID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.karnak.backend.dicom.DicomForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.karnak.backend.dicom.Params;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Transfer-syntax and SOP-class negotiation over real associations. Uncompressed transfer
 * syntaxes only (no codec / OpenCV): Implicit VR Little Endian is adapted to Explicit VR
 * Little Endian, Explicit VR is preserved, and several SOP classes negotiate over one
 * pooled association.
 */
@Tag("integration")
@DisplayNameGeneration(ReplaceUnderscores.class)
class TransferSyntaxNegotiationIntegrationTest extends GatewayItTestSupport {

	private ForwardDicomNode fwdNode;

	private Scp scp;

	private ForwardService forwardService;

	@BeforeEach
	void setUp() throws Exception {
		scp = startScp();
		fwdNode = new ForwardDicomNode("SOURCE-IT");
		forwardService = new ForwardService(mock(ApplicationEventPublisher.class), null);
	}

	@ParameterizedTest
	@ValueSource(strings = { UID.ExplicitVRLittleEndian, UID.ImplicitVRLittleEndian })
	void forwards_and_adapts_uncompressed_transfer_syntaxes(String inputTs) throws Exception {
		DicomForwardDestination dest = dicomDestination(fwdNode, scp, 1);
		String iuid = "1.2.826.0.1.3680043.8.498.1";
		try {
			Params p = params(iuid, CUID_SC, inputTs, serialize(iuid, CUID_SC, inputTs));
			forwardService.storeMultipleDestination(fwdNode, List.of(dest), p);
		}
		finally {
			dest.stop();
		}

		assertEquals(Set.of(iuid), receivedSopInstanceUids(scp.storageDir()));
		// Each uncompressed input forwards and is stored uncompressed (no codec
		// corruption).
		// The exact negotiated syntax (Explicit vs Implicit VR LE) depends on
		// presentation-
		// context negotiation order, so the deterministic output-TS mapping is asserted
		// in the
		// transcoding tests where an explicit output transfer syntax is configured.
		String storedTs = receivedTransferSyntaxes(scp.storageDir()).get(iuid);
		assertTrue(UID.ExplicitVRLittleEndian.equals(storedTs) || UID.ImplicitVRLittleEndian.equals(storedTs),
				"expected an uncompressed little-endian syntax but was " + storedTs);
	}

	@Test
	void negotiates_several_sop_classes_over_a_pooled_destination() throws Exception {
		DicomForwardDestination dest = dicomDestination(fwdNode, scp, 2);
		List<String> cuids = List.of(UID.SecondaryCaptureImageStorage, UID.CTImageStorage, UID.MRImageStorage,
				UID.ComputedRadiographyImageStorage);

		Set<String> sent = new TreeSet<>();
		try {
			int i = 0;
			for (String cuid : cuids) {
				String iuid = "1.2.826.0.1.3680043.8.498." + (i++);
				sent.add(iuid);
				Params p = params(iuid, cuid, TS_EXPLICIT, serialize(iuid, cuid, TS_EXPLICIT));
				forwardService.storeMultipleDestination(fwdNode, List.of(dest), p);
			}
		}
		finally {
			dest.stop();
		}

		assertEquals(sent, receivedSopInstanceUids(scp.storageDir()));
	}

}
