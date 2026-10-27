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
import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.karnak.backend.constant.ApiVersion;
import org.karnak.backend.constant.EndPoint;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.entity.DicomSourceNodeEntity;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.enums.NodeEventType;
import org.karnak.backend.model.event.NodeEvent;
import org.karnak.backend.model.forwardnode.DestinationMapper;
import org.karnak.backend.model.forwardnode.DestinationModel;
import org.karnak.backend.model.forwardnode.DicomSourceNodeMapper;
import org.karnak.backend.model.forwardnode.DicomSourceNodeModel;
import org.karnak.backend.model.forwardnode.ForwardNodeMapper;
import org.karnak.backend.model.forwardnode.ForwardNodeModel;
import org.karnak.backend.service.DestinationService;
import org.karnak.backend.service.ForwardNodeAPIService;
import org.karnak.backend.service.ForwardNodeService;
import org.karnak.backend.service.ProjectService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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

	private final ProjectService projectService;

	/**
	 * Publishes the {@link NodeEvent}s that keep the gateway routing table of this
	 * instance in step with the database, and bump the gateway version so the other
	 * instances reload. The Vaadin UI publishes the same events after its own writes.
	 */
	private final ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	public ForwardNodeController(final ForwardNodeAPIService forwardNodeAPIService,
			final ForwardNodeService forwardNodeService, final DestinationService destinationService,
			final ProjectService projectService, final ApplicationEventPublisher applicationEventPublisher) {
		this.forwardNodeAPIService = forwardNodeAPIService;
		this.forwardNodeService = forwardNodeService;
		this.destinationService = destinationService;
		this.projectService = projectService;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	// ======== Forward Nodes ========

	@Operation(summary = "List all forward nodes")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Forward nodes found"),
			@ApiResponse(responseCode = "204", description = "No forward nodes configured", content = @Content) })
	@GetMapping(produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyAuthority('collaborator','backend') AND hasAuthority('karnak_read')")
	public ResponseEntity<List<ForwardNodeModel>> retrieveAllForwardNodes() {
		List<ForwardNodeEntity> nodes = forwardNodeService.retrieveAllForwardNodes();
		if (nodes.isEmpty()) {
			return ResponseEntity.noContent().build();
		}
		List<ForwardNodeModel> models = nodes.stream().map(ForwardNodeMapper::toModel).toList();
		return ResponseEntity.ok(models);
	}

	@Operation(summary = "Create a forward node")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Forward node created"),
			@ApiResponse(responseCode = "400", description = "Missing or invalid fwdAeTitle", content = @Content),
			@ApiResponse(responseCode = "409", description = "AE Title already exists", content = @Content) })
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyAuthority('collaborator','backend') AND hasAuthority('karnak_create')")
	public ResponseEntity<ForwardNodeModel> createForwardNode(@Valid @RequestBody ForwardNodeModel forwardNodeModel) {
		if (forwardNodeService.retrieveAllForwardNodes()
			.stream()
			.anyMatch(f -> Objects.equals(f.getFwdAeTitle(), forwardNodeModel.getFwdAeTitle()))) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		ForwardNodeEntity forwardNodeEntity = ForwardNodeMapper.toEntity(forwardNodeModel);
		forwardNodeAPIService.addForwardNode(forwardNodeEntity);
		return ResponseEntity.status(HttpStatus.CREATED).body(ForwardNodeMapper.toModel(forwardNodeEntity));
	}

	@Operation(summary = "Get a forward node by uuid")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Forward node found"),
			@ApiResponse(responseCode = "404", description = "Forward node not found", content = @Content) })
	@GetMapping(value = "/{forwardNodeUuid}", produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyAuthority('collaborator','backend') AND hasAuthority('karnak_read')")
	public ResponseEntity<ForwardNodeModel> retrieveForwardNode(@PathVariable("forwardNodeUuid") UUID forwardNodeUuid) {
		ForwardNodeEntity node = forwardNodeAPIService.retrieveForwardNodeByUuid(forwardNodeUuid);
		return node == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(ForwardNodeMapper.toModel(node));
	}

	@Operation(summary = "Update a forward node",
			description = "Only fwdAeTitle and fwdDescription are updatable. "
					+ "Source nodes and destinations are preserved (manage them via their own endpoints).")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Forward node updated"),
			@ApiResponse(responseCode = "404", description = "Forward node not found", content = @Content) })
	@PutMapping(value = "/{forwardNodeUuid}", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyAuthority('collaborator','backend') AND hasAuthority('karnak_update')")
	public ResponseEntity<ForwardNodeModel> updateForwardNode(@PathVariable("forwardNodeUuid") UUID forwardNodeUuid,
			@RequestBody ForwardNodeModel forwardNodeModel) {
		// This is a partial update, so the payload cannot be @Valid: one that only
		// changes the description carries no AETitle, which @NotBlank would reject. The
		// fields that are present are held to the constraints of the creation endpoint
		// instead - in particular the AETitle, which ends up in the DICOM gateway and
		// its logs. Checked before the lookup: a malformed payload is a bad request
		// whether or not the node it names exists.
		if (violatesConstraint(forwardNodeModel.getFwdAeTitle(), ForwardNodeModel.AE_TITLE)
				|| violatesConstraint(forwardNodeModel.getFwdDescription(), ForwardNodeModel.NO_CONTROL_CHARACTERS)) {
			return ResponseEntity.badRequest().build();
		}
		ForwardNodeEntity existing = forwardNodeAPIService.retrieveForwardNodeByUuid(forwardNodeUuid);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		// Merge only fwdAeTitle/fwdDescription onto existing entity so that the
		// associated source nodes and destinations (cascade=ALL + orphanRemoval)
		// are not silently wiped out by an incoming partial payload.
		if (forwardNodeModel.getFwdAeTitle() != null && !forwardNodeModel.getFwdAeTitle().isBlank()) {
			existing.setFwdAeTitle(forwardNodeModel.getFwdAeTitle());
		}
		if (forwardNodeModel.getFwdDescription() != null) {
			existing.setFwdDescription(forwardNodeModel.getFwdDescription());
		}
		forwardNodeAPIService.updateForwardNode(existing);
		return ResponseEntity.ok(ForwardNodeMapper.toModel(existing));
	}

	@Operation(summary = "Delete a forward node")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Forward node deleted"),
			@ApiResponse(responseCode = "404", description = "Forward node not found", content = @Content) })
	@DeleteMapping(value = "/{forwardNodeUuid}", produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyAuthority('collaborator','backend') AND hasAuthority('karnak_delete')")
	public ResponseEntity<Void> deleteForwardNode(@PathVariable("forwardNodeUuid") UUID forwardNodeUuid) {
		ForwardNodeEntity existing = forwardNodeAPIService.retrieveForwardNodeByUuid(forwardNodeUuid);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		forwardNodeAPIService.deleteForwardNode(existing);
		return ResponseEntity.noContent().build();
	}

	// ======== Source Nodes ========

	@Operation(summary = "List source nodes of a forward node")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Source nodes found"),
			@ApiResponse(responseCode = "204", description = "No source nodes configured", content = @Content),
			@ApiResponse(responseCode = "404", description = "Forward node not found", content = @Content) })
	@GetMapping(value = "/{forwardNodeUuid}/source-nodes", produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyAuthority('collaborator','backend') AND hasAuthority('karnak_read')")
	public ResponseEntity<Collection<DicomSourceNodeModel>> retrieveSourceNodes(
			@PathVariable("forwardNodeUuid") UUID forwardNodeUuid) {
		ForwardNodeEntity node = forwardNodeAPIService.retrieveForwardNodeByUuid(forwardNodeUuid);
		if (node == null) {
			return ResponseEntity.notFound().build();
		}
		if (node.getSourceNodes() == null || node.getSourceNodes().isEmpty()) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(node.getSourceNodes().stream().map(DicomSourceNodeMapper::toModel).toList());
	}

	@Operation(summary = "Add a source node to a forward node")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Source node added"),
			@ApiResponse(responseCode = "404", description = "Forward node not found", content = @Content),
			@ApiResponse(responseCode = "409", description = "Source node with the same characteristics already exists",
					content = @Content) })
	@PostMapping(value = "/{forwardNodeUuid}/source-nodes", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyAuthority('collaborator','backend') AND hasAuthority('karnak_update')")
	public ResponseEntity<DicomSourceNodeModel> addSourceNode(@PathVariable("forwardNodeUuid") UUID forwardNodeUuid,
			@Valid @RequestBody DicomSourceNodeModel sourceNodeModel) {
		ForwardNodeEntity node = forwardNodeAPIService.retrieveForwardNodeByUuid(forwardNodeUuid);
		if (node == null) {
			return ResponseEntity.notFound().build();
		}
		DicomSourceNodeEntity sourceNode = DicomSourceNodeMapper.toEntity(sourceNodeModel);
		if (node.getSourceNodes() != null && node.getSourceNodes().contains(sourceNode)) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}
		forwardNodeService.updateSourceNode(node, sourceNode);
		return ResponseEntity.status(HttpStatus.CREATED).body(DicomSourceNodeMapper.toModel(sourceNode));
	}

	@Operation(summary = "Remove a source node from a forward node")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Source node removed"),
			@ApiResponse(responseCode = "404", description = "Forward node or source node not found",
					content = @Content) })
	@DeleteMapping(value = "/{forwardNodeUuid}/source-nodes/{sourceNodeUuid}",
			produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyAuthority('collaborator','backend') AND hasAuthority('karnak_delete')")
	public ResponseEntity<Void> deleteSourceNode(@PathVariable("forwardNodeUuid") UUID forwardNodeUuid,
			@PathVariable("sourceNodeUuid") UUID sourceNodeUuid) {
		ForwardNodeEntity node = forwardNodeAPIService.retrieveForwardNodeByUuid(forwardNodeUuid);
		if (node == null) {
			return ResponseEntity.notFound().build();
		}
		DicomSourceNodeEntity sourceNode = forwardNodeService.retrieveSourceNodeByUuid(node, sourceNodeUuid);
		if (sourceNode == null) {
			return ResponseEntity.notFound().build();
		}
		forwardNodeService.deleteSourceNode(node, sourceNode);
		// Without the event the instance that served the call keeps accepting this source
		// AE Title until its next reload; see deleteDestination.
		applicationEventPublisher.publishEvent(new NodeEvent(sourceNode, NodeEventType.REMOVE));
		return ResponseEntity.noContent().build();
	}

	// ======== Destinations ========

	@Operation(summary = "List destinations of a forward node")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Destinations found"),
			@ApiResponse(responseCode = "204", description = "No destinations configured", content = @Content),
			@ApiResponse(responseCode = "404", description = "Forward node not found", content = @Content) })
	@GetMapping(value = "/{forwardNodeUuid}/destinations", produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyAuthority('collaborator','backend') AND hasAuthority('karnak_read')")
	public ResponseEntity<Collection<DestinationModel>> retrieveDestinations(
			@PathVariable("forwardNodeUuid") UUID forwardNodeUuid) {
		ForwardNodeEntity node = forwardNodeAPIService.retrieveForwardNodeByUuid(forwardNodeUuid);
		if (node == null) {
			return ResponseEntity.notFound().build();
		}
		Collection<DestinationEntity> destinations = destinationService.retrieveDestinations(node);
		if (destinations == null || destinations.isEmpty()) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(destinations.stream().map(DestinationMapper::toModel).toList());
	}

	@Operation(summary = "Add a destination to a forward node")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Destination added"),
			@ApiResponse(responseCode = "404", description = "Forward node not found", content = @Content) })
	@PostMapping(value = "/{forwardNodeUuid}/destinations", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyAuthority('collaborator','backend') AND hasAuthority('karnak_create')")
	public ResponseEntity<DestinationModel> addDestination(@PathVariable("forwardNodeUuid") UUID forwardNodeUuid,
			@Valid @RequestBody DestinationModel destinationModel) {
		ForwardNodeEntity node = forwardNodeAPIService.retrieveForwardNodeByUuid(forwardNodeUuid);
		if (node == null) {
			return ResponseEntity.notFound().build();
		}
		DestinationEntity destinationEntity = DestinationMapper.toEntity(destinationModel);
		resolveProjects(destinationModel, destinationEntity);
		DestinationEntity saved = destinationService.save(node, destinationEntity);
		return ResponseEntity.status(HttpStatus.CREATED).body(DestinationMapper.toModel(saved));
	}

	@Operation(summary = "Update a destination of a forward node")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Destination updated"),
			@ApiResponse(responseCode = "404", description = "Forward node or destination not found",
					content = @Content) })
	@PutMapping(value = "/{forwardNodeUuid}/destinations/{destinationUuid}",
			consumes = MediaType.APPLICATION_JSON_VALUE, produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyAuthority('collaborator','backend') AND hasAuthority('karnak_update')")
	public ResponseEntity<DestinationModel> updateDestination(@PathVariable("forwardNodeUuid") UUID forwardNodeUuid,
			@PathVariable("destinationUuid") UUID destinationUuid,
			@Valid @RequestBody DestinationModel destinationModel) {
		ForwardNodeEntity node = forwardNodeAPIService.retrieveForwardNodeByUuid(forwardNodeUuid);
		if (node == null) {
			return ResponseEntity.notFound().build();
		}
		DestinationEntity existing = forwardNodeService.retrieveDestinationByUuid(node, destinationUuid);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		DestinationEntity destinationEntity = DestinationMapper.toEntity(destinationModel);
		// toEntity only returns null for a null model; the body is required, so Spring
		// answers a 400 before this handler runs.
		destinationEntity.setId(existing.getId()); // NOSONAR
		destinationEntity.setUuid(existing.getUuid());
		resolveProjects(destinationModel, destinationEntity);
		DestinationEntity saved = destinationService.save(node, destinationEntity);
		return ResponseEntity.ok(DestinationMapper.toModel(saved));
	}

	@Operation(summary = "Delete a destination from a forward node")
	@ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Destination deleted"),
			@ApiResponse(responseCode = "404", description = "Forward node or destination not found",
					content = @Content) })
	@DeleteMapping(value = "/{forwardNodeUuid}/destinations/{destinationUuid}",
			produces = ApiVersion.V1_APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAnyAuthority('collaborator','backend') AND hasAuthority('karnak_delete')")
	public ResponseEntity<Void> deleteDestination(@PathVariable("forwardNodeUuid") UUID forwardNodeUuid,
			@PathVariable("destinationUuid") UUID destinationUuid) {
		ForwardNodeEntity node = forwardNodeAPIService.retrieveForwardNodeByUuid(forwardNodeUuid);
		if (node == null) {
			return ResponseEntity.notFound().build();
		}
		DestinationEntity existing = forwardNodeService.retrieveDestinationByUuid(node, destinationUuid);
		if (existing == null) {
			return ResponseEntity.notFound().build();
		}
		destinationService.delete(existing);
		// A REST delete used to leave the destination in the routing table of the
		// instance that served it, which kept forwarding every instance to a destination
		// that no longer exists, once more for each destination deleted this way. The
		// event removes it from that instance's table and bumps the gateway version, so
		// every other instance reloads.
		applicationEventPublisher.publishEvent(new NodeEvent(existing, NodeEventType.REMOVE));
		return ResponseEntity.noContent().build();
	}

	/**
	 * Whether a value carried by a partial update payload breaks the constraint the
	 * creation endpoint declares for it. An absent value is not a violation: it simply
	 * leaves the persisted one untouched.
	 */
	private static boolean violatesConstraint(String value, String pattern) {
		return value != null && !value.matches(pattern);
	}

	/**
	 * Resolves the de-identification and tag-morphing project uuids carried by the
	 * incoming model into their {@link ProjectEntity} and attaches them to the
	 * destination entity.
	 * @param model the incoming destination model.
	 * @param entity the destination entity to populate.
	 * @throws ResponseStatusException a 400 when a referenced project does not exist.
	 */
	private void resolveProjects(DestinationModel model, DestinationEntity entity) {
		// Both callers pass their required @Valid @RequestBody, so the model is never
		// null.
		if (model.getDeIdentificationProjectUuid() != null) { // NOSONAR
			ProjectEntity project = projectService.retrieveProjectByUuid(model.getDeIdentificationProjectUuid());
			if (project == null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"De-identification project not found: " + model.getDeIdentificationProjectUuid());
			}
			entity.setDeIdentificationProjectEntity(project);
		}
		if (model.getTagMorphingProjectUuid() != null) {
			ProjectEntity project = projectService.retrieveProjectByUuid(model.getTagMorphingProjectUuid());
			if (project == null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Tag morphing project not found: " + model.getTagMorphingProjectUuid());
			}
			entity.setTagMorphingProjectEntity(project);
		}
	}

}
