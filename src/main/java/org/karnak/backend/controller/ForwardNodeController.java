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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.karnak.backend.constant.EndPoint;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.DicomSourceNodeEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.service.DestinationService;
import org.karnak.backend.service.ForwardNodeAPIService;
import org.karnak.backend.service.ForwardNodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rest controller managing forward nodes, their source nodes, and destinations
 */
@RestController
@RequestMapping(EndPoint.FORWARD_NODES_PATH)
@Tag(name = "ForwardNode", description = "API Endpoints for Forward Nodes")
public class ForwardNodeController {

	private final ForwardNodeAPIService forwardNodeAPIService;

	private final ForwardNodeService forwardNodeService;

	private final DestinationService destinationService;

	@Autowired
	public ForwardNodeController(final ForwardNodeAPIService forwardNodeAPIService,
			final ForwardNodeService forwardNodeService, final DestinationService destinationService) {
		this.forwardNodeAPIService = forwardNodeAPIService;
		this.forwardNodeService = forwardNodeService;
		this.destinationService = destinationService;
	}

	// ======== Forward Nodes ========

	@Operation(summary = "List all forward nodes")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Forward nodes found"),
			@ApiResponse(responseCode = "204", description = "No forward nodes configured", content = @Content) })
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<ForwardNodeEntity>> getAllForwardNodes() {
		List<ForwardNodeEntity> nodes = forwardNodeService.getAllForwardNodes();
		return nodes.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(nodes);
	}

	@Operation(summary = "Create a forward node")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Forward node created"),
			@ApiResponse(responseCode = "400", description = "Missing or invalid fwdAeTitle", content = @Content),
			@ApiResponse(responseCode = "409", description = "AE Title already exists", content = @Content) })
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ForwardNodeEntity> createForwardNode(@RequestBody ForwardNodeEntity forwardNodeEntity) {
		String aeTitle = forwardNodeEntity.getFwdAeTitle();
		if (aeTitle == null || aeTitle.isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		boolean aeExists = forwardNodeService.getAllForwardNodes()
			.stream()
			.anyMatch(f -> Objects.equals(f.getFwdAeTitle(), aeTitle));
		if (aeExists) {
			return ResponseEntity.status(409).build();
		}
		forwardNodeEntity.setId(null);
		forwardNodeAPIService.addForwardNode(forwardNodeEntity);
		return ResponseEntity.status(201).body(forwardNodeEntity);
	}

	@Operation(summary = "Get a forward node by ID")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Forward node found"),
			@ApiResponse(responseCode = "404", description = "Forward node not found", content = @Content) })
	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ForwardNodeEntity> getForwardNode(@PathVariable Long id) {
		ForwardNodeEntity node = forwardNodeAPIService.getForwardNodeById(id);
		return node == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(node);
	}

	@Operation(summary = "Update a forward node",
			description = "Only fwdAeTitle and fwdDescription are updatable. "
					+ "Source nodes and destinations are preserved (manage them via their own endpoints).")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Forward node updated"),
			@ApiResponse(responseCode = "404", description = "Forward node not found", content = @Content) })
	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ForwardNodeEntity> updateForwardNode(@PathVariable Long id,
			@RequestBody ForwardNodeEntity forwardNodeEntity) {
		ForwardNodeEntity existing = forwardNodeAPIService.getForwardNodeById(id);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		// Merge only fwdAeTitle/fwdDescription onto existing entity so that the
		// associated source nodes and destinations (cascade=ALL + orphanRemoval)
		// are not silently wiped out by an incoming partial payload.
		if (forwardNodeEntity.getFwdAeTitle() != null && !forwardNodeEntity.getFwdAeTitle().isBlank()) {
			existing.setFwdAeTitle(forwardNodeEntity.getFwdAeTitle());
		}
		if (forwardNodeEntity.getFwdDescription() != null) {
			existing.setFwdDescription(forwardNodeEntity.getFwdDescription());
		}
		forwardNodeAPIService.updateForwardNode(existing);
		return ResponseEntity.ok(existing);
	}

	@Operation(summary = "Delete a forward node")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Forward node deleted"),
			@ApiResponse(responseCode = "404", description = "Forward node not found", content = @Content) })
	@DeleteMapping(value = "/{id}")
	public ResponseEntity<Void> deleteForwardNode(@PathVariable Long id) {
		ForwardNodeEntity existing = forwardNodeAPIService.getForwardNodeById(id);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		forwardNodeAPIService.deleteForwardNode(existing);
		return ResponseEntity.noContent().build();
	}

	// ======== Source Nodes ========

	@Operation(summary = "List source nodes of a forward node")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Source nodes found"),
			@ApiResponse(responseCode = "404", description = "Forward node not found", content = @Content) })
	@GetMapping(value = "/{id}/source-nodes", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Collection<DicomSourceNodeEntity>> getSourceNodes(@PathVariable Long id) {
		ForwardNodeEntity node = forwardNodeAPIService.getForwardNodeById(id);
		if (node == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(forwardNodeService.getAllSourceNodes(node));
	}

	@Operation(summary = "Add a source node to a forward node")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Source node added"),
			@ApiResponse(responseCode = "404", description = "Forward node not found", content = @Content) })
	@PostMapping(value = "/{id}/source-nodes", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<DicomSourceNodeEntity> addSourceNode(@PathVariable Long id,
			@RequestBody DicomSourceNodeEntity sourceNode) {
		ForwardNodeEntity node = forwardNodeAPIService.getForwardNodeById(id);
		if (node == null) {
			return ResponseEntity.notFound().build();
		}
		sourceNode.setId(null);
		forwardNodeService.updateSourceNode(node, sourceNode);
		return ResponseEntity.status(201).body(sourceNode);
	}

	@Operation(summary = "Remove a source node from a forward node")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Source node removed"),
			@ApiResponse(responseCode = "404", description = "Forward node or source node not found",
					content = @Content) })
	@DeleteMapping(value = "/{id}/source-nodes/{sourceNodeId}")
	public ResponseEntity<Void> deleteSourceNode(@PathVariable Long id, @PathVariable Long sourceNodeId) {
		ForwardNodeEntity node = forwardNodeAPIService.getForwardNodeById(id);
		if (node == null) {
			return ResponseEntity.notFound().build();
		}
		DicomSourceNodeEntity sourceNode = forwardNodeService.getSourceNodeById(node, sourceNodeId);
		if (sourceNode == null) {
			return ResponseEntity.notFound().build();
		}
		forwardNodeService.deleteSourceNode(node, sourceNode);
		return ResponseEntity.noContent().build();
	}

	// ======== Destinations ========

	@Operation(summary = "List destinations of a forward node")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Destinations found"),
			@ApiResponse(responseCode = "404", description = "Forward node not found", content = @Content) })
	@GetMapping(value = "/{id}/destinations", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Collection<DestinationEntity>> getDestinations(@PathVariable Long id) {
		ForwardNodeEntity node = forwardNodeAPIService.getForwardNodeById(id);
		if (node == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(destinationService.retrieveDestinations(node));
	}

	@Operation(summary = "Add a destination to a forward node")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Destination added"),
			@ApiResponse(responseCode = "404", description = "Forward node not found", content = @Content) })
	@PostMapping(value = "/{id}/destinations", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<DestinationEntity> addDestination(@PathVariable Long id,
			@RequestBody DestinationEntity destinationEntity) {
		ForwardNodeEntity node = forwardNodeAPIService.getForwardNodeById(id);
		if (node == null) {
			return ResponseEntity.notFound().build();
		}
		destinationEntity.setId(null);
		DestinationEntity saved = destinationService.save(node, destinationEntity);
		return ResponseEntity.status(201).body(saved);
	}

	@Operation(summary = "Update a destination of a forward node")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Destination updated"),
			@ApiResponse(responseCode = "404", description = "Forward node or destination not found",
					content = @Content) })
	@PutMapping(value = "/{id}/destinations/{destinationId}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<DestinationEntity> updateDestination(@PathVariable Long id, @PathVariable Long destinationId,
			@RequestBody DestinationEntity destinationEntity) {
		ForwardNodeEntity node = forwardNodeAPIService.getForwardNodeById(id);
		if (node == null) {
			return ResponseEntity.notFound().build();
		}
		DestinationEntity existing = forwardNodeService.getDestinationById(node, destinationId);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		destinationEntity.setId(destinationId);
		DestinationEntity saved = destinationService.save(node, destinationEntity);
		return ResponseEntity.ok(saved);
	}

	@Operation(summary = "Delete a destination from a forward node")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Destination deleted"),
			@ApiResponse(responseCode = "404", description = "Forward node or destination not found",
					content = @Content) })
	@DeleteMapping(value = "/{id}/destinations/{destinationId}")
	public ResponseEntity<Void> deleteDestination(@PathVariable Long id, @PathVariable Long destinationId) {
		ForwardNodeEntity node = forwardNodeAPIService.getForwardNodeById(id);
		if (node == null) {
			return ResponseEntity.notFound().build();
		}
		DestinationEntity existing = forwardNodeService.getDestinationById(node, destinationId);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		destinationService.delete(existing);
		return ResponseEntity.noContent().build();
	}

}
