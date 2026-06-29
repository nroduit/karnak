/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.enums.DestinationType;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.DicomNodeList;
import org.karnak.backend.model.dicom.WebDestinationNode;
import org.karnak.backend.service.DicomNodeConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.weasis.core.util.StringUtil;
import org.weasis.dicom.param.DicomNode;
import org.weasis.dicom.param.DicomNodeSource;

@Component
public class DicomNodeUtil {

	/**
	 * Display name of the dynamic group listing every DICOM destination configured in the
	 * gateway. This group is computed on the fly and is therefore read-only (its nodes
	 * cannot be edited or deleted as DICOM node configurations, and the group itself
	 * cannot be renamed or removed).
	 */
	public static final String GATEWAY_DESTINATIONS_GROUP_NAME = "Gateway destinations";

	private final DicomNodeConfigService dicomNodeConfigService;

	private final DestinationRepo destinationRepo;

	private final List<DicomNodeSource> dicomNodeSources;

	@Autowired
	public DicomNodeUtil(DicomNodeConfigService dicomNodeConfigService, DestinationRepo destinationRepo,
			List<DicomNodeSource> dicomNodeSources) {
		this.dicomNodeConfigService = dicomNodeConfigService;
		this.destinationRepo = destinationRepo;
		this.dicomNodeSources = dicomNodeSources;
	}

	/**
	 * @return the dynamic source groups first (e.g. the Gateway destinations), followed
	 * by every user-defined DICOM node group (the reserved worklist group is excluded)
	 */
	public List<DicomNodeList> getAllDicomNodeTypes() {
		var nodeLists = getDynamicNodeGroups();
		nodeLists.addAll(dicomNodeConfigService.getAllDicomNodeTypes());
		return nodeLists;
	}

	/**
	 * Build the read-only groups exposed by the registered {@link DicomNodeSource}s (the
	 * Gateway destinations, plus any source contributed by another module). The nodes
	 * carry no configuration id, so they are treated as read-only by the DICOM node
	 * management UI.
	 * @return one group per source, in source order, each possibly empty
	 */
	public List<DicomNodeList> getDynamicNodeGroups() {
		var groups = new ArrayList<DicomNodeList>();
		for (DicomNodeSource source : dicomNodeSources) {
			groups.add(toNodeList(source));
		}
		return groups;
	}

	private static DicomNodeList toNodeList(DicomNodeSource source) {
		var nodeList = new DicomNodeList(source.getGroupName());
		for (DicomNode node : source.getNodes()) {
			String name = StringUtil.hasText(node.getDescription()) ? node.getDescription() : node.getAet();
			var configNode = new ConfigNode(name, node);
			configNode.setNodeType(source.getGroupName());
			nodeList.add(configNode);
		}
		return nodeList;
	}

	/**
	 * Build the dynamic list of all DICOMweb (STOW-RS) destinations configured in the
	 * gateway. Destinations without a request URL are skipped and destinations sharing
	 * the same URL are reported once.
	 * @return the gateway DICOMweb destinations, possibly empty
	 */
	public List<WebDestinationNode> getGatewayStowDestinations() {
		var destinations = new ArrayList<WebDestinationNode>();
		Set<String> seen = new HashSet<>();
		for (DestinationEntity destination : destinationRepo.findAll()) {
			if (destination.getDestinationType() != DestinationType.stow) {
				continue;
			}
			String url = destination.getUrl();
			if (!StringUtil.hasText(url) || !seen.add(url)) {
				continue;
			}
			String description = StringUtil.hasText(destination.getDescription()) ? destination.getDescription() : url;
			String authConfig = StringUtil.hasText(destination.getAuthConfig()) ? destination.getAuthConfig() : null;
			destinations.add(new WebDestinationNode(description, url, authConfig));
		}
		return destinations;
	}

	/**
	 * @return one list per organizational group of worklist nodes (the ungrouped worklist
	 * nodes under the "Worklists" label), mirroring {@link #getAllDicomNodeTypes()}
	 */
	public List<DicomNodeList> getWorkListNodeTypes() {
		return dicomNodeConfigService.getWorkListNodeTypes();
	}

	/**
	 * @return {@link #getAllDicomNodeTypes()} followed by the worklist groups.
	 * Connectivity tools (echo, monitor) check any DICOM node, worklist SCPs included:
	 * C-ECHO and the capabilities probe are type-agnostic, so worklist nodes are offered
	 * alongside every other node rather than being filtered out by their reserved type.
	 */
	public List<DicomNodeList> getAllNodeTypesIncludingWorklist() {
		var nodeLists = getAllDicomNodeTypes();
		nodeLists.addAll(dicomNodeConfigService.getWorkListNodeTypes());
		return nodeLists;
	}

	public ConfigNode saveDicomNode(String description, String aeTitle, String hostname, Integer port,
			String nodeType) {
		return dicomNodeConfigService.saveNode(description, aeTitle, hostname, port, nodeType);
	}

	public ConfigNode updateDicomNode(Long id, String description, String aeTitle, String hostname, Integer port) {
		return dicomNodeConfigService.updateNode(id, description, aeTitle, hostname, port);
	}

	public void deleteDicomNode(Long id) {
		dicomNodeConfigService.deleteNode(id);
	}

}
