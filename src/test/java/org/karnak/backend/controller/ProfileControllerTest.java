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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.service.profilepipe.ProfilePipeService;
import org.karnak.frontend.profile.component.errorprofile.ProfileError;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProfileControllerTest {

	private ProfilePipeService profilePipeService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		profilePipeService = Mockito.mock(ProfilePipeService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new ProfileController(profilePipeService)).build();
	}

	@Test
	void getAllProfiles_returns_204_when_empty() throws Exception {
		when(profilePipeService.getAllProfiles()).thenReturn(List.of());
		mockMvc.perform(get("/api/profiles")).andExpect(status().isNoContent());
	}

	@Test
	void getAllProfiles_returns_summary_with_safe_defaults_for_nulls() throws Exception {
		ProfileEntity p = new ProfileEntity();
		p.setName(null); // null fields must be serialized as ""
		p.setVersion(null);
		p.setMinimumKarnakVersion(null);
		p.setByDefault(null);
		when(profilePipeService.getAllProfiles()).thenReturn(List.of(p));

		mockMvc.perform(get("/api/profiles"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].name").value(""))
			.andExpect(jsonPath("$[0].version").value(""))
			.andExpect(jsonPath("$[0].minimumKarnakVersion").value(""))
			.andExpect(jsonPath("$[0].byDefault").value(false));
	}

	@Test
	void uploadProfile_rejects_empty_file() throws Exception {
		MockMultipartFile file = new MockMultipartFile("file", "empty.yml", "text/yaml", new byte[0]);
		mockMvc.perform(multipart("/api/profiles").file(file))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").exists());
		verify(profilePipeService, never()).saveProfilePipe(any(), anyBoolean());
	}

	@Test
	void uploadProfile_rejects_invalid_yaml() throws Exception {
		// Tab characters in YAML are syntactically invalid.
		String invalidYaml = "\tname: oops\n\tversion: nope\n";
		MockMultipartFile file = new MockMultipartFile("file", "bad.yml", "text/yaml", invalidYaml.getBytes());
		mockMvc.perform(multipart("/api/profiles").file(file))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").exists());
		verify(profilePipeService, never()).saveProfilePipe(any(), anyBoolean());
	}

	@Test
	void uploadProfile_returns_422_when_profile_has_validation_errors() throws Exception {
		// Build a minimal valid YAML that gets parsed but whose validation reports
		// errors.
		// NOTE: the top-level key is "profileElements", matching ProfilePipeBody's
		// setter.
		String yaml = "name: P\n" + "version: \"1.0\"\n" + "minimumKarnakVersion: \"1.0.0\"\n" + "profileElements:\n"
				+ "  - name: \"bad\"\n" + "    codename: \"not.a.real.codename\"\n";
		MockMultipartFile file = new MockMultipartFile("file", "p.yml", "text/yaml", yaml.getBytes());

		ProfileElementEntity element = new ProfileElementEntity();
		element.setName("bad");
		ProfileError err = new ProfileError(element);
		err.setError("Cannot find the profile codename: not.a.real.codename");
		when(profilePipeService.validateProfile(any())).thenReturn(List.of(err));

		mockMvc.perform(multipart("/api/profiles").file(file))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.errors[0]").value("bad: Cannot find the profile codename: not.a.real.codename"));

		verify(profilePipeService, never()).saveProfilePipe(any(), anyBoolean());
	}

	@Test
	void getProfile_returns_404_when_missing() throws Exception {
		when(profilePipeService.getAllProfiles()).thenReturn(List.of());
		mockMvc.perform(get("/api/profiles/123")).andExpect(status().isNotFound());
	}

	@Test
	void updateProfile_returns_404_when_missing() throws Exception {
		when(profilePipeService.getAllProfiles()).thenReturn(List.of());
		mockMvc.perform(put("/api/profiles/123").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}"))
			.andExpect(status().isNotFound());
	}

	@Test
	void deleteProfile_returns_204_when_present() throws Exception {
		ProfileEntity p = new ProfileEntity();
		p.setId(1L);
		when(profilePipeService.getAllProfiles()).thenReturn(List.of(p));
		mockMvc.perform(delete("/api/profiles/1")).andExpect(status().isNoContent());
		verify(profilePipeService).deleteProfile(p);
	}

}
