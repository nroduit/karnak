/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.HashContext;
import org.slf4j.LoggerFactory;

/**
 * Exercises the {@code log.trace(...)} branches of the action items, which are skipped
 * when trace logging is disabled. Enabling TRACE on the action package logger forces
 * those branches to run.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class ActionTraceLoggingTest {

	private static final HMAC HMAC_KEY = new HMAC(
			new HashContext(HMAC.hexToByte("0123456789abcdef0123456789abcdef"), "PATIENT-1"));

	private Logger actionLogger;

	private Level previousLevel;

	@BeforeEach
	void enableTrace() {
		actionLogger = (Logger) LoggerFactory.getLogger("org.karnak.backend.model.action");
		previousLevel = actionLogger.getLevel();
		actionLogger.setLevel(Level.TRACE);
	}

	@AfterEach
	void restoreLevel() {
		actionLogger.setLevel(previousLevel);
	}

	private static Attributes patientName() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.PatientName, VR.PN, "Doe^John");
		return dcm;
	}

	@Test
	void keep_logs_and_preserves_the_value() {
		Attributes dcm = patientName();

		new Keep("K").execute(dcm, Tag.PatientName, HMAC_KEY);

		assertEquals("Doe^John", dcm.getString(Tag.PatientName));
	}

	@Test
	void exclude_instance_logs_and_preserves_the_value() {
		Attributes dcm = patientName();

		new ExcludeInstance("E").execute(dcm, Tag.PatientName, HMAC_KEY);

		assertEquals("Doe^John", dcm.getString(Tag.PatientName));
	}

	@Test
	void remove_logs_and_deletes_the_tag() {
		Attributes dcm = patientName();

		new Remove("X").execute(dcm, Tag.PatientName, HMAC_KEY);

		assertFalse(dcm.contains(Tag.PatientName));
	}

	@Test
	void replace_null_logs_and_empties_the_value() {
		Attributes dcm = patientName();

		new ReplaceNull("Z").execute(dcm, Tag.PatientName, HMAC_KEY);

		assertEquals(null, dcm.getString(Tag.PatientName));
	}

	@Test
	void replace_logs_for_both_value_and_null_paths() {
		Attributes dcm = patientName();

		new Replace("D", "dummy").execute(dcm, Tag.PatientName, HMAC_KEY);
		assertEquals("dummy", dcm.getString(Tag.PatientName));

		new Replace("D", null).execute(dcm, Tag.PatientName, HMAC_KEY);
		assertEquals(null, dcm.getString(Tag.PatientName));
	}

	@Test
	void uid_logs_for_present_and_absent_values() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.StudyInstanceUID, VR.UI, "1.2.3");

		new UID("U").execute(dcm, Tag.StudyInstanceUID, HMAC_KEY);
		assertEquals(HMAC_KEY.uidHash("1.2.3"), dcm.getString(Tag.StudyInstanceUID));

		// Absent value: nothing is written but the trace branch still runs.
		new UID("U").execute(new Attributes(), Tag.SeriesInstanceUID, HMAC_KEY);
	}

	@Test
	void add_logs_for_both_value_and_null_paths() {
		new Add("a", Tag.PatientComments, VR.LT, "comment").execute(new Attributes(), 0, HMAC_KEY);
		new Add("a", Tag.PatientComments, VR.LT, null).execute(new Attributes(), 0, HMAC_KEY);
	}

	@Test
	void default_dummy_logs_for_each_value_representation() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.PatientName, VR.PN, "Doe^John");
		dcm.setString(Tag.WindowCenter, VR.DS, "128.0");
		dcm.setString(Tag.StudyInstanceUID, VR.UI, "1.2.3");
		dcm.setString(Tag.StudyDate, VR.DA, "20200101");

		new DefaultDummy("D").execute(dcm, Tag.PatientName, HMAC_KEY);
		new DefaultDummy("D").execute(dcm, Tag.WindowCenter, HMAC_KEY);
		new DefaultDummy("D").execute(dcm, Tag.StudyInstanceUID, HMAC_KEY);
		new DefaultDummy("D").execute(dcm, Tag.StudyDate, HMAC_KEY);

		assertEquals("UNKNOWN", dcm.getString(Tag.PatientName));
		assertEquals("0", dcm.getString(Tag.WindowCenter));
	}

}