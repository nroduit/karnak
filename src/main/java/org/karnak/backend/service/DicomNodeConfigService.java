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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.data.entity.DicomNodeConfigEntity;
import org.karnak.backend.data.repo.DicomNodeConfigRepo;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.DicomNodeList;
import org.karnak.backend.util.CsvParseResult;
import org.karnak.backend.util.DicomNodeCsvCodec;
import org.karnak.backend.util.DicomNodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.weasis.dicom.param.DicomNode;

/**
 * CRUD for the persisted DICOM node configurations. A node carries a {@code nodeType}
 * (its purpose, e.g. WORKSTATION or the reserved WORKLIST) and an optional
 * {@code nodeGroup} (an organizational group). The two axes are orthogonal: every node,
 * worklist nodes included, is organized by its {@code nodeGroup}. Groups are implicit:
 * they are the distinct {@code nodeGroup} values in use, so they appear and disappear
 * with their nodes. Only the dynamic {@link DicomNodeUtil#GATEWAY_DESTINATIONS_GROUP_NAME
 * "Gateway destinations"} group is computed elsewhere and cannot be used as a persisted
 * group.
 */
@Service
@NullUnmarked
public class DicomNodeConfigService {

	/** Default purpose for nodes that do not pick one explicitly. */
	public static final String NODE_TYPE_WORKSTATION = "WORKSTATION";

	/** Reserved purpose: worklist nodes form a single, non-modifiable group. */
	public static final String NODE_TYPE_WORKLIST = "WORKLIST";

	/** Display name of the reserved worklist group. */
	private static final String WORKLIST_DISPLAY_NAME = "Worklists";

	private final DicomNodeConfigRepo dicomNodeConfigRepo;

	@Autowired
	public DicomNodeConfigService(DicomNodeConfigRepo dicomNodeConfigRepo) {
		this.dicomNodeConfigRepo = dicomNodeConfigRepo;
	}

	// ---------------------------------------------------------------------------------------
	// Management API (used by the DICOM node management section)
	// ---------------------------------------------------------------------------------------

	/**
	 * @return every node configuration (worklist nodes included); worklist nodes carry
	 * the reserved {@value #NODE_TYPE_WORKLIST} type and are filtered out only by the
	 * echo/monitor flows
	 */
	@Transactional(readOnly = true)
	public List<DicomNodeConfigEntity> findAll() {
		return dicomNodeConfigRepo.findAll();
	}

	/**
	 * @param group the organizational group to filter on, or null for every node
	 * @return the node configurations, optionally restricted to a single group
	 */
	@Transactional(readOnly = true)
	public List<DicomNodeConfigEntity> findAll(@Nullable String group) {
		return (group == null) ? findAll() : dicomNodeConfigRepo.findByNodeGroup(group);
	}

	/**
	 * @return the organizational groups already in use, ordered alphabetically
	 */
	@Transactional(readOnly = true)
	public List<String> getKnownGroups() {
		return dicomNodeConfigRepo.findDistinctNodeGroups();
	}

	/**
	 * @return the node purposes to offer in the editor: the reserved
	 * {@value #NODE_TYPE_WORKSTATION} and {@value #NODE_TYPE_WORKLIST} plus any custom
	 * types already in use
	 */
	@Transactional(readOnly = true)
	public List<String> getNodeTypes() {
		Set<String> types = new TreeSet<>();
		types.add(NODE_TYPE_WORKSTATION);
		types.add(NODE_TYPE_WORKLIST);
		for (String type : dicomNodeConfigRepo.findDistinctNodeTypes()) {
			if (type != null && !type.isBlank()) {
				types.add(type);
			}
		}
		return new ArrayList<>(types);
	}

	/**
	 * Persist a new DICOM node configuration.
	 * @return the saved node, with its generated id, type and group set
	 */
	@Transactional
	public ConfigNode saveNode(String description, String aeTitle, String hostname, Integer port, String nodeType,
			@Nullable String nodeGroup) {
		rejectReservedGroup(nodeGroup);
		var entity = new DicomNodeConfigEntity(normalizeDescription(description, aeTitle), aeTitle, hostname, port,
				defaultType(nodeType), emptyToNull(nodeGroup));
		return toConfigNode(dicomNodeConfigRepo.save(entity));
	}

	/** Back-compat quick save: persists a node with no organizational group. */
	@Transactional
	public ConfigNode saveNode(String description, String aeTitle, String hostname, Integer port, String nodeType) {
		return saveNode(description, aeTitle, hostname, port, nodeType, null);
	}

