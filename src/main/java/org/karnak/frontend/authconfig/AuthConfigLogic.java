/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.authconfig;

import com.vaadin.flow.data.provider.ListDataProvider;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.karnak.backend.data.entity.AuthConfigEntity;
import org.karnak.backend.data.repo.AuthConfigRepo;
import org.karnak.backend.enums.AuthConfigType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuthConfigLogic extends ListDataProvider<AuthConfigEntity> {

	private final transient AuthConfigRepo authConfigRepo;

	@Autowired
	public AuthConfigLogic(final AuthConfigRepo authConfigRepo) {
		super(new ArrayList<>());
		this.authConfigRepo = authConfigRepo;
		initDataProvider();
	}

	@Override
	public void refreshAll() {
		getItems().clear();
		getItems().addAll(authConfigRepo.findAll());
		super.refreshAll();
	}

	public boolean contains(String code) {
		return getItems().stream().anyMatch(authConfigEntity -> authConfigEntity.getCode().equals(code));
	}

	private void initDataProvider() {
		getItems().addAll(authConfigRepo.findAll());
	}

	public AuthConfigEntity retrieveAuthConfig(String code) {
		return authConfigRepo.findByCode(code);
	}

	public void deleteAuthConfig(String code) {
		authConfigRepo.delete(authConfigRepo.findByCode(code));
	}

	public void createAuthConfig(String code, AuthConfigType type, AuthConfigEntity data) {
		data.setAuthConfigType(type);
		data.setCode(code);
		authConfigRepo.save(data);
	}

}
