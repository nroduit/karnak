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
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.karnak.backend.data.entity.WebDestinationConfigEntity;
import org.karnak.backend.data.repo.WebDestinationConfigRepo;
import org.karnak.backend.enums.DicomWebServiceType;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class WebDestinationConfigServiceTest {

	@Mock
	private WebDestinationConfigRepo repository;

	@InjectMocks
	private WebDestinationConfigService service;

	private static WebDestinationConfigEntity endpoint(String url, String services, String group) {
		return new WebDestinationConfigEntity("desc", url, services, group);
	}

	@Test
	void find_all_with_group_filters_on_the_group() {
		var inGroup = endpoint("https://a/dicom-web", "", "SiteA");
		when(repository.findByGroupName("SiteA")).thenReturn(List.of(inGroup));

		assertEquals(List.of(inGroup), service.findAll("SiteA"));
	}

	@Test
	void known_groups_are_the_distinct_group_names() {
		when(repository.findDistinctGroupNames()).thenReturn(List.of("SiteA", "SiteB"));

		assertEquals(List.of("SiteA", "SiteB"), service.getKnownGroups());
	}

	@Test
	void save_encodes_the_services_and_drops_a_blank_group() {
		when(repository.save(any(WebDestinationConfigEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		service.save("Cloud", "https://stow/dicom-web", EnumSet.of(DicomWebServiceType.STOW_RS), "  ");

		var captor = ArgumentCaptor.forClass(WebDestinationConfigEntity.class);
		verify(repository).save(captor.capture());
		assertEquals("STOW_RS", captor.getValue().getServices());
		assertNull(captor.getValue().getGroupName());
	}

	@Test
	void save_rejects_the_dynamic_gateway_group() {
		assertThrows(IllegalArgumentException.class,
				() -> service.save("Cloud", "https://stow/dicom-web", Set.of(), "Gateway destinations"));
		verify(repository, never()).save(any(WebDestinationConfigEntity.class));
	}

	@Test
	void update_mutates_the_existing_endpoint() {
		var existing = endpoint("https://old/dicom-web", "", "SiteA");
		existing.setId(5L);
		when(repository.findById(5L)).thenReturn(Optional.of(existing));
		when(repository.save(any(WebDestinationConfigEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		service.update(5L, "New", "https://new/dicom-web", EnumSet.of(DicomWebServiceType.QIDO_RS), "SiteB");

		assertEquals("https://new/dicom-web", existing.getUrl());
		assertEquals("QIDO_RS", existing.getServices());
		assertEquals("SiteB", existing.getGroupName());
	}

	@Test
	void update_throws_when_id_is_unknown() {
		when(repository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class,
				() -> service.update(99L, "n", "https://x/dicom-web", Set.of(), null));
	}

	@Test
	void delete_delegates_to_the_repository() {
		service.delete(3L);

		verify(repository).deleteById(3L);
	}

	@Test
	void import_forces_the_target_group_and_skips_existing_urls() {
		when(repository.existsByUrl("https://a/dicom-web")).thenReturn(false);
		when(repository.existsByUrl("https://b/dicom-web")).thenReturn(true);
		when(repository.save(any(WebDestinationConfigEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		var csv = """
				description,url,services,group
				A,https://a/dicom-web,STOW-RS,Ignored
				B,https://b/dicom-web,,Ignored
				""";

		var report = service.importCsv(stream(csv), ',', "SiteA", false);

		assertEquals(1, report.imported());
		var captor = ArgumentCaptor.forClass(WebDestinationConfigEntity.class);
		verify(repository).save(captor.capture());
		assertEquals("SiteA", captor.getValue().getGroupName());
	}

	@Test
	void import_replace_clears_the_target_group_first() {
		var existing = endpoint("https://old/dicom-web", "", "SiteA");
		when(repository.findByGroupName("SiteA")).thenReturn(List.of(existing));
		when(repository.save(any(WebDestinationConfigEntity.class)))
			.thenAnswer(invocation -> invocation.getArgument(0));

		var csv = """
				description,url,services,group
				New,https://new/dicom-web,,SiteA
				""";

		var report = service.importCsv(stream(csv), ',', "SiteA", true);

		assertEquals(1, report.imported());
		assertEquals(1, report.removed());
		verify(repository).deleteAll(List.of(existing));
	}

	@Test
	void decode_services_round_trips_and_treats_blank_as_empty() {
		assertTrue(WebDestinationConfigService.decodeServices("").isEmpty());
		assertEquals(EnumSet.of(DicomWebServiceType.STOW_RS, DicomWebServiceType.WADO_RS),
				WebDestinationConfigService.decodeServices("STOW_RS,WADO_RS"));
	}

	@Test
	void to_web_destination_node_falls_back_to_the_url_for_the_description() {
		var entity = new WebDestinationConfigEntity(null, "https://stow/dicom-web", "", null);

		var node = service.toWebDestinationNode(entity);

		assertEquals("https://stow/dicom-web", node.description());
		assertEquals("https://stow/dicom-web", node.url());
		assertNull(node.authConfig());
	}

	private static ByteArrayInputStream stream(String csv) {
		return new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
	}

}
