/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.dicom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.weasis.dicom.param.DicomNode;

@DisplayNameGeneration(ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class GatewayDicomNodeSourceTest {

	@Mock
	private DestinationRepo destinationRepo;

	@InjectMocks
	private GatewayDicomNodeSource gatewaySource;

	@Test
	void group_name_is_the_gateway_destinations_label() {
		assertEquals("Gateway destinations", gatewaySource.getGroupName());
	}

	@Test
	void lists_dicom_destinations_with_descriptions_skipping_stow_and_deduplicating() {
		var pacs = DestinationEntity.ofDicom("Main PACS", "PACS_AE", "pacs.host", 11112, false);
		var pacsDuplicate = DestinationEntity.ofDicom("PACS copy", "PACS_AE", "pacs.host", 11112, false);
		var viewer = DestinationEntity.ofDicom("Viewer", "VIEWER_AE", "viewer.host", 104, false);
		var stow = DestinationEntity.ofStow("Cloud", "https://stow.example/dicomweb", "");
		when(destinationRepo.findAll()).thenReturn(List.of(pacs, pacsDuplicate, viewer, stow));

		List<DicomNode> nodes = gatewaySource.getNodes();

		assertEquals(2, nodes.size());

		DicomNode first = nodes.getFirst();
		assertEquals("Main PACS", first.getDescription());
		assertEquals("PACS_AE", first.getAet());
		assertEquals("pacs.host", first.getHostname());
		assertEquals(11112, first.getPort());
		// Dynamic nodes carry no configuration id (read-only).
		assertNull(first.getId());

		assertEquals("VIEWER_AE", nodes.get(1).getAet());
	}

	@Test
	void description_falls_back_to_ae_title_when_unset() {
		var pacs = DestinationEntity.ofDicom("", "PACS_AE", "pacs.host", 11112, false);
		when(destinationRepo.findAll()).thenReturn(List.of(pacs));

		assertEquals("PACS_AE", gatewaySource.getNodes().getFirst().getDescription());
	}

	@Test
	void empty_when_no_dicom_destination_is_configured() {
		when(destinationRepo.findAll()).thenReturn(List.of());

		assertEquals(List.of(), gatewaySource.getNodes());
	}

}