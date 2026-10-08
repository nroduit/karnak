/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.HashContext;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * {@link BasicProfile} delegates to the DICOM confidentiality-profile action map, which
 * is built from {@code AppConfig}; a {@code @SpringBootTest} context provides that
 * singleton.
 */
@SpringBootTest
@DisplayNameGeneration(ReplaceUnderscores.class)
class BasicProfileTest {

	private static final HMAC HMAC_KEY = new HMAC(
			new HashContext(HMAC.hexToByte("0123456789abcdef0123456789abcdef"), "PATIENT-1"));

	private static BasicProfile basicProfile() {
		return new BasicProfile(new ProfileElementEntity("Basic", "basic.dicom.profile", null, null, null, 0, null));
	}

	@Test
	void resolves_an_action_for_a_tag_in_the_confidentiality_profile() {
		BasicProfile profile = basicProfile();

		// PatientName is part of the basic confidentiality profile.
		assertNotNull(profile.getAction(new Attributes(), new Attributes(), Tag.PatientName, HMAC_KEY));
	}

	@Test
	void returns_null_for_a_tag_not_covered_by_the_profile() {
		BasicProfile profile = basicProfile();

		// SOPClassUID is retained by the basic profile (no action).
		assertNull(profile.getAction(new Attributes(), new Attributes(), Tag.SOPClassUID, HMAC_KEY));
	}

}