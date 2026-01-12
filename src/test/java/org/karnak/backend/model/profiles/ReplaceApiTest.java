/*
 * Copyright (c) 2025 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.exception.AbortException;
import org.karnak.backend.exception.EndpointException;
import org.karnak.backend.service.ApplicationContextProvider;
import org.karnak.backend.service.EndpointService;
import org.karnak.backend.service.profilepipe.Profile;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@SpringBootTest
public class ReplaceApiTest {

	// Services
	@Mock
	private static EndpointService endpointService;

	private static MockedStatic<ApplicationContextProvider> acp;

	private final static String AUTH_CONFIG = "endpoint";

	private final static String UNKNOWN_AUTH_CONFIG = "endpoint_unknown";

	private final static String TEST_URL = "http://sample.url.com/endpoint";

	private final static String TEST_MALFORMED_URL = "http://sample.url.com/{{getString(#Tag.PatientID}}/endpoint";

	private final static String TEST_UNKNOWN_URL = "http://sample.unknown.com/endpoint";

	@BeforeEach
	void setUp() {
		when(endpointService.get(AUTH_CONFIG, TEST_URL))
			.thenReturn("{\"id\": 411,\"type\": \"ZAWIN\",\"value\": \"78727671\"}");
		when(endpointService.get(null, TEST_URL))
			.thenReturn("{\"id\": 411,\"type\": \"ZAWIN\",\"value\": \"78727674\"}");
		when(endpointService.get(AUTH_CONFIG, TEST_URL + "/birthdate"))
			.thenReturn("{\"id\": 411,\"type\": \"ZAWIN\",\"value\": \"78727674\",\"birthdate\": \"19900101\"}");
		when(endpointService.get(UNKNOWN_AUTH_CONFIG, TEST_URL))
			.thenThrow(new IllegalArgumentException("Authentication Code " + UNKNOWN_AUTH_CONFIG + " is not defined"));
		when(endpointService.get(AUTH_CONFIG, TEST_UNKNOWN_URL))
			.thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

		acp.when(() -> ApplicationContextProvider.bean(EndpointService.class)).thenReturn(endpointService);
	}

	@BeforeAll
	static void setUpStatic() {
		acp = Mockito.mockStatic(ApplicationContextProvider.class);
	}

	@Test
	public void replaceZawinIdWithAuthConfig() {
		final Attributes dataset1 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-get-from-api");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientID, VR.LO, "97035674");

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "action.replace.api", null, null,
				null, 0, profileEntity);
		profileElementEntity.addArgument(new ArgumentEntity("url", TEST_URL, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("responsePath", "value", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("authConfig", "endpoint", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,0020)", profileElementEntity));
		profileElementEntity.setProfileEntity(profileEntity);

		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, null, null, null, null);

		assertEquals("78727671", dataset1.getString(Tag.PatientID));
	}

	@Test
	public void replaceZawinIdWithAuthConfig_withDate() {
		final Attributes dataset1 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-get-from-api");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientID, VR.LO, "97035674");
		dataset1.setString(Tag.PatientBirthDate, VR.DA, "");

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "action.replace.api", null, null,
				null, 0, profileEntity);
		profileElementEntity.addArgument(new ArgumentEntity("url", TEST_URL + "/birthdate", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("responsePath", "birthdate", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("authConfig", "endpoint", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,0030)", profileElementEntity));
		profileElementEntity.setProfileEntity(profileEntity);

		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, null, null, null, null);

		assertEquals("19900101", dataset1.getString(Tag.PatientBirthDate));
	}

	@Test
	public void replaceZawinIdWithAuthConfig_malformedUrl() {
		final Attributes dataset1 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-get-from-api");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientID, VR.LO, "97035674");

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "action.replace.api", null, null,
				null, 0, profileEntity);
		profileElementEntity.addArgument(new ArgumentEntity("url", TEST_MALFORMED_URL, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("responsePath", "value", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("authConfig", "endpoint", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,0020)", profileElementEntity));
		profileElementEntity.setProfileEntity(profileEntity);

		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, null, null, null, null);

		assertEquals("97035674", dataset1.getString(Tag.PatientID));
	}

	@Test
	public void replaceZawinIdWithoutAuthConfig() {
		final Attributes dataset1 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-get-from-api");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientID, VR.LO, "97035674");

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "action.replace.api", null, null,
				null, 0, profileEntity);
		profileElementEntity.addArgument(new ArgumentEntity("url", TEST_URL, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("responsePath", "value", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,0020)", profileElementEntity));
		profileElementEntity.setProfileEntity(profileEntity);

		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, null, null, null, null);

		assertEquals("78727674", dataset1.getString(Tag.PatientID));
	}

	@Test
	public void replaceZawinIdWithAuthConfig_authConfigNotFound_shouldAbort() {
		final Attributes dataset1 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-get-from-api");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientID, VR.LO, "97035674");

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "action.replace.api", null, null,
				null, 0, profileEntity);
		profileElementEntity.addArgument(new ArgumentEntity("url", TEST_URL, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("responsePath", "value", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("authConfig", UNKNOWN_AUTH_CONFIG, profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,0020)", profileElementEntity));
		profileElementEntity.setProfileEntity(profileEntity);

		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		assertThrows(EndpointException.class, () -> profile.applyAction(dataset1, dataset1, null, null, null, null));
	}

	@Test
	public void replaceZawinIdWithAuthConfig_authConfigNotFound_shouldSetDefaultValue() {
		final Attributes dataset1 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-get-from-api");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientID, VR.LO, "97035674");

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "action.replace.api", null, null,
				null, 0, profileEntity);
		profileElementEntity.addArgument(new ArgumentEntity("url", TEST_URL, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("responsePath", "value", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("authConfig", UNKNOWN_AUTH_CONFIG, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("defaultValue", "dummy", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,0020)", profileElementEntity));
		profileElementEntity.setProfileEntity(profileEntity);

		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, null, null, null, null);

		assertEquals("dummy", dataset1.getString(Tag.PatientID));
	}

	@Test
	public void replaceZawinIdWithAuthConfig_urlNotFound_shouldAbort() {
		final Attributes dataset1 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-get-from-api");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientID, VR.LO, "97035674");

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "action.replace.api", null, null,
				null, 0, profileEntity);
		profileElementEntity.addArgument(new ArgumentEntity("url", TEST_UNKNOWN_URL, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("responsePath", "value", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("authConfig", AUTH_CONFIG, profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,0020)", profileElementEntity));
		profileElementEntity.setProfileEntity(profileEntity);

		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		assertThrows(EndpointException.class, () -> profile.applyAction(dataset1, dataset1, null, null, null, null));
	}

	@Test
	public void replaceZawinIdWithAuthConfig_urlNotFound_shouldSetDefaultValue() {
		final Attributes dataset1 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-get-from-api");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientID, VR.LO, "97035674");

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "action.replace.api", null, null,
				null, 0, profileEntity);
		profileElementEntity.addArgument(new ArgumentEntity("url", TEST_UNKNOWN_URL, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("responsePath", "value", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("authConfig", AUTH_CONFIG, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("defaultValue", "dummy", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,0020)", profileElementEntity));
		profileElementEntity.setProfileEntity(profileEntity);

		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, null, null, null, null);

		assertEquals("dummy", dataset1.getString(Tag.PatientID));
	}

	@Test
	public void replaceZawinIdWithAuthConfig_jsonValueNotFound_shouldAbort() {
		final Attributes dataset1 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-get-from-api");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientID, VR.LO, "97035674");

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "action.replace.api", null, null,
				null, 0, profileEntity);
		profileElementEntity.addArgument(new ArgumentEntity("url", TEST_URL, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("responsePath", "unexisting", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("authConfig", AUTH_CONFIG, profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,0020)", profileElementEntity));
		profileElementEntity.setProfileEntity(profileEntity);

		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		assertThrows(AbortException.class, () -> profile.applyAction(dataset1, dataset1, null, null, null, null));
	}

	@Test
	public void replaceZawinIdWithAuthConfig_jsonValueNotFound_shouldSetDefaultValue() {
		final Attributes dataset1 = new Attributes();

		dataset1.setString(Tag.PatientName, VR.PN, "TEST-get-from-api");
		dataset1.setString(Tag.StudyInstanceUID, VR.UI, "12345");
		dataset1.setString(Tag.PatientID, VR.LO, "97035674");

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "action.replace.api", null, null,
				null, 0, profileEntity);
		profileElementEntity.addArgument(new ArgumentEntity("url", TEST_UNKNOWN_URL, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("responsePath", "unexisting", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("authConfig", AUTH_CONFIG, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("defaultValue", "dummy", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,0020)", profileElementEntity));
		profileElementEntity.setProfileEntity(profileEntity);

		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dataset1, dataset1, null, null, null, null);

		assertEquals("dummy", dataset1.getString(Tag.PatientID));
	}

}
