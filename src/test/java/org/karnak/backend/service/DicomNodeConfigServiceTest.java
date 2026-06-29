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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.karnak.backend.data.entity.DicomNodeConfigEntity;
import org.karnak.backend.data.repo.DicomNodeConfigRepo;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class DicomNodeConfigServiceTest {

	@Mock
	private DicomNodeConfigRepo dicomNodeConfigRepo;

	@InjectMocks
	private DicomNodeConfigService dicomNodeConfigService;

	private static DicomNodeConfigEntity node(String description, String aet, String host, int port, String type,
			String group) {
		return new DicomNodeConfigEntity(description, aet, host, port, type, group);
	}

	@Test
	void groups_managed_nodes_by_node_group_ordered_with_ungrouped_under_workstation() {
		var ungrouped = node("Karnak", "KARNAK-GATEWAY", "localhost", 11112, "WORKSTATION", null);
		var pacs = node("Public", "DICOMSERVER", "dicomserver.co.uk", 11112, "WORKSTATION", "PACS_WEB");
		when(dicomNodeConfigRepo.findByNodeTypeNot("WORKLIST")).thenReturn(List.of(ungrouped, pacs));

		var result = dicomNodeConfigService.getAllDicomNodeTypes();

		assertEquals(2, result.size());
		assertEquals("PACS_WEB", result.get(0).getName());
		assertEquals("WORKSTATION", result.get(1).getName());
		assertEquals("KARNAK-GATEWAY", result.get(1).getFirst().getAet());
	}

	@Test
	void exposes_id_and_node_type_on_loaded_nodes() {
		var workstation = node("Karnak", "KARNAK-GATEWAY", "localhost", 11112, "WORKSTATION", null);
		workstation.setId(42L);
		when(dicomNodeConfigRepo.findByNodeTypeNot("WORKLIST")).thenReturn(List.of(workstation));

		var configNode = dicomNodeConfigService.getAllDicomNodeTypes().get(0).getFirst();

		assertEquals(42L, configNode.getId());
		assertEquals("WORKSTATION", configNode.getNodeType());
	}

	@Test
	void groups_worklist_nodes_by_node_group_with_ungrouped_under_worklists() {
		var ungrouped = node("Public", "ADVT", "dicomserver.co.uk", 104, "WORKLIST", null);
		var grouped = node("Site", "SITE-MWL", "mwl.host", 105, "WORKLIST", "SiteA");
		when(dicomNodeConfigRepo.findByNodeType("WORKLIST")).thenReturn(List.of(ungrouped, grouped));

		var result = dicomNodeConfigService.getWorkListNodeTypes();

		assertEquals(2, result.size());
		assertEquals("SiteA", result.get(0).getName());
		assertEquals("SITE-MWL", result.get(0).getFirst().getAet());
		assertEquals("Worklists", result.get(1).getName());
		assertEquals("ADVT", result.get(1).getFirst().getAet());
	}

	@Test
	void known_groups_are_the_distinct_node_groups() {
		when(dicomNodeConfigRepo.findDistinctNodeGroups()).thenReturn(List.of("PACS_WEB", "SiteA"));

		assertEquals(List.of("PACS_WEB", "SiteA"), dicomNodeConfigService.getKnownGroups());
	}

	@Test
	void node_types_include_workstation_and_worklist_plus_custom_types() {
		when(dicomNodeConfigRepo.findDistinctNodeTypes()).thenReturn(List.of("PACS"));

		assertEquals(List.of("PACS", "WORKLIST", "WORKSTATION"), dicomNodeConfigService.getNodeTypes());
	}

	@Test
	void save_node_persists_type_and_group() {
		when(dicomNodeConfigRepo.save(any(DicomNodeConfigEntity.class))).thenAnswer(invocation -> {
			DicomNodeConfigEntity entity = invocation.getArgument(0);
			entity.setId(7L);
			return entity;
		});

		var saved = dicomNodeConfigService.saveNode("My PACS", "PACS", "pacs.host", 104, "PACS", "SiteA");

		var captor = ArgumentCaptor.forClass(DicomNodeConfigEntity.class);
		verify(dicomNodeConfigRepo).save(captor.capture());
		assertEquals("PACS", captor.getValue().getNodeType());
		assertEquals("SiteA", captor.getValue().getNodeGroup());
		assertEquals(7L, saved.getId());
	}

	@Test
	void save_node_quick_overload_uses_no_group() {
		when(dicomNodeConfigRepo.save(any(DicomNodeConfigEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		dicomNodeConfigService.saveNode("Karnak", "KARNAK", "localhost", 11112, "WORKSTATION");

		var captor = ArgumentCaptor.forClass(DicomNodeConfigEntity.class);
		verify(dicomNodeConfigRepo).save(captor.capture());
		assertNull(captor.getValue().getNodeGroup());
	}

	@Test
	void save_node_falls_back_to_ae_title_when_description_is_blank() {
		when(dicomNodeConfigRepo.save(any(DicomNodeConfigEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		var saved = dicomNodeConfigService.saveNode("  ", "PACS", "pacs.host", 104, "WORKSTATION", null);

		assertEquals("PACS", saved.getName());
	}

	@Test
	void save_node_rejects_only_the_dynamic_gateway_destinations_group() {
		assertThrows(IllegalArgumentException.class,
				() -> dicomNodeConfigService.saveNode("n", "AET", "h", 104, "WORKSTATION", "Gateway destinations"));
		verify(dicomNodeConfigRepo, never()).save(any(DicomNodeConfigEntity.class));
	}

	@Test
	void save_node_allows_grouping_a_worklist_node() {
		when(dicomNodeConfigRepo.save(any(DicomNodeConfigEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		dicomNodeConfigService.saveNode("MWL", "SITE-MWL", "mwl.host", 105, "WORKLIST", "SiteA");

		var captor = ArgumentCaptor.forClass(DicomNodeConfigEntity.class);
		verify(dicomNodeConfigRepo).save(captor.capture());
		assertEquals("WORKLIST", captor.getValue().getNodeType());
		assertEquals("SiteA", captor.getValue().getNodeGroup());
	}

	@Test
	void update_node_mutates_type_and_group() {
		var existing = node("Old", "OLD", "old.host", 100, "WORKSTATION", "SiteA");
		existing.setId(5L);
		when(dicomNodeConfigRepo.findById(5L)).thenReturn(Optional.of(existing));
		when(dicomNodeConfigRepo.save(any(DicomNodeConfigEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		var updated = dicomNodeConfigService.updateNode(5L, "New", "NEW", "new.host", 200, "PACS", "SiteB");

		assertEquals("NEW", updated.getAet());
		assertEquals("PACS", existing.getNodeType());
		assertEquals("SiteB", existing.getNodeGroup());
		assertEquals(200, updated.getPort());
	}

	@Test
	void update_node_quick_overload_preserves_type_and_group() {
		var existing = node("Old", "OLD", "old.host", 100, "WORKLIST", "SiteA");
		existing.setId(5L);
		when(dicomNodeConfigRepo.findById(5L)).thenReturn(Optional.of(existing));
		when(dicomNodeConfigRepo.save(any(DicomNodeConfigEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		dicomNodeConfigService.updateNode(5L, "New", "NEW", "new.host", 200);

		assertEquals("WORKLIST", existing.getNodeType());
		assertEquals("SiteA", existing.getNodeGroup());
	}

	@Test
	void update_node_throws_when_id_is_unknown() {
		when(dicomNodeConfigRepo.findById(99L)).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class,
				() -> dicomNodeConfigService.updateNode(99L, "New", "NEW", "new.host", 200, "PACS", null));
	}

	@Test
	void delete_node_delegates_to_repository() {
		dicomNodeConfigService.deleteNode(3L);

		verify(dicomNodeConfigRepo).deleteById(3L);
	}

	@Test
	void rename_group_moves_its_nodes_and_returns_the_count() {
		var node = node("n", "AET", "h", 11112, "WORKSTATION", "OLD");
		when(dicomNodeConfigRepo.findByNodeGroup("OLD")).thenReturn(List.of(node));

		int moved = dicomNodeConfigService.renameGroup("OLD", "NEW");

		assertEquals(1, moved);
		assertEquals("NEW", node.getNodeGroup());
		verify(dicomNodeConfigRepo).save(node);
	}

	@Test
	void delete_group_removes_its_nodes_and_returns_the_count() {
		var node = node("n", "AET", "h", 11112, "WORKSTATION", "PACS_WEB");
		when(dicomNodeConfigRepo.findByNodeGroup("PACS_WEB")).thenReturn(List.of(node));

		int removed = dicomNodeConfigService.deleteGroup("PACS_WEB");

		assertEquals(1, removed);
		verify(dicomNodeConfigRepo).deleteAll(List.of(node));
	}

	@Test
	void delete_group_rejects_only_the_dynamic_gateway_destinations_group() {
		assertThrows(IllegalArgumentException.class, () -> dicomNodeConfigService.deleteGroup("Gateway destinations"));
	}

	@Test
	void import_forces_every_row_into_the_target_group() {
		when(dicomNodeConfigRepo.findByNodeGroup("SiteA")).thenReturn(List.of());
		when(dicomNodeConfigRepo.save(any(DicomNodeConfigEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		var csv = """
				description,aetitle,hostname,port,nodeType,nodeGroup
				Public,DICOMSERVER,dicomserver.co.uk,11112,WORKSTATION,Ignored
				""";

		var report = dicomNodeConfigService.importCsv(stream(csv), ',', "SiteA", false);

		assertEquals(1, report.imported());
		assertTrue(report.errors().isEmpty());
		var captor = ArgumentCaptor.forClass(DicomNodeConfigEntity.class);
		verify(dicomNodeConfigRepo).save(captor.capture());
		assertEquals("SiteA", captor.getValue().getNodeGroup());
	}

	@Test
	void import_replace_clears_the_target_group_first() {
		var existing = node("Existing", "OLD", "old.host", 104, "WORKSTATION", "SiteA");
		when(dicomNodeConfigRepo.findByNodeGroup("SiteA")).thenReturn(List.of(existing));
		when(dicomNodeConfigRepo.save(any(DicomNodeConfigEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		var csv = """
				description,aetitle,hostname,port,nodeType,nodeGroup
				New,NEW,new.host,11112,WORKSTATION,SiteA
				""";

		var report = dicomNodeConfigService.importCsv(stream(csv), ',', "SiteA", true);

		assertEquals(1, report.imported());
		assertEquals(1, report.removed());
		verify(dicomNodeConfigRepo).deleteAll(List.of(existing));
	}

	@Test
	void import_maps_legacy_fifth_column_to_the_group() {
		when(dicomNodeConfigRepo.findByNodeGroup("PACS_WEB")).thenReturn(List.of());
		when(dicomNodeConfigRepo.save(any(DicomNodeConfigEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		// legacy 5-column export: the fifth column held the group
		var csv = """
				Public,DICOMSERVER,dicomserver.co.uk,11112,PACS_WEB
				""";

		var report = dicomNodeConfigService.importCsv(stream(csv), ',', null, false);

		assertEquals(1, report.imported());
		var captor = ArgumentCaptor.forClass(DicomNodeConfigEntity.class);
		verify(dicomNodeConfigRepo).save(captor.capture());
		assertEquals("WORKSTATION", captor.getValue().getNodeType());
		assertEquals("PACS_WEB", captor.getValue().getNodeGroup());
	}

	@Test
	void import_legacy_four_column_file_leaves_the_node_ungrouped() {
		when(dicomNodeConfigRepo.findByNodeGroupIsNull()).thenReturn(List.of());
		when(dicomNodeConfigRepo.save(any(DicomNodeConfigEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		var csv = """
				# legacy export
				Legacy,LEG,leg.host,104
				""";

		var report = dicomNodeConfigService.importCsv(stream(csv), ',', null, false);

		assertEquals(1, report.imported());
		var captor = ArgumentCaptor.forClass(DicomNodeConfigEntity.class);
		verify(dicomNodeConfigRepo).save(captor.capture());
		assertNull(captor.getValue().getNodeGroup());
		assertEquals("WORKSTATION", captor.getValue().getNodeType());
	}

	@Test
	void import_skips_duplicates_already_present_in_the_group() {
		var existing = node("Karnak", "KARNAK", "localhost", 11112, "WORKSTATION", null);
		when(dicomNodeConfigRepo.findByNodeGroupIsNull()).thenReturn(List.of(existing));

		var csv = """
				Karnak,KARNAK,localhost,11112
				""";

		var report = dicomNodeConfigService.importCsv(stream(csv), ',', null, false);

		assertEquals(0, report.imported());
		verify(dicomNodeConfigRepo, never()).save(any(DicomNodeConfigEntity.class));
	}

	@Test
	void import_skips_a_worklist_duplicate_already_present_in_the_group() {
		var existing = node("Existing", "ADVT", "dicomserver.co.uk", 104, "WORKLIST", null);
		when(dicomNodeConfigRepo.findByNodeGroupIsNull()).thenReturn(List.of(existing));

		var csv = """
				Public,ADVT,dicomserver.co.uk,104,WORKLIST
				""";

		var report = dicomNodeConfigService.importCsv(stream(csv), ',', null, false);

		assertEquals(0, report.imported());
		verify(dicomNodeConfigRepo, never()).save(any(DicomNodeConfigEntity.class));
	}

	@Test
	void import_reports_invalid_rows_without_aborting() {
		var csv = """
				Bad,BAD,bad.host,notaport
				""";

		var report = dicomNodeConfigService.importCsv(stream(csv), ',', null, false);

		assertEquals(0, report.imported());
		assertEquals(1, report.errors().size());
		assertTrue(report.errors().getFirst().contains("port"));
	}

	@Test
	void import_rejects_the_dynamic_group_as_a_target() {
		assertThrows(IllegalArgumentException.class,
				() -> dicomNodeConfigService.importCsv(stream(""), ',', "Gateway destinations", false));
	}

	@Test
	void export_writes_the_six_column_header_and_rows() {
		var node = node("Karnak", "KARNAK-GATEWAY", "localhost", 11112, "WORKSTATION", "SiteA");
		when(dicomNodeConfigRepo.findAll()).thenReturn(List.of(node));

		var csv = new String(dicomNodeConfigService.exportCsv(null), StandardCharsets.UTF_8);

		assertTrue(csv.contains("\"description\",\"aetitle\",\"hostname\",\"port\",\"nodeType\",\"nodeGroup\""));
		assertTrue(csv.contains("\"KARNAK-GATEWAY\",\"localhost\",\"11112\",\"WORKSTATION\",\"SiteA\""));
	}

	private static ByteArrayInputStream stream(String csv) {
		return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
	}

}
