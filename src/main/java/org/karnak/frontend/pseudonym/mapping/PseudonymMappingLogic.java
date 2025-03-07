/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.pseudonym.mapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.karnak.backend.cache.ExternalIDCache;
import org.karnak.backend.cache.Patient;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Mapping logic service use to make calls to backend and implement logic linked to the
 * mapping view
 */
@Service
public class PseudonymMappingLogic {

	// View
	private PseudonymMappingView pseudonymMappingView;

	// Service
	private final ProjectService projectService;

	// Cache
	private final ExternalIDCache externalIDCache;

	/**
	 * Autowired constructor
	 * @param externalIDCache External ID Cache
	 * @param projectService Project service
	 */
	@Autowired
	public PseudonymMappingLogic(final ExternalIDCache externalIDCache, final ProjectService projectService) {
		this.externalIDCache = externalIDCache;
		this.projectService = projectService;
		this.pseudonymMappingView = null;
	}

	/**
	 * Retrieve a map of patients by project stored in external id cache which have the
	 * pseudonym in parameter
	 * @param pseudonym Pseudonym
	 * @return Map of patients by project
	 */
	public Map<String, Patient> retrieveExternalIDCachePatients(String pseudonym) {
		Map<String, Patient> externalIDCacheMapping = new HashMap<>();

		// Look for patients in externalID cache corresponding to the input of the
		// user
		List<Patient> patientsFound = externalIDCache.getAll()
			.stream()
			.filter(extId -> Objects.equals(extId.getPseudonym(), pseudonym))
			.toList();

		// Add mapping found
		patientsFound.forEach(p -> {
			Long projectID = p.getProjectID();
			ProjectEntity projectEntity = projectService.retrieveProject(projectID);
			externalIDCacheMapping.put(projectEntity.getName(), p);
		});

		return externalIDCacheMapping;
	}

	// TODO TELIMA-289: retrieve patients in pseudonym-service

	public PseudonymMappingView getMappingView() {
		return pseudonymMappingView;
	}

	public void setMappingView(PseudonymMappingView pseudonymMappingView) {
		this.pseudonymMappingView = pseudonymMappingView;
	}

}
