/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.dicom;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.dcm4che3.data.UID;
import org.dcm4che3.img.DicomOutputData;
import org.dcm4che3.img.util.DicomUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.weasis.dicom.param.DicomNode;

class ForwardDestinationTest {

	// Mock
	MockedStatic<DicomUtils> dicomUtilsMock;

	MockedStatic<DicomOutputData> dicomOutputDataMock;

	@BeforeEach
	void setUp() {
		dicomUtilsMock = Mockito.mockStatic(DicomUtils.class);
		dicomOutputDataMock = Mockito.mockStatic(DicomOutputData.class);
	}

	@AfterEach
	void tearDown() {
		// Close static mock
		if (dicomUtilsMock != null) {
			dicomUtilsMock.close();
		}
		if (dicomOutputDataMock != null) {
			dicomOutputDataMock.close();
		}
	}

	@Test
	void when_get_output_transfer_syntax_should_retrieve_value_initialized() throws IOException {
		// Init data
		ForwardDicomNode forwardDicomNode = new ForwardDicomNode("fwdAeTitle");
		DicomNode dicomNode = new DicomNode("fwdAeTitle", 1111);
		ForwardDestination forwardDestination = new DicomForwardDestination(forwardDicomNode, dicomNode);

		// Call method
		String outputTransferSyntax = forwardDestination.getOutputTransferSyntax();

		// Test result
		assertEquals("", outputTransferSyntax);
	}

	@Test
	void when_not_native_not_rle_lossless_should_return_originalTsuid() throws IOException {
		// Init data
		ForwardDicomNode forwardDicomNode = new ForwardDicomNode("fwdAeTitle");
		DicomNode dicomNode = new DicomNode("fwdAeTitle", 1111);
		ForwardDestination forwardDestination = new DicomForwardDestination(forwardDicomNode, dicomNode);

		// Mock
		dicomUtilsMock.when(() -> DicomUtils.isNative(Mockito.anyString())).thenReturn(false);

		// Call method
		String outputTransferSyntax = forwardDestination.getOutputTransferSyntax("originalTsuid");

		// Test result
		assertEquals("originalTsuid", outputTransferSyntax);
	}

	@Test
	void when_uid_rlelossless_or_endian_should_return_explicit_vr_little_endian() throws IOException {
		// Init data
		ForwardDicomNode forwardDicomNode = new ForwardDicomNode("fwdAeTitle");
		DicomNode dicomNode = new DicomNode("fwdAeTitle", 1111);
		ForwardDestination forwardDestination = new DicomForwardDestination(forwardDicomNode, dicomNode);

		// Mock
		dicomUtilsMock.when(() -> DicomUtils.isNative(Mockito.anyString())).thenReturn(true);
		dicomOutputDataMock.when(() -> DicomOutputData.isSupportedSyntax(Mockito.anyString())).thenReturn(false);

		// Call method
		String outputTransferSyntax = forwardDestination.getOutputTransferSyntax(UID.RLELossless);

		// Test result
		assertEquals(UID.ExplicitVRLittleEndian, outputTransferSyntax);

		// Call method
		outputTransferSyntax = forwardDestination.getOutputTransferSyntax(UID.ImplicitVRLittleEndian);

		// Test result
		assertEquals(UID.ExplicitVRLittleEndian, outputTransferSyntax);

		// Call method
		outputTransferSyntax = forwardDestination.getOutputTransferSyntax(UID.ExplicitVRBigEndian);

		// Test result
		assertEquals(UID.ExplicitVRLittleEndian, outputTransferSyntax);
	}

}
