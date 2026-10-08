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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.ExcludedTagEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.Replace;
import org.karnak.backend.model.profilepipe.HMAC;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ActionDatesTest {

	private static final HMAC HMAC_KEY = new HMAC(HMAC.hexToByte("0123456789abcdef0123456789abcdef"));

	private static ProfileElementEntity element(String option, List<ArgumentEntity> args) {
		return new ProfileElementEntity("name", "action.on.dates", null, "D", option, args, 0, null);
	}

	private static List<ArgumentEntity> shiftArgs() {
		List<ArgumentEntity> args = new ArrayList<>();
		args.add(new ArgumentEntity("days", "30"));
		args.add(new ArgumentEntity("seconds", "0"));
		return args;
	}

	private static Attributes withStudyDate() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.StudyDate, VR.DA, "20200101");
		return dcm;
	}

	@Nested
	class Validation {

		@Test
		void rejects_a_missing_option() {
			assertThrows(ProfileException.class, () -> new ActionDates(element(null, shiftArgs())));
		}

		@Test
		void rejects_an_unknown_option() {
			assertThrows(ProfileException.class, () -> new ActionDates(element("not_an_option", shiftArgs())));
		}

		@Test
		void accepts_the_shift_range_option() throws ProfileException {
			List<ArgumentEntity> args = new ArrayList<>();
			args.add(new ArgumentEntity("max_days", "365"));
			args.add(new ArgumentEntity("max_seconds", "86400"));

			// Constructor performs validation; absence of an exception is the assertion.
			new ActionDates(element("shift_range", args));
		}

		@Test
		void accepts_the_shift_by_tag_option_with_no_arguments() throws ProfileException {
			new ActionDates(element("shift_by_tag", new ArrayList<>()));
		}

	}

	@Nested
	class GetAction {

		@Test
		void shifts_a_date_valued_tag() throws ProfileException {
			ActionDates profile = new ActionDates(element("shift", shiftArgs()));
			Attributes dcm = withStudyDate();

			var action = profile.getAction(dcm, dcm, Tag.StudyDate, HMAC_KEY);

			assertInstanceOf(Replace.class, action);
		}

		@Test
		void ignores_a_non_date_tag() throws ProfileException {
			ActionDates profile = new ActionDates(element("shift", shiftArgs()));
			Attributes dcm = new Attributes();
			dcm.setString(Tag.PatientName, VR.PN, "Doe^John");

			assertNull(profile.getAction(dcm, dcm, Tag.PatientName, HMAC_KEY));
		}

		@Test
		void ignores_an_excepted_date_tag() throws ProfileException {
			ProfileElementEntity element = element("shift", shiftArgs());
			element.addExceptedtags(new ExcludedTagEntity("(0008,0020)", element));
			ActionDates profile = new ActionDates(element);
			Attributes dcm = withStudyDate();

			assertNull(profile.getAction(dcm, dcm, Tag.StudyDate, HMAC_KEY));
		}

		@Test
		void ignores_a_date_tag_outside_the_included_list() throws ProfileException {
			ProfileElementEntity element = element("shift", shiftArgs());
			element.addIncludedTag(new IncludedTagEntity("(0008,0023)", element));
			ActionDates profile = new ActionDates(element);
			Attributes dcm = withStudyDate();

			// StudyDate (0008,0020) is not the included ContentDate (0008,0023).
			assertNull(profile.getAction(dcm, dcm, Tag.StudyDate, HMAC_KEY));
		}

	}

}