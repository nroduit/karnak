/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.dicom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.ServerSocket;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.karnak.backend.data.entity.AuthConfigEntity;
import org.karnak.backend.data.repo.AuthConfigRepo;
import org.karnak.backend.model.dicom.result.AuthCheckResult;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(ReplaceUnderscores.class)
class WebTokenServiceTest {

	@Mock
	private AuthConfigRepo authConfigRepo;

	@Test
	void unknown_auth_configuration_is_reported_as_not_defined() {
		when(authConfigRepo.findByCode("missing")).thenReturn(null);
		WebTokenService service = new WebTokenService(authConfigRepo);

		AuthCheckResult result = service.checkToken("missing");

		assertEquals("missing", result.authConfig());
		assertFalse(result.acquired());
		assertNotNull(result.error());
		assertTrue(result.error().contains("not defined"));
	}

	@Test
	void unreachable_token_endpoint_is_reported_as_failure() throws IOException {
		AuthConfigEntity entity = new AuthConfigEntity();
		entity.setClientId("client");
		entity.setClientSecret("secret");
		entity.setScope("read");
		entity.setAccessTokenUrl("http://127.0.0.1:" + closedPort() + "/token");
		when(authConfigRepo.findByCode("idp")).thenReturn(entity);
		WebTokenService service = new WebTokenService(authConfigRepo);

		AuthCheckResult result = service.checkToken("idp");

		assertFalse(result.acquired());
		assertNotNull(result.error());
	}

	private static int closedPort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

}
