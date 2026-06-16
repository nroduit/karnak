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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.service.ForwardService;
import org.springframework.context.ApplicationEventPublisher;
import org.weasis.dicom.param.DicomNode;

class ForwardUtilTest {

	// Service
	private ForwardService forwardService;

	@BeforeEach
	public void setUp() {
		final ApplicationEventPublisher applicationEventPublisher = mock(ApplicationEventPublisher.class);

		// Build mocked service
		forwardService = new ForwardService(applicationEventPublisher);
	}

	@Test
	void should_create_params() {

		// Init data
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("test".getBytes());

		// Create params
		Params p = new Params("iuid", "cuid", "tsuid", 0, byteArrayInputStream, null);

		// Test results
		assertEquals("iuid", p.getIuid());
		assertEquals("cuid", p.getCuid());
		assertEquals("tsuid", p.getTsuid());
		assertEquals(0, p.getPriority());
		assertEquals(byteArrayInputStream, p.getData());
		assertNull(p.getAs());
	}

	@Test
	void when_store_no_destination_should_throw_exception() {
		// Init data
		List<ForwardDestination> destList = new ArrayList<>();
		ForwardDicomNode fwdNode = new ForwardDicomNode("fwdAeTitle");

		Assertions.assertThrows(IllegalStateException.class, () -> {
			// Call method
			forwardService.storeMultipleDestination(fwdNode, destList, null);
		});
	}

	@Test
	void when_store_dicom_dir_should_return() throws IOException {
		// Init data
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("test".getBytes());
		// Create params
		Params p = new Params("iuid", "1.2.840.10008.1.3.10", "tsuid", 0, byteArrayInputStream, null);
		List<ForwardDestination> destList = new ArrayList<>();
		ForwardDicomNode forwardDicomNode = new ForwardDicomNode("fwdAeTitle");
		DicomNode dicomNode = new DicomNode("fwdAeTitle", 1111);
		ForwardDestination forwardDestination = new DicomForwardDestination(forwardDicomNode, dicomNode);
		destList.add(forwardDestination);

		Assertions.assertDoesNotThrow(() -> {
			// Call method
			forwardService.storeMultipleDestination(forwardDicomNode, destList, p);
		});
	}

	@Test
	void when_store_one_dest_remote_dest_invalid_type_dicom_should_throw_io_exception() throws IOException {

		// Init data
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream("test".getBytes());
		// Create params
		Params p = new Params("iuid", "cuid", "tsuid", 0, byteArrayInputStream, null);
		List<ForwardDestination> destList = new ArrayList<>();
		ForwardDicomNode forwardDicomNode = new ForwardDicomNode("fwdAeTitle");
		DicomNode dicomNode = new DicomNode("fwdAeTitle", 1111);
		ForwardDestination forwardDestination = new DicomForwardDestination(forwardDicomNode, dicomNode);
		destList.add(forwardDestination);

		Assertions.assertThrows(IOException.class, () -> {
			// Call method
			forwardService.storeMultipleDestination(forwardDicomNode, destList, p);
		});
	}

}
