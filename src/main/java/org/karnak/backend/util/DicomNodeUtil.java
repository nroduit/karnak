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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.data.repo.DestinationRepo;
import org.karnak.backend.enums.DestinationType;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.DicomNodeList;
import org.karnak.backend.service.DicomNodeConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.weasis.core.util.StringUtil;
import org.weasis.dicom.param.DicomNode;

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

	@Autowired
	public DicomNodeUtil(DicomNodeConfigService dicomNodeConfigService, DestinationRepo destinationRepo) {
		this.dicomNodeConfigService = dicomNodeConfigService;
		this.destinationRepo = destinationRepo;
	}

	/**
	 * @return the dynamic gateway destinations group first, followed by every
	 * user-defined DICOM node group (the reserved worklist group is excluded)
	 */
	public List<DicomNodeList> getAllDicomNodeTypes() {
		var nodeLists = new ArrayList<DicomNodeList>();
		nodeLists.add(getGatewayDestinationNodes());
		nodeLists.addAll(dicomNodeConfigService.getAllDicomNodeTypes());
		return nodeLists;
	}

	/**
	 * Build the dynamic group of all DICOM destinations configured in the gateway.
	 * STOW-RS destinations have no AE Title / host / port and are skipped, and
	 * destinations sharing the same AE Title, host and port are reported once. The
	 * returned nodes carry no configuration id so they are treated as read-only by the
	 * DICOM node management UI.
	 * @return the gateway destinations group, possibly empty
	 */
	public DicomNodeList getGatewayDestinationNodes() {
		var nodeList = new DicomNodeList(GATEWAY_DESTINATIONS_GROUP_NAME);
		Set<String> seen = new HashSet<>();
		for (DestinationEntity destination : destinationRepo.findAll()) {
			if (destination.getDestinationType() != DestinationType.dicom) {
				continue;
			}
			String aeTitle = destination.getAeTitle();
			String hostname = destination.getHostname();
			Integer port = destination.getPort();
			if (!StringUtil.hasText(aeTitle) || !StringUtil.hasText(hostname) || port == null || port <= 0) {
				continue;
			}
			if (!seen.add(aeTitle + '\\' + hostname + '\\' + port)) {
				continue;
			}
			String description = StringUtil.hasText(destination.getDescription()) ? destination.getDescription()
					: aeTitle;
			var node = new ConfigNode(description, new DicomNode(aeTitle, hostname, port));
			node.setNodeType(GATEWAY_DESTINATIONS_GROUP_NAME);
			nodeList.add(node);
		}
		return nodeList;
	}

	public DicomNodeList getWorkListNodes() {
		return dicomNodeConfigService.getWorkListNodes();
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

	/** Export every DICOM node from every group (worklists excluded) as CSV. */
	public byte[] exportDicomNodes() {
		return dicomNodeConfigService.exportDicomNodes();
	}

	/** Export every worklist node as CSV. */
	public byte[] exportWorkListNodes() {
		return dicomNodeConfigService.exportWorkListNodes();
	}

	/** Import DICOM nodes from CSV, dispatching each row to its group. */
	public int importDicomNodes(InputStream inputStream, char separator) {
		return dicomNodeConfigService.importDicomNodes(inputStream, separator);
	}

	/** Import worklist nodes from CSV, storing every row as a worklist node. */
	public int importWorkListNodes(InputStream inputStream, char separator) {
		return dicomNodeConfigService.importWorkListNodes(inputStream, separator);
	}

	public List<String> getGroups() {
		return dicomNodeConfigService.getGroups();
	}

	public void createGroup(String name) {
		dicomNodeConfigService.createGroup(name);
	}

	public void renameGroup(String oldName, String newName) {
		dicomNodeConfigService.renameGroup(oldName, newName);
	}

	public int deleteGroup(String name) {
		return dicomNodeConfigService.deleteGroup(name);
	}

}