	/**
	 * Update an existing DICOM node configuration, including its purpose and group.
	 * @return the updated node
	 */
	@Transactional
	public ConfigNode updateNode(Long id, String description, String aeTitle, String hostname, Integer port,
			String nodeType, @Nullable String nodeGroup) {
		rejectReservedGroup(nodeGroup);
		var entity = load(id);
		entity.setDescription(normalizeDescription(description, aeTitle));
		entity.setAeTitle(aeTitle);
		entity.setHostname(hostname);
		entity.setPort(port);
		entity.setNodeType(defaultType(nodeType));
		entity.setNodeGroup(emptyToNull(nodeGroup));
		return toConfigNode(dicomNodeConfigRepo.save(entity));
	}

	/** Back-compat update that leaves the node purpose and group unchanged. */
	@Transactional
	public ConfigNode updateNode(Long id, String description, String aeTitle, String hostname, Integer port) {
		var entity = load(id);
		entity.setDescription(normalizeDescription(description, aeTitle));
		entity.setAeTitle(aeTitle);
		entity.setHostname(hostname);
		entity.setPort(port);
		return toConfigNode(dicomNodeConfigRepo.save(entity));
	}

	@Transactional
	public void deleteNode(Long id) {
		dicomNodeConfigRepo.deleteById(id);
	}

	/**
	 * Rename an organizational group, moving all of its nodes to the new name.
	 * @return the number of nodes that were moved
	 */
	@Transactional
	public int renameGroup(String oldGroup, String newGroup) {
		rejectReservedGroup(oldGroup);
		rejectReservedGroup(newGroup);
		var nodes = dicomNodeConfigRepo.findByNodeGroup(oldGroup);
		for (DicomNodeConfigEntity entity : nodes) {
			entity.setNodeGroup(emptyToNull(newGroup));
			dicomNodeConfigRepo.save(entity);
		}
		return nodes.size();
	}

	/**
	 * Delete an organizational group together with all of its nodes.
	 * @return the number of nodes that were removed
	 */
	@Transactional
	public int deleteGroup(String group) {
		rejectReservedGroup(group);
		var nodes = dicomNodeConfigRepo.findByNodeGroup(group);
		dicomNodeConfigRepo.deleteAll(nodes);
		return nodes.size();
	}

	/**
	 * Import DICOM nodes from a CSV document.
	 *
	 * <p>
	 * When {@code targetGroup} is non-null every imported node is forced into that group
	 * (overriding the file's own group column); when null the file's group column is
	 * used. When {@code replace} is true the import scope is cleared first
	 * ({@code targetGroup}'s nodes, or every managed node when no group is targeted).
	 * Rows whose {@code (AE Title, hostname, port)} already exist - in the database or
	 * earlier in the same document - within their group are skipped. Rows that fail
	 * validation are skipped and reported in {@link ImportReport#errors()} without
	 * aborting the valid rows.
	 * @return how many nodes were removed (by the replace) and persisted, with per-row
	 * errors
	 */
	@Transactional
	public ImportReport importCsv(InputStream inputStream, char separator, @Nullable String targetGroup,
			boolean replace) {
		rejectReservedGroup(targetGroup);
		CsvParseResult<DicomNodeConfigEntity> parsed = DicomNodeCsvCodec.parse(inputStream, separator, targetGroup,
				targetGroup != null);

		int removed = replace ? clearScope(targetGroup) : 0;

		Map<String, Set<String>> keysByGroup = new HashMap<>();
		int imported = 0;
		for (DicomNodeConfigEntity entity : parsed.entities()) {
			String group = entity.getNodeGroup();
			Set<String> keys = keysByGroup.computeIfAbsent(group, g -> replace ? new HashSet<>() : loadExistingKeys(g));
			if (!keys.add(nodeKey(entity.getAeTitle(), entity.getHostname(), entity.getPort()))) {
				continue;
			}
			dicomNodeConfigRepo.save(entity);
			imported++;
		}
		return new ImportReport(imported, removed, parsed.errors());
	}

	/** Export the managed nodes (optionally a single group) as a CSV document. */
	@Transactional(readOnly = true)
	public byte[] exportCsv(@Nullable String group) {
		return DicomNodeCsvCodec.export(findAll(group));
	}

	// ---------------------------------------------------------------------------------------
	// Check-flow API (used to populate the group/node selectors of the check tools)
	// ---------------------------------------------------------------------------------------

	/**
	 * @return one list per organizational group of managed (non-worklist) nodes, ordered
	 * by group name; the ungrouped nodes are returned under the
	 * {@value #NODE_TYPE_WORKSTATION} label
	 */
	@Transactional(readOnly = true)
	public List<DicomNodeList> getAllDicomNodeTypes() {
		return groupByNodeGroup(dicomNodeConfigRepo.findByNodeTypeNot(NODE_TYPE_WORKLIST), NODE_TYPE_WORKSTATION);
	}

