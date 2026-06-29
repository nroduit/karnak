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
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.data.entity.WebDestinationConfigEntity;
import org.karnak.backend.data.repo.WebDestinationConfigRepo;
import org.karnak.backend.enums.DicomWebServiceType;
import org.karnak.backend.model.dicom.WebDestinationNode;
import org.karnak.backend.util.CsvParseResult;
import org.karnak.backend.util.DicomNodeUtil;
import org.karnak.backend.util.WebDestinationCsvCodec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD for the persisted DICOMweb endpoints. The probed services are stored as a
 * comma-separated list of {@link DicomWebServiceType} names; an empty list means "all".
 * Groups are implicit (the distinct {@code group_name} values in use). The dynamic
 * {@link DicomNodeUtil#GATEWAY_DESTINATIONS_GROUP_NAME "Gateway destinations"} group is
 * computed from the gateway STOW-RS destinations and cannot be used as a persisted group.
 */
@Service
@NullUnmarked
public class WebDestinationConfigService {

	private final WebDestinationConfigRepo repository;

	@Autowired
	public WebDestinationConfigService(WebDestinationConfigRepo repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public List<WebDestinationConfigEntity> findAll() {
		return repository.findAll();
	}

	/**
	 * @param group the organizational group to filter on, or null for every endpoint
	 * @return the persisted endpoints, optionally restricted to a single group
	 */
	@Transactional(readOnly = true)
	public List<WebDestinationConfigEntity> findAll(@Nullable String group) {
		return (group == null) ? findAll() : repository.findByGroupName(group);
	}

	/**
	 * @return the organizational groups already in use, ordered alphabetically
	 */
	@Transactional(readOnly = true)
	public List<String> getKnownGroups() {
		return repository.findDistinctGroupNames();
	}

	@Transactional
	public WebDestinationConfigEntity save(String description, String url, Set<DicomWebServiceType> services,
			@Nullable String group) {
		rejectReservedGroup(group);
		return repository.save(new WebDestinationConfigEntity(emptyToNull(description), url, encodeServices(services),
				emptyToNull(group)));
	}

	@Transactional
	public WebDestinationConfigEntity update(Long id, String description, String url, Set<DicomWebServiceType> services,
			@Nullable String group) {
		rejectReservedGroup(group);
		var entity = repository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("No DICOMweb endpoint with id " + id));
		entity.setDescription(emptyToNull(description));
		entity.setUrl(url);
		entity.setServices(encodeServices(services));
		entity.setGroupName(emptyToNull(group));
		return repository.save(entity);
	}

	@Transactional
	public void delete(Long id) {
		repository.deleteById(id);
	}

	/**
	 * Import endpoints from a CSV document (see
	 * {@link DicomNodeConfigService#importCsv}). Rows whose {@code url} already exists -
	 * in the database or earlier in the same document - are skipped. Rows that fail
	 * validation are reported in {@link ImportReport#errors()}.
	 * @return how many endpoints were removed (by the replace) and persisted, with
	 * per-row errors
	 */
	@Transactional
	public ImportReport importCsv(InputStream inputStream, char separator, @Nullable String targetGroup,
			boolean replace) {
		rejectReservedGroup(targetGroup);
		CsvParseResult<WebDestinationConfigEntity> parsed = WebDestinationCsvCodec.parse(inputStream, separator);
		if (targetGroup != null) {
			parsed.entities().forEach((entity) -> entity.setGroupName(targetGroup));
		}

		int removed = replace ? clearScope(targetGroup) : 0;

		Set<String> seen = new HashSet<>();
		int imported = 0;
		for (WebDestinationConfigEntity entity : parsed.entities()) {
			if (!seen.add(entity.getUrl()) || (!replace && repository.existsByUrl(entity.getUrl()))) {
				continue;
			}
			repository.save(entity);
			imported++;
		}
		return new ImportReport(imported, removed, parsed.errors());
	}

	/** Export the endpoints (optionally a single group) as a CSV document. */
	@Transactional(readOnly = true)
	public byte[] exportCsv(@Nullable String group) {
		return WebDestinationCsvCodec.export(findAll(group));
	}

	/**
	 * Adapt a persisted endpoint to the {@link WebDestinationNode} the check service
	 * consumes.
	 */
	public WebDestinationNode toWebDestinationNode(WebDestinationConfigEntity entity) {
		String description = (entity.getDescription() != null && !entity.getDescription().isBlank())
				? entity.getDescription() : entity.getUrl();
		return new WebDestinationNode(description, entity.getUrl(), null);
	}

	/**
	 * @return the services a stored endpoint should be probed for (empty/blank means
	 * "all")
	 */
	public static Set<DicomWebServiceType> decodeServices(@Nullable String services) {
		Set<DicomWebServiceType> result = EnumSet.noneOf(DicomWebServiceType.class);
		if (services == null || services.isBlank()) {
			return result;
		}
		for (String token : services.split(",")) {
			String name = token.trim();
			if (name.isEmpty()) {
				continue;
			}
			try {
				result.add(DicomWebServiceType.valueOf(name));
			}
			catch (IllegalArgumentException ex) {
				// ignore unknown service names
			}
		}
		return result;
	}

	public static String encodeServices(Set<DicomWebServiceType> services) {
		return services.stream().map(Enum::name).collect(Collectors.joining(","));
	}

	private int clearScope(@Nullable String targetGroup) {
		var entities = (targetGroup == null) ? repository.findAll() : repository.findByGroupName(targetGroup);
		repository.deleteAll(entities);
		return entities.size();
	}

	private void rejectReservedGroup(@Nullable String group) {
		if (group != null && DicomNodeUtil.GATEWAY_DESTINATIONS_GROUP_NAME.equalsIgnoreCase(group.trim())) {
			throw new IllegalArgumentException("\"" + group + "\" is a reserved group and cannot be used");
		}
	}

	private static @Nullable String emptyToNull(@Nullable String value) {
		return (value == null || value.isBlank()) ? null : value.trim();
	}

	/**
	 * Outcome of a CSV import.
	 *
	 * @param imported how many endpoints were persisted
	 * @param removed how many existing endpoints were deleted first (0 unless replacing)
	 * @param errors per-row messages for rows skipped or adjusted by validation
	 */
	public record ImportReport(int imported, int removed, List<String> errors) {
	}

}
