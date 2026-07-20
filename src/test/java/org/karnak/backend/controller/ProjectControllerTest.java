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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.cache.Patient;
import org.karnak.backend.cache.PatientClient;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.entity.SecretEntity;
import org.karnak.backend.service.ProjectService;
import org.karnak.backend.service.profilepipe.ProfilePipeService;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProjectControllerTest {

	private ProjectService projectService;

	private ProfilePipeService profilePipeService;

	private PatientClient patientClient;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		projectService = Mockito.mock(ProjectService.class);
		profilePipeService = Mockito.mock(ProfilePipeService.class);
		patientClient = Mockito.mock(PatientClient.class);
		mockMvc = MockMvcBuilders
			.standaloneSetup(new ProjectController(projectService, profilePipeService, patientClient))
			.build();
	}

	@Test
	void getAllProjects_returns_204_when_empty() throws Exception {
		when(projectService.getAllProjects()).thenReturn(List.of());
		mockMvc.perform(get("/api/projects")).andExpect(status().isNoContent());
	}

	@Test
	void getAllProjects_does_not_expose_secret_key_bytes() throws Exception {
		ProjectEntity project = new ProjectEntity();
		project.setId(1L);
		project.setName("p");
		SecretEntity secret = new SecretEntity(new byte[] { 0x01, 0x02, 0x03, 0x04 });
		secret.setId(7L);
		secret.setActive(true);
		project.addActiveSecretEntity(secret);
		when(projectService.getAllProjects()).thenReturn(List.of(project));

		MvcResult result = mockMvc.perform(get("/api/projects"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].name").value("p"))
			.andReturn();

		String body = result.getResponse().getContentAsString();
		assertFalse(body.contains("secretKey"),
				"Raw HMAC bytes must never be serialized in the project response: " + body);
	}

	@Test
	void createProject_rejects_missing_name() throws Exception {
		mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isBadRequest());

		verify(projectService, never()).save(any());
	}

	@Test
	void createProject_returns_400_for_non_numeric_profile_id() throws Exception {
		mockMvc
			.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"p\", \"profileId\": \"not-a-number\"}"))
			.andExpect(status().isBadRequest());

		verify(projectService, never()).save(any());
	}

	@Test
	void createProject_returns_404_when_profile_missing() throws Exception {
		when(profilePipeService.getAllProfiles()).thenReturn(List.of());
		mockMvc
			.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\": \"p\", \"profileId\": 1}"))
			.andExpect(status().isNotFound());

		verify(projectService, never()).save(any());
	}

	@Test
	void createProject_returns_201_when_no_profile() throws Exception {
		when(projectService.save(any(ProjectEntity.class))).thenAnswer(inv -> {
			ProjectEntity arg = inv.getArgument(0);
			arg.setId(42L);
			return arg;
		});
		mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"p\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(42));
	}

	@Test
	void updateProject_can_detach_profile_via_null() throws Exception {
		ProjectEntity project = new ProjectEntity();
		project.setId(1L);
		project.setName("old");
		ProfileEntity attached = new ProfileEntity();
		attached.setId(9L);
		project.setProfileEntity(attached);
		when(projectService.retrieveProject(1L)).thenReturn(project);

		mockMvc.perform(put("/api/projects/1").contentType(MediaType.APPLICATION_JSON).content("{\"profileId\": null}"))
			.andExpect(status().isOk());

		// The detach happens in-place on the same object that was returned by
		// retrieveProject.
		org.junit.jupiter.api.Assertions.assertNull(project.getProfileEntity(),
				"profileEntity should have been cleared by the null payload");
		verify(projectService).update(project);
	}

	@Test
	void addSecret_rejects_invalid_hex_length() throws Exception {
		ProjectEntity project = new ProjectEntity();
		project.setId(1L);
		when(projectService.retrieveProject(1L)).thenReturn(project);

		mockMvc
			.perform(post("/api/projects/1/secrets").contentType(MediaType.APPLICATION_JSON)
				.content("{\"hexKey\": \"deadbeef\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error").exists());

		verify(projectService, never()).save(any());
	}

	@Test
	void addSecret_rejects_non_hex_characters() throws Exception {
		ProjectEntity project = new ProjectEntity();
		project.setId(1L);
		when(projectService.retrieveProject(1L)).thenReturn(project);

		// 32 chars but contains 'z' which is not hex
		mockMvc
			.perform(post("/api/projects/1/secrets").contentType(MediaType.APPLICATION_JSON)
				.content("{\"hexKey\": \"zzzzbeefdeadbeefdeadbeefdeadbeef\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void addSecret_generates_random_when_no_body() throws Exception {
		ProjectEntity project = new ProjectEntity();
		project.setId(1L);
		when(projectService.retrieveProject(1L)).thenReturn(project);
		when(projectService.save(any(ProjectEntity.class))).thenAnswer(inv -> inv.getArgument(0));

		mockMvc.perform(post("/api/projects/1/secrets").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.hexKey").exists())
			.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void addExternalId_rejects_invalid_birth_date() throws Exception {
		ProjectEntity project = new ProjectEntity();
		project.setId(1L);
		when(projectService.retrieveProject(1L)).thenReturn(project);

		mockMvc
			.perform(post("/api/projects/1/external-ids").contentType(MediaType.APPLICATION_JSON)
				.content("{\"pseudonym\":\"P\",\"patientId\":\"ID\",\"patientBirthDate\":\"not-a-date\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void addExternalId_returns_404_when_project_missing() throws Exception {
		when(projectService.retrieveProject(99L)).thenReturn(null);

		mockMvc
			.perform(post("/api/projects/99/external-ids").contentType(MediaType.APPLICATION_JSON)
				.content("{\"pseudonym\":\"P\",\"patientId\":\"ID\"}"))
			.andExpect(status().isNotFound());
	}

	@Test
	void addExternalId_returns_409_when_patient_exists() throws Exception {
		ProjectEntity project = new ProjectEntity();
		project.setId(1L);
		when(projectService.retrieveProject(1L)).thenReturn(project);
		when(patientClient.put(any(), any())).thenReturn(new Patient("OTHER", "ID", "", "", null, "", ""));

		mockMvc
			.perform(post("/api/projects/1/external-ids").contentType(MediaType.APPLICATION_JSON)
				.content("{\"pseudonym\":\"P\",\"patientId\":\"ID\"}"))
			.andExpect(status().isConflict());
	}

	@Test
	void importExternalIds_skips_invalid_dates_without_500() throws Exception {
		ProjectEntity project = new ProjectEntity();
		project.setId(1L);
		when(projectService.retrieveProject(1L)).thenReturn(project);
		when(patientClient.put(any(), any())).thenReturn(null);

		mockMvc
			.perform(post("/api/projects/1/external-ids/import").contentType(MediaType.APPLICATION_JSON)
				.content("[{\"pseudonym\":\"A\",\"patientId\":\"1\"},"
						+ "{\"pseudonym\":\"B\",\"patientId\":\"2\",\"patientBirthDate\":\"junk\"}]"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.added").value(1))
			.andExpect(jsonPath("$.skipped").value(1));
	}

	@Test
	void deleteExternalId_returns_404_when_patient_missing() throws Exception {
		ProjectEntity project = new ProjectEntity();
		project.setId(1L);
		when(projectService.retrieveProject(1L)).thenReturn(project);
		when(patientClient.get(any())).thenReturn(null);

		mockMvc.perform(delete("/api/projects/1/external-ids/MISSING").param("issuerId", ""))
			.andExpect(status().isNotFound());
	}

	@Test
	void deleteAllExternalIds_returns_404_when_project_missing() throws Exception {
		when(projectService.retrieveProject(99L)).thenReturn(null);
		mockMvc.perform(delete("/api/projects/99/external-ids")).andExpect(status().isNotFound());
	}

	@Test
	void getExternalIds_returns_only_project_scoped_patients() throws Exception {
		ProjectEntity project = new ProjectEntity();
		project.setId(1L);
		when(projectService.retrieveProject(1L)).thenReturn(project);

		Patient p1 = new Patient("PS1", "ID1", "F", "L", null, "", "");
		p1.setProjectID(1L);
		Patient p2 = new Patient("PS2", "ID2", "F", "L", null, "", "");
		p2.setProjectID(2L); // different project
		when(patientClient.getAll()).thenReturn(List.of(p1, p2));

		mockMvc.perform(get("/api/projects/1/external-ids"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(1))
			.andExpect(jsonPath("$[0].pseudonym").value("PS1"));
	}

	@Test
	void getExternalIds_returns_empty_list_when_cache_empty() throws Exception {
		ProjectEntity project = new ProjectEntity();
		project.setId(1L);
		when(projectService.retrieveProject(1L)).thenReturn(project);
		when(patientClient.getAll()).thenReturn(Collections.emptyList());

		mockMvc.perform(get("/api/projects/1/external-ids"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(0));
	}

}