	/**
	 * @return one list per organizational group of worklist nodes, ordered by group name;
	 * the ungrouped worklist nodes are returned under the {@value #WORKLIST_DISPLAY_NAME}
	 * label. Worklist nodes are grouped exactly like every other node; the only
	 * difference from {@link #getAllDicomNodeTypes()} is the {@value #NODE_TYPE_WORKLIST}
	 * type filter.
	 */
	@Transactional(readOnly = true)
	public List<DicomNodeList> getWorkListNodeTypes() {
		return groupByNodeGroup(dicomNodeConfigRepo.findByNodeType(NODE_TYPE_WORKLIST), WORKLIST_DISPLAY_NAME);
	}

	/**
	 * Bucket nodes by their organizational group, ordered alphabetically by group name.
	 * @param nodes the nodes to organize
	 * @param ungroupedLabel the bucket name used for nodes that carry no group
	 * @return one {@link DicomNodeList} per group
	 */
	private List<DicomNodeList> groupByNodeGroup(List<DicomNodeConfigEntity> nodes, String ungroupedLabel) {
		var byGroup = new TreeMap<String, DicomNodeList>();
		for (DicomNodeConfigEntity entity : nodes) {
			String group = entity.getNodeGroup();
			String label = (group == null || group.isBlank()) ? ungroupedLabel : group;
			byGroup.computeIfAbsent(label, DicomNodeList::new).add(toConfigNode(entity));
		}
		return new ArrayList<>(byGroup.values());
	}

	// ---------------------------------------------------------------------------------------
	// Internals
	// ---------------------------------------------------------------------------------------

	private DicomNodeConfigEntity load(Long id) {
		return dicomNodeConfigRepo.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No DICOM node configuration with id " + id));
	}

	/**
	 * Deletes the nodes in the import scope: a single group when {@code targetGroup} is
	 * set, otherwise every managed node (worklist nodes included, matching a full
	 * export).
	 * @return the number of nodes deleted
	 */
	private int clearScope(@Nullable String targetGroup) {
		var nodes = (targetGroup == null) ? findAll() : dicomNodeConfigRepo.findByNodeGroup(targetGroup);
		dicomNodeConfigRepo.deleteAll(nodes);
		return nodes.size();
	}

	private Set<String> loadExistingKeys(@Nullable String group) {
		Set<String> keys = new HashSet<>();
		var nodes = (group == null) ? dicomNodeConfigRepo.findByNodeGroupIsNull()
				: dicomNodeConfigRepo.findByNodeGroup(group);
		for (DicomNodeConfigEntity entity : nodes) {
			keys.add(nodeKey(entity.getAeTitle(), entity.getHostname(), entity.getPort()));
		}
		return keys;
	}

	private void rejectReservedGroup(@Nullable String group) {
		if (group == null) {
			return;
		}
		String name = group.trim();
		if (DicomNodeUtil.GATEWAY_DESTINATIONS_GROUP_NAME.equalsIgnoreCase(name)) {
			throw new IllegalArgumentException("\"" + group + "\" is a reserved group and cannot be used");
		}
	}

	private static String defaultType(String nodeType) {
		return (nodeType == null || nodeType.isBlank()) ? NODE_TYPE_WORKSTATION : nodeType.trim();
	}

	private static String nodeKey(String aeTitle, String hostname, Integer port) {
		return aeTitle + '\\' + hostname + '\\' + port;
	}

	private static ConfigNode toConfigNode(DicomNodeConfigEntity entity) {
		var dicomNode = new DicomNode(entity.getAeTitle(), entity.getHostname(), entity.getPort());
		var node = new ConfigNode(normalizeDescription(entity.getDescription(), entity.getAeTitle()), dicomNode);
		node.setId(entity.getId());
		node.setNodeType(entity.getNodeType());
		return node;
	}

	private static String normalizeDescription(String description, String aeTitle) {
		return (description == null || description.isBlank()) ? aeTitle : description;
	}

	private static @Nullable String emptyToNull(@Nullable String value) {
		return (value == null || value.isBlank()) ? null : value.trim();
	}

	/**
	 * Outcome of a CSV import.
	 *
	 * @param imported how many nodes were persisted
	 * @param removed how many existing nodes were deleted first (0 unless replacing)
	 * @param errors per-row messages for rows skipped by validation
	 */
	public record ImportReport(int imported, int removed, List<String> errors) {
	}

}
