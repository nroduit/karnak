/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.DicomNodeList;
import org.karnak.backend.model.dicom.WebDestinationNode;
import org.karnak.backend.service.DicomNodeConfigService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.weasis.dicom.param.DicomNode;
import org.weasis.dicom.param.DicomNodeSource;

@DisplayNameGeneration(ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class DicomNodeUtilTest {

	@Mock
	private DicomNodeConfigService dicomNodeConfigService;

	@Mock
	private DestinationRepo destinationRepo;

	@Mock
	private DicomNodeSource gatewaySource;

	private DicomNodeUtil dicomNodeUtil;

	@BeforeEach
	void setUp() {
		dicomNodeUtil = new DicomNodeUtil(dicomNodeConfigService, destinationRepo, List.of(gatewaySource));
	}

	@Test
	void returns_dynamic_source_groups_first_then_service_node_types() {
		var workstations = new DicomNodeList("Workstations");
		var pacsWeb = new DicomNodeList("PACS Public WEB");
		when(gatewaySource.getGroupName()).thenReturn("Gateway destinations");
		when(gatewaySource.getNodes()).thenReturn(List.of());
		when(dicomNodeConfigService.getAllDicomNodeTypes()).thenReturn(List.of(workstations, pacsWeb));

		var result = dicomNodeUtil.getAllDicomNodeTypes();

		assertNotNull(result);
		assertEquals(3, result.size());
		assertEquals("Gateway destinations", result.get(0).getName());
		assertEquals("Workstations", result.get(1).getName());
		assertEquals("PACS Public WEB", result.get(2).getName());
	}

	@Test
	void dynamic_source_nodes_become_read_only_config_nodes_labelled_by_their_group() {
		when(gatewaySource.getGroupName()).thenReturn("Gateway destinations");
		when(gatewaySource.getNodes()).thenReturn(List.of(new DicomNode("PACS_AE", "pacs.host", 11112, "Main PACS"),
				new DicomNode("VIEWER_AE", "viewer.host", 104, null)));

		var groups = dicomNodeUtil.getDynamicNodeGroups();

		assertEquals(1, groups.size());
		assertEquals("Gateway destinations", groups.getFirst().getName());

		ConfigNode first = groups.getFirst().getFirst();
		assertEquals("Main PACS", first.getName());
		assertEquals("PACS_AE", first.getAet());
		assertEquals("pacs.host", first.getHostname());
		assertEquals(11112, first.getPort());
		assertNull(first.getId());
		assertEquals("Gateway destinations", first.getNodeType());

		// A node without a description falls back to its AE Title for the display name.
		assertEquals("VIEWER_AE", groups.getFirst().get(1).getName());
	}

	@Test
	void gateway_stow_destinations_lists_stow_only_deduped_with_description_fallback() {
		var stow = DestinationEntity.ofStow("Main web", "https://pacs/dicom-web", "");
		var stowDuplicate = DestinationEntity.ofStow("Duplicate", "https://pacs/dicom-web", "");
		var stowNoDescription = DestinationEntity.ofStow("", "https://other/dicom-web", "");
		var stowNoUrl = DestinationEntity.ofStow("No URL", "", "");
		var dicom = DestinationEntity.ofDicom("A PACS", "PACS_AE", "pacs.host", 104, false);
		when(destinationRepo.findAll()).thenReturn(List.of(stow, stowDuplicate, stowNoDescription, stowNoUrl, dicom));

		var result = dicomNodeUtil.getGatewayStowDestinations();

		assertEquals(2, result.size());
		assertEquals(new WebDestinationNode("Main web", "https://pacs/dicom-web"), result.get(0));
		// Description falls back to the URL when unset.
		assertEquals(new WebDestinationNode("https://other/dicom-web", "https://other/dicom-web"), result.get(1));
	}

	@Test
	void gateway_stow_destinations_is_empty_without_stow_destinations() {
		when(destinationRepo.findAll())
			.thenReturn(List.of(DestinationEntity.ofDicom("A PACS", "PACS_AE", "pacs.host", 104, false)));

		assertEquals(List.of(), dicomNodeUtil.getGatewayStowDestinations());
	}

	@Test
	void all_node_types_including_worklist_appends_worklist_groups_after_the_others() {
		var workstations = new DicomNodeList("Workstations");
		var worklists = new DicomNodeList("Worklists");
		when(gatewaySource.getGroupName()).thenReturn("Gateway destinations");
		when(gatewaySource.getNodes()).thenReturn(List.of());
		when(dicomNodeConfigService.getAllDicomNodeTypes()).thenReturn(List.of(workstations));
		when(dicomNodeConfigService.getWorkListNodeTypes()).thenReturn(List.of(worklists));

		var result = dicomNodeUtil.getAllNodeTypesIncludingWorklist();

		assertEquals(3, result.size());
		assertEquals("Gateway destinations", result.get(0).getName());
		assertEquals("Workstations", result.get(1).getName());
		assertEquals("Worklists", result.get(2).getName());
	}

	@Test
	void returns_worklist_node_types_from_service() {
		var worklists = List.of(new DicomNodeList("Worklists"));
		when(dicomNodeConfigService.getWorkListNodeTypes()).thenReturn(worklists);

		var result = dicomNodeUtil.getWorkListNodeTypes();

		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("Worklists", result.getFirst().getName());
	}

	@Test
	void delegates_save_dicom_node_to_service() {
		var node = new ConfigNode("PACS", new DicomNode("PACS", "pacs.host", 104));
		when(dicomNodeConfigService.saveNode("PACS", "PACS", "pacs.host", 104, "WORKSTATION")).thenReturn(node);

		var result = dicomNodeUtil.saveDicomNode("PACS", "PACS", "pacs.host", 104, "WORKSTATION");

		assertSame(node, result);
	}

	@Test
	void delegates_update_dicom_node_to_service() {
		var node = new ConfigNode("PACS", new DicomNode("PACS", "pacs.host", 104));
		when(dicomNodeConfigService.updateNode(1L, "PACS", "PACS", "pacs.host", 104)).thenReturn(node);

		var result = dicomNodeUtil.updateDicomNode(1L, "PACS", "PACS", "pacs.host", 104);

		assertSame(node, result);
	}

	@Test
	void delegates_delete_dicom_node_to_service() {
		dicomNodeUtil.deleteDicomNode(9L);

		verify(dicomNodeConfigService).deleteNode(9L);
	}

}
