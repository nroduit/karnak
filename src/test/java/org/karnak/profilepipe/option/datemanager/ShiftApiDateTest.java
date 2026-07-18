/*
 * Copyright (c) 2025-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.profilepipe.option.datemanager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
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
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.service.ApplicationContextProvider;
import org.karnak.backend.service.EndpointService;
import org.karnak.backend.service.profilepipe.Profile;
import org.karnak.backend.util.ShiftApiDate;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;

@SpringBootTest
@TestPropertySource(properties = { "spring.cache.type=none" // Disable caching for tests
})
class ShiftApiDateTest {

	private static EndpointService endpointService;

	private static MockedStatic<ApplicationContextProvider> acp;

	private static final String AUTH_CONFIG = "endpoint";

	private static final String TEST_URL = "http://sample.url.com/patient/97035674/day-shift";

	private static final String TEST_URL_TEMPLATE = "http://sample.url.com/patient/{{getString(#Tag.PatientID)}}/day-shift";

	private static final String TEST_UNKNOWN_URL = "http://sample.unknown.com/day-shift";

	private static final HMAC hmac = new HMAC(HMAC.generateRandomKey());

	private static final Attributes dataset = new Attributes();

	private static final List<ArgumentEntity> argumentEntities = new ArrayList<>();

	@BeforeAll
	static void setUpStatic() {
		endpointService = Mockito.mock(EndpointService.class);
		acp = Mockito.mockStatic(ApplicationContextProvider.class);
	}

	@BeforeEach
	void setUp() {
		reset(endpointService);
		argumentEntities.clear();

		dataset.clear();
		dataset.setString(Tag.PatientID, VR.LO, "97035674");
		dataset.setString(Tag.StudyDate, VR.DA, "20180209");
		dataset.setString(Tag.StudyTime, VR.TM, "120843");
		dataset.setString(Tag.PatientAge, VR.AS, "043Y");
		dataset.setString(Tag.AcquisitionDateTime, VR.DT, "20180209120854.354");
		dataset.setString(Tag.SeriesDate, VR.DA, "20180209");

		when(endpointService.get(AUTH_CONFIG, TEST_URL)).thenReturn("{\"value\": 10}");
		when(endpointService.get(null, TEST_URL)).thenReturn("{\"value\": \"10\"}");
		when(endpointService.get(AUTH_CONFIG, TEST_URL + "/seconds")).thenReturn("{\"value\": 10, \"seconds\": 500}");
		when(endpointService.get(AUTH_CONFIG, TEST_UNKNOWN_URL))
			.thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
		when(endpointService.get(AUTH_CONFIG, TEST_URL + "/missing")).thenReturn("{\"id\": 411}");
		when(endpointService.get(AUTH_CONFIG, TEST_URL + "/invalid")).thenReturn("{\"value\": \"not-a-number\"}");

		acp.when(() -> ApplicationContextProvider.bean(EndpointService.class)).thenReturn(endpointService);
	}

	private void addDefaultArguments() {
		ArgumentEntity url = new ArgumentEntity();
		url.setArgumentKey("url");
		url.setArgumentValue(TEST_URL);
		ArgumentEntity daysPath = new ArgumentEntity();
		daysPath.setArgumentKey("days_path");
		daysPath.setArgumentValue("/value");
		ArgumentEntity authConfig = new ArgumentEntity();
		authConfig.setArgumentKey("authConfig");
		authConfig.setArgumentValue(AUTH_CONFIG);
		argumentEntities.add(url);
		argumentEntities.add(daysPath);
		argumentEntities.add(authConfig);
	}

	@Test
	void shiftStudyDateByDaysFromApi() {
		addDefaultArguments();
		assertEquals("20180130", ShiftApiDate.shift(dataset, Tag.StudyDate, argumentEntities, hmac));
	}

	@Test
	void shiftStudyTimeWithSecondsFromApi() {
		addDefaultArguments();
		ArgumentEntity secondsPath = new ArgumentEntity();
		secondsPath.setArgumentKey("seconds_path");
		secondsPath.setArgumentValue("/seconds");
		argumentEntities.get(0).setArgumentValue(TEST_URL + "/seconds");
		argumentEntities.add(secondsPath);

		assertEquals("120023.000000", ShiftApiDate.shift(dataset, Tag.StudyTime, argumentEntities, hmac));
	}

	@Test
	void shiftPatientAgeFromApi() {
		addDefaultArguments();
		assertEquals("043Y", ShiftApiDate.shift(dataset, Tag.PatientAge, argumentEntities, hmac));
	}

	@Test
	void shiftAcquisitionDateTimeFromApi() {
		addDefaultArguments();
		assertEquals("20180130120854.354000",
				ShiftApiDate.shift(dataset, Tag.AcquisitionDateTime, argumentEntities, hmac));
	}

	@Test
	void urlTemplateResolvesPatientId() {
		ArgumentEntity url = new ArgumentEntity();
		url.setArgumentKey("url");
		url.setArgumentValue(TEST_URL_TEMPLATE);
		ArgumentEntity daysPath = new ArgumentEntity();
		daysPath.setArgumentKey("days_path");
		daysPath.setArgumentValue("value");
		ArgumentEntity authConfig = new ArgumentEntity();
		authConfig.setArgumentKey("authConfig");
		authConfig.setArgumentValue(AUTH_CONFIG);
		argumentEntities.add(url);
		argumentEntities.add(daysPath);
		argumentEntities.add(authConfig);

		assertEquals("20180130", ShiftApiDate.shift(dataset, Tag.StudyDate, argumentEntities, hmac));
	}

	@Test
	void httpErrorShouldAbort() {
		addDefaultArguments();
		argumentEntities.get(0).setArgumentValue(TEST_UNKNOWN_URL);
		assertThrows(EndpointException.class, () -> ShiftApiDate.shift(dataset, Tag.StudyDate, argumentEntities, hmac));
	}

	@Test
	void missingDaysPathInResponseShouldAbort() {
		addDefaultArguments();
		argumentEntities.get(0).setArgumentValue(TEST_URL + "/missing");
		assertThrows(AbortException.class, () -> ShiftApiDate.shift(dataset, Tag.StudyDate, argumentEntities, hmac));
	}

	@Test
	void nonNumericShiftValueShouldAbort() {
		addDefaultArguments();
		argumentEntities.get(0).setArgumentValue(TEST_URL + "/invalid");
		assertThrows(AbortException.class, () -> ShiftApiDate.shift(dataset, Tag.StudyDate, argumentEntities, hmac));
	}

	@Test
	void verifyShiftArgumentsMissingUrl() {
		ArgumentEntity daysPath = new ArgumentEntity();
		daysPath.setArgumentKey("days_path");
		daysPath.setArgumentValue("/value");
		assertThrows(IllegalArgumentException.class, () -> ShiftApiDate.verifyShiftArguments(List.of(daysPath)));
	}

	@Test
	void verifyShiftArgumentsMissingDaysPath() {
		ArgumentEntity url = new ArgumentEntity();
		url.setArgumentKey("url");
		url.setArgumentValue(TEST_URL);
		assertThrows(IllegalArgumentException.class, () -> ShiftApiDate.verifyShiftArguments(List.of(url)));
	}

	@Test
	void profileApplyActionShiftsMultipleDateTagsFromSingleApiResponse() {
		final Attributes dcm = new Attributes();
		dcm.setString(Tag.PatientID, VR.LO, "97035674");
		dcm.setString(Tag.StudyDate, VR.DA, "20180209");
		dcm.setString(Tag.SeriesDate, VR.DA, "20180209");

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Shift dates from API", "action.on.dates",
				null, null, "shift_from_api", 0, profileEntity);
		profileElementEntity.addArgument(new ArgumentEntity("url", TEST_URL_TEMPLATE, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("days_path", "/value", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("authConfig", AUTH_CONFIG, profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0008,0020)", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0008,0021)", profileElementEntity));
		profileElementEntity.setProfileEntity(profileEntity);

		profileEntity.addProfilePipe(profileElementEntity);
		Profile profile = new Profile(profileEntity);
		profile.applyAction(dcm, dcm, hmac, null, null, null);

		assertEquals("20180130", dcm.getString(Tag.StudyDate));
		assertEquals("20180130", dcm.getString(Tag.SeriesDate));
	}

	@org.junit.jupiter.api.AfterAll
	static void tearDownStatic() {
		if (acp != null) {
			acp.close();
		}
	}

}
