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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.exception.ProfileException;

@DisplayNameGeneration(ReplaceUnderscores.class)
class CleanPixelDataTest {

	@Test
	void never_produces_a_metadata_action() throws ProfileException {
		CleanPixelData profile = new CleanPixelData(
				new ProfileElementEntity("Clean", "clean.pixel.data", null, null, null, 0, null));

		assertNull(profile.getAction(new Attributes(), new Attributes(), Tag.PixelData, null));
	}

	@Test
	void exposes_the_profile_element_metadata_through_the_abstract_base() throws ProfileException {
		ArgumentEntity argument = new ArgumentEntity("key", "value");
		ProfileElementEntity element = new ProfileElementEntity("Clean", "clean.pixel.data", null, null, "color",
				List.of(argument), 3, null);
		CleanPixelData profile = new CleanPixelData(element);

		assertEquals("Clean", profile.getName());
		assertEquals("Clean", profile.toString());
		assertEquals("clean.pixel.data", profile.getCodeName());
		assertEquals("color", profile.getOption());
		assertEquals(3, profile.getPosition());
		assertNull(profile.getCondition());
		assertEquals(List.of(argument), profile.getArguments());
	}

	@Test
	void rejects_an_invalid_condition_expression() {
		ProfileElementEntity element = new ProfileElementEntity("Clean", "clean.pixel.data", "this is ((( invalid",
				null, null, 0, null);

		ProfileException exception = assertThrows(ProfileException.class, () -> new CleanPixelData(element));
		assertTrue(exception.getMessage().startsWith("Expression is not valid"));
	}

}