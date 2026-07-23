/*
 * Copyright (c) 2024-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.DicomSourceNodeEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.service.DestinationService;
import org.karnak.backend.service.ForwardNodeAPIService;
import org.karnak.backend.service.ForwardNodeService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ForwardNodeControllerTest {

	private ForwardNodeAPIService forwardNodeAPIService;

	private ForwardNodeService forwardNodeService;

	private DestinationService destinationService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		forwardNodeAPIService = Mockito.mock(ForwardNodeAPIService.class);
		forwardNodeService = Mockito.mock(ForwardNodeService.class);
		destinationService = Mockito.mock(DestinationService.class);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new ForwardNodeController(forwardNodeAPIService, forwardNodeService, destinationService))
			.build();
	}

	@Test
	void getAllForwardNodes_returns_204_when_empty() throws Exception {
		when(forwardNodeService.getAllForwardNodes()).thenReturn(List.of());
		mockMvc.perform(get("/api/forward-nodes")).andExpect(status().isNoContent());
	}

	@Test
	void getAllForwardNodes_returns_200_with_list() throws Exception {
		ForwardNodeEntity node = new ForwardNodeEntity("MY_GATEWAY");
		node.setId(1L);
		when(forwardNodeService.getAllForwardNodes()).thenReturn(List.of(node));
		mockMvc.perform(get("/api/forward-nodes"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].fwdAeTitle").value("MY_GATEWAY"));
	}

	@Test
	void createForwardNode_rejects_missing_aetitle() throws Exception {
		mockMvc
			.perform(post("/api/forward-nodes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"fwdDescription\": \"no aet\"}"))
			.andExpect(status().isBadRequest());

		verify(forwardNodeAPIService, never()).addForwardNode(any());
	}

	@Test
	void createForwardNode_returns_409_when_aetitle_exists() throws Exception {
		ForwardNodeEntity existing = new ForwardNodeEntity("DUP");
		when(forwardNodeService.getAllForwardNodes()).thenReturn(List.of(existing));

		mockMvc
			.perform(post("/api/forward-nodes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"fwdAeTitle\": \"DUP\"}"))
			.andExpect(status().isConflict());

		verify(forwardNodeAPIService, never()).addForwardNode(any());
	}

	@Test
	void createForwardNode_returns_201() throws Exception {
		when(forwardNodeService.getAllForwardNodes()).thenReturn(List.of());

		mockMvc
			.perform(post("/api/forward-nodes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"fwdAeTitle\": \"GW1\", \"fwdDescription\": \"main\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.fwdAeTitle").value("GW1"));

		verify(forwardNodeAPIService).addForwardNode(any(ForwardNodeEntity.class));
	}

	@Test
	void getForwardNode_returns_404_when_missing() throws Exception {
		when(forwardNodeAPIService.getForwardNodeById(99L)).thenReturn(null);
		mockMvc.perform(get("/api/forward-nodes/99")).andExpect(status().isNotFound());
	}

	/**
	 * Regression test for a previously found bug: a PUT with a partial body was wiping
	 * out the existing source nodes and destinations via cascade=ALL/orphanRemoval. The
	 * controller must merge fwdAeTitle/fwdDescription onto the existing entity and pass
	 * the existing entity to the service so children survive.
	 */
	@Test
	void updateForwardNode_preserves_source_nodes_and_destinations() throws Exception {
		ForwardNodeEntity existing = new ForwardNodeEntity("GW1");
		existing.setId(1L);
		existing.setFwdDescription("old");
		DicomSourceNodeEntity src = new DicomSourceNodeEntity();
		src.setId(10L);
		existing.addSourceNode(src);
		DestinationEntity dest = DestinationEntity.ofDicomEmpty();
		dest.setId(20L);
		existing.addDestination(dest);
		when(forwardNodeAPIService.getForwardNodeById(1L)).thenReturn(existing);

		mockMvc
			.perform(put("/api/forward-nodes/1").contentType(MediaType.APPLICATION_JSON)
				.content("{\"fwdAeTitle\": \"GW1\", \"fwdDescription\": \"new\"}"))
			.andExpect(status().isOk());

		ArgumentCaptor<ForwardNodeEntity> captor = ArgumentCaptor.forClass(ForwardNodeEntity.class);
		verify(forwardNodeAPIService).updateForwardNode(captor.capture());
		ForwardNodeEntity saved = captor.getValue();
		assertEquals(1L, saved.getId());
		assertEquals("new", saved.getFwdDescription());
		assertEquals(1, saved.getSourceNodes().size(), "source nodes must be preserved");
		assertEquals(1, saved.getDestinationEntities().size(), "destinations must be preserved");
		assertNotNull(saved.getSourceNodes().iterator().next().getId());
		assertNotNull(saved.getDestinationEntities().iterator().next().getId());
	}

	@Test
	void updateForwardNode_returns_404_when_missing() throws Exception {
		when(forwardNodeAPIService.getForwardNodeById(2L)).thenReturn(null);

		mockMvc
			.perform(put("/api/forward-nodes/2").contentType(MediaType.APPLICATION_JSON)
				.content("{\"fwdAeTitle\": \"X\"}"))
			.andExpect(status().isNotFound());
	}

	@Test
	void deleteForwardNode_returns_204() throws Exception {
		ForwardNodeEntity existing = new ForwardNodeEntity("X");
		existing.setId(3L);
		when(forwardNodeAPIService.getForwardNodeById(3L)).thenReturn(existing);

		mockMvc.perform(delete("/api/forward-nodes/3")).andExpect(status().isNoContent());
		verify(forwardNodeAPIService).deleteForwardNode(existing);
	}

	@Test
	void addSourceNode_returns_404_when_forward_node_missing() throws Exception {
		when(forwardNodeAPIService.getForwardNodeById(7L)).thenReturn(null);
		mockMvc
			.perform(post("/api/forward-nodes/7/source-nodes").contentType(MediaType.APPLICATION_JSON)
				.content("{\"aeTitle\": \"SRC\"}"))
			.andExpect(status().isNotFound());
	}

	@Test
	void addDestination_returns_201_and_returns_saved_entity() throws Exception {
		ForwardNodeEntity node = new ForwardNodeEntity("GW");
		node.setId(5L);
		when(forwardNodeAPIService.getForwardNodeById(5L)).thenReturn(node);
		when(destinationService.save(any(), any(DestinationEntity.class))).thenAnswer(inv -> {
			DestinationEntity d = inv.getArgument(1);
			d.setId(123L);
			return d;
		});

		mockMvc
			.perform(post("/api/forward-nodes/5/destinations").contentType(MediaType.APPLICATION_JSON)
				.content("{\"destinationType\": \"dicom\", \"aeTitle\": \"D\", \"hostname\": \"h\", \"port\": 11112}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(123));
	}

	@Test
	void deleteDestination_returns_404_when_destination_missing() throws Exception {
		ForwardNodeEntity node = new ForwardNodeEntity("GW");
		node.setId(5L);
		when(forwardNodeAPIService.getForwardNodeById(5L)).thenReturn(node);
		when(forwardNodeService.getDestinationById(node, 99L)).thenReturn(null);

		mockMvc.perform(delete("/api/forward-nodes/5/destinations/99")).andExpect(status().isNotFound());
	}

}
