/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import java.util.List;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.AuthConfigEntity;
import org.karnak.backend.data.repo.AuthConfigRepo;
import org.karnak.frontend.util.CollatorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Authentication configuration service
 */
@Service
@NullUnmarked
public class AuthConfigService {

	private final AuthConfigRepo authConfigRepo;

	@Autowired
	public AuthConfigService(final AuthConfigRepo authConfigRepo) {
		this.authConfigRepo = authConfigRepo;
	}

	/**
	 * Retrieve the identifiers (codes) of all the stored authentication configurations,
	 * sorted alphabetically so they read consistently in a selection combo.
	 * @return the list of authentication-config identifiers
	 */
	public List<String> getAllAuthConfigCodes() {
		return authConfigRepo.findAll()
			.stream()
			.map(AuthConfigEntity::getCode)
			.sorted(CollatorUtils.stringComparator())
			.toList();
	}

}