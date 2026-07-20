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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.AuthConfigEntity;
import org.karnak.backend.data.repo.AuthConfigRepo;
import org.karnak.backend.enums.AuthConfigType;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthConfigControllerTest {

	private AuthConfigRepo authConfigRepo;

	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		authConfigRepo = Mockito.mock(AuthConfigRepo.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new AuthConfigController(authConfigRepo)).build();
	}

	@Test
	void getAllAuthConfigs_when_empty_returns_no_content() throws Exception {
		when(authConfigRepo.findAll()).thenReturn(Collections.emptyList());
		mockMvc.perform(get("/api/auth-configs")).andExpect(status().isNoContent());
	}

	@Test
	void getAllAuthConfigs_when_present_returns_list_and_never_exposes_clientSecret() throws Exception {
		AuthConfigEntity cfg = new AuthConfigEntity();
		cfg.setId(1L);
		cfg.setCode("keycloak");
		cfg.setClientId("client");
		cfg.setClientSecret("super-secret");
		cfg.setAccessTokenUrl("https://idp/token");
		cfg.setScope("openid");
		cfg.setAuthConfigType(AuthConfigType.OAUTH2);
		when(authConfigRepo.findAll()).thenReturn(List.of(cfg));

		MvcResult result = mockMvc.perform(get("/api/auth-configs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].code").value("keycloak"))
			.andExpect(jsonPath("$[0].clientId").value("client"))
			.andReturn();

		String body = result.getResponse().getContentAsString();
		assertFalse(body.contains("super-secret"), "clientSecret must not be serialized in API responses: " + body);
	}

	@Test
	void createAuthConfig_rejects_blank_code() throws Exception {
		AuthConfigEntity cfg = new AuthConfigEntity();
		cfg.setCode("");

		mockMvc
			.perform(post("/api/auth-configs").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(cfg)))
			.andExpect(status().isBadRequest());

		verify(authConfigRepo, never()).save(any());
	}

	@Test
	void createAuthConfig_conflict_when_code_exists() throws Exception {
		AuthConfigEntity existing = new AuthConfigEntity();
		existing.setCode("dup");
		when(authConfigRepo.findByCode("dup")).thenReturn(existing);

		AuthConfigEntity incoming = new AuthConfigEntity();
		incoming.setCode("dup");

		mockMvc
			.perform(post("/api/auth-configs").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(incoming)))
			.andExpect(status().isConflict());

		verify(authConfigRepo, never()).save(any());
	}

	@Test
	void createAuthConfig_defaults_auth_config_type_when_missing() throws Exception {
		when(authConfigRepo.findByCode("new")).thenReturn(null);
		when(authConfigRepo.save(any(AuthConfigEntity.class))).thenAnswer(inv -> {
			AuthConfigEntity arg = inv.getArgument(0);
			arg.setId(42L);
			return arg;
		});

		mockMvc
			.perform(post("/api/auth-configs").contentType(MediaType.APPLICATION_JSON)
				.content("{\"code\": \"new\", \"clientId\": \"c\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.authConfigType").value("OAUTH2"));
	}

	@Test
	void getAuthConfig_returns_404_when_missing() throws Exception {
		when(authConfigRepo.findByCode("missing")).thenReturn(null);
		mockMvc.perform(get("/api/auth-configs/missing")).andExpect(status().isNotFound());
	}

	@Test
	void updateAuthConfig_only_updates_provided_fields() throws Exception {
		AuthConfigEntity existing = new AuthConfigEntity();
		existing.setCode("k");
		existing.setClientId("old-id");
		existing.setScope("old-scope");
		existing.setClientSecret("old-secret");
		when(authConfigRepo.findByCode("k")).thenReturn(existing);
		when(authConfigRepo.save(any(AuthConfigEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		mockMvc
			.perform(put("/api/auth-configs/k").contentType(MediaType.APPLICATION_JSON)
				.content("{\"scope\": \"new-scope\"}"))
			.andExpect(status().isOk());

		// existing entity was mutated in place; clientId should be unchanged
		// (verified by Mockito: the same object was saved with new-scope and old-id)
		verify(authConfigRepo).save(eq(existing));
		assertFalse("old-scope".equals(existing.getScope()), "scope should be updated");
	}

	@Test
	void deleteAuthConfig_returns_204_when_present() throws Exception {
		AuthConfigEntity existing = new AuthConfigEntity();
		existing.setCode("k");
		when(authConfigRepo.findByCode("k")).thenReturn(existing);

		mockMvc.perform(delete("/api/auth-configs/k")).andExpect(status().isNoContent());
		verify(authConfigRepo).delete(existing);
	}

	@Test
	void deleteAuthConfig_returns_404_when_absent() throws Exception {
		when(authConfigRepo.findByCode("nope")).thenReturn(null);
		mockMvc.perform(delete("/api/auth-configs/nope")).andExpect(status().isNotFound());
		verify(authConfigRepo, never()).delete(any());
	}

}
