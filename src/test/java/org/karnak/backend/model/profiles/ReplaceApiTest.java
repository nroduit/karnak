/*
 * Copyright (c) 2025-2026 Karnak Team and other contributors.
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
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.AfterAll;
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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;

@SpringBootTest
@TestPropertySource(properties = { "spring.cache.type=none" // Disable caching for tests
})
public class ReplaceApiTest {

	// Services
	private static EndpointService endpointService;

	private static MockedStatic<ApplicationContextProvider> acp;

	private final static String AUTH_CONFIG = "endpoint";

	private final static String UNKNOWN_AUTH_CONFIG = "endpoint_unknown";

	private final static String TEST_URL = "http://sample.url.com/endpoint";

	private final static String TEST_MALFORMED_URL = "http://sample.url.com/{{getString(#Tag.PatientID}}/endpoint";

	private final static String TEST_UNKNOWN_URL = "http://sample.unknown.com/endpoint";

	@BeforeAll
	static void setUpStatic() {
		endpointService = Mockito.mock(EndpointService.class);
		acp = Mockito.mockStatic(ApplicationContextProvider.class);
	}

	@AfterAll
	static void tearDown() {
		if (acp != null) {
			acp.close();
		}
	}

	@BeforeEach
	void setUp() {
		// Reset mock to clear any previous interactions
		reset(endpointService);
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

	/**
	 * Builds a profile replacing {@code includedTag} by the {@code value} field of the
	 * response of an endpoint whose URL carries the patient identifier.
	 */
	private static Profile profileWithPatientIdInTheUrl(String includedTag) {
		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "action.replace.api", null, null,
				null, 0, profileEntity);
		profileElementEntity
			.addArgument(new ArgumentEntity("url", TEST_URL + "/{{getString(#Tag.PatientID)}}", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("responsePath", "value", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("authConfig", AUTH_CONFIG, profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity(includedTag, profileElementEntity));
		profileElementEntity.setProfileEntity(profileEntity);

		profileEntity.addProfilePipe(profileElementEntity);
		return new Profile(profileEntity);
	}

	@Test
	public void urlIsResolvedOnTheOriginalCopyNotOnTheDeIdentifiedDataset() {
		when(endpointService.get(AUTH_CONFIG, TEST_URL + "/97035674"))
			.thenReturn("{\"value\": \"from-the-original-id\"}");
		when(endpointService.get(AUTH_CONFIG, TEST_URL + "/PSEUDO-1"))
			.thenReturn("{\"value\": \"from-the-pseudonymized-id\"}");

		final Attributes original = new Attributes();
		original.setString(Tag.PatientID, VR.LO, "97035674");
		original.setString(Tag.PatientBirthDate, VR.DA, "19800101");
		final Attributes dataset = new Attributes(original);
		// (0010,0020) sorts before (0010,0030): by the time the birth date is visited,
		// the identifier of the working dataset has already been pseudonymized
		dataset.setString(Tag.PatientID, VR.LO, "PSEUDO-1");

		profileWithPatientIdInTheUrl("(0010,0030)").applyAction(dataset, original, null, null, null, null);

		assertEquals("from-the-original-id", dataset.getString(Tag.PatientBirthDate));
	}

	@Test
	public void replacesATagNestedInASequenceUsingTheOriginalCopyForTheUrl() {
		when(endpointService.get(AUTH_CONFIG, TEST_URL + "/97035674"))
			.thenReturn("{\"value\": \"from-the-original-id\"}");
		when(endpointService.get(AUTH_CONFIG, TEST_URL + "/PSEUDO-1"))
			.thenReturn("{\"value\": \"from-the-pseudonymized-id\"}");

		final Attributes original = new Attributes();
		original.setString(Tag.PatientID, VR.LO, "97035674");
		final Attributes item = new Attributes();
		item.setString(Tag.RequestedProcedureID, VR.SH, "RP-1");
		original.newSequence(Tag.RequestAttributesSequence, 1).add(item);
		final Attributes dataset = new Attributes(original);
		dataset.setString(Tag.PatientID, VR.LO, "PSEUDO-1");

		// The nested tag is reached through the sequence recursion: the copy passed down
		// is the copy of the item, from which the identifier of the study is resolved
		profileWithPatientIdInTheUrl("(0040,1001)").applyAction(dataset, original, null, null, null, null);

		assertEquals("from-the-original-id",
				dataset.getNestedDataset(Tag.RequestAttributesSequence).getString(Tag.RequestedProcedureID));
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
