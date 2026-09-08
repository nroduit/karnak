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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.profilepipe.HMAC;

@DisplayNameGeneration(ReplaceUnderscores.class)
class ActionItemsTest {

	private static final HMAC HMAC_KEY = new HMAC(HMAC.hexToByte("0123456789abcdef0123456789abcdef"));

	@Nested
	class KeepAction {

		@Test
		void leaves_the_value_untouched() {
			Attributes dcm = new Attributes();
			dcm.setString(Tag.PatientName, VR.PN, "Doe^John");

			new Keep("K").execute(dcm, Tag.PatientName, HMAC_KEY);

			assertEquals("Doe^John", dcm.getString(Tag.PatientName));
		}

	}

	@Nested
	class RemoveAction {

		@Test
		void removes_the_tag_from_the_object() {
			Attributes dcm = new Attributes();
			dcm.setString(Tag.PatientName, VR.PN, "Doe^John");

			new Remove("X").execute(dcm, Tag.PatientName, HMAC_KEY);

			assertFalse(dcm.contains(Tag.PatientName));
		}

	}

	@Nested
	class ReplaceNullAction {

		@Test
		void keeps_the_tag_but_empties_its_value() {
			Attributes dcm = new Attributes();
			dcm.setString(Tag.PatientName, VR.PN, "Doe^John");

			new ReplaceNull("Z").execute(dcm, Tag.PatientName, HMAC_KEY);

			assertTrue(dcm.contains(Tag.PatientName));
			assertNull(dcm.getString(Tag.PatientName));
		}

	}

	@Nested
	class ExcludeInstanceAction {

		@Test
		void does_not_modify_the_object() {
			Attributes dcm = new Attributes();
			dcm.setString(Tag.PatientName, VR.PN, "Doe^John");

			new ExcludeInstance("EI").execute(dcm, Tag.PatientName, HMAC_KEY);

			assertEquals("Doe^John", dcm.getString(Tag.PatientName));
		}

	}

	@Nested
	class UidAction {

		@Test
		void replaces_uid_with_its_deterministic_hash() {
			Attributes dcm = new Attributes();
			dcm.setString(Tag.StudyInstanceUID, VR.UI, "1.2.3.4.5");

			new UID("U").execute(dcm, Tag.StudyInstanceUID, HMAC_KEY);

			String hashed = dcm.getString(Tag.StudyInstanceUID);
			assertEquals(HMAC_KEY.uidHash("1.2.3.4.5"), hashed);
			assertTrue(hashed.startsWith("2.25."));
		}

		@Test
		void leaves_a_missing_uid_absent() {
			Attributes dcm = new Attributes();

			new UID("U").execute(dcm, Tag.StudyInstanceUID, HMAC_KEY);

			assertFalse(dcm.contains(Tag.StudyInstanceUID));
		}

	}

	@Nested
	class DefaultDummyAction {

		@Test
		void replaces_text_value_with_UNKNOWN() {
			Attributes dcm = new Attributes();
			dcm.setString(Tag.PatientName, VR.PN, "Doe^John");

			new DefaultDummy("D").execute(dcm, Tag.PatientName, HMAC_KEY);

			assertEquals("UNKNOWN", dcm.getString(Tag.PatientName));
		}

		@Test
		void replaces_numeric_value_with_zero() {
			Attributes dcm = new Attributes();
			// Window Center has VR DS (decimal string).
			dcm.setString(Tag.WindowCenter, VR.DS, "128.0");

			new DefaultDummy("D").execute(dcm, Tag.WindowCenter, HMAC_KEY);

			assertEquals("0", dcm.getString(Tag.WindowCenter));
		}

		@Test
		void hashes_uid_value() {
			Attributes dcm = new Attributes();
			dcm.setString(Tag.StudyInstanceUID, VR.UI, "1.2.3.4.5");

			new DefaultDummy("D").execute(dcm, Tag.StudyInstanceUID, HMAC_KEY);

			assertEquals(HMAC_KEY.uidHash("1.2.3.4.5"), dcm.getString(Tag.StudyInstanceUID));
		}

	}

	@Nested
	class ConvertAction {

		@Test
		void maps_symbols_to_concrete_action_types() {
			assertInstanceOf(ReplaceNull.class, AbstractAction.convertAction("Z"));
			assertInstanceOf(Remove.class, AbstractAction.convertAction("X"));
			assertInstanceOf(Keep.class, AbstractAction.convertAction("K"));
			assertInstanceOf(UID.class, AbstractAction.convertAction("U"));
			assertInstanceOf(DefaultDummy.class, AbstractAction.convertAction("D"));
		}

		@Test
		void returns_null_for_unknown_symbol() {
			assertNull(AbstractAction.convertAction("unknown"));
		}

		@Test
		void returns_null_for_null_symbol() {
			assertNull(AbstractAction.convertAction(null));
		}

	}

	@Nested
	class GetStringValue {

		@Test
		void returns_the_text_value_of_a_present_tag() {
			Attributes dcm = new Attributes();
			dcm.setString(Tag.PatientName, VR.PN, "Doe^John");

			assertEquals("Doe^John", AbstractAction.getStringValue(dcm, Tag.PatientName));
		}

		@Test
		void returns_empty_string_for_a_null_object() {
			assertEquals("", AbstractAction.getStringValue(null, Tag.PatientName));
		}

		@Test
		void labels_sequence_data() {
			Attributes dcm = new Attributes();
			dcm.newSequence(Tag.ReferencedImageSequence, 1).add(new Attributes());

			assertEquals("Sequence Data", AbstractAction.getStringValue(dcm, Tag.ReferencedImageSequence));
		}

	}

}