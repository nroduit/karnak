/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profilepipe;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class HMACTest {

	// A deterministic 16-byte key so hashing results are reproducible across runs.
	private static final byte[] KEY = HMAC.hexToByte("0123456789abcdef0123456789abcdef");

	@Nested
	class ByteToHex {

		@Test
		void converts_each_byte_to_two_lowercase_hex_chars() {
			assertEquals("000fff10", HMAC.byteToHex(new byte[] { 0x00, 0x0f, (byte) 0xff, 0x10 }));
		}

		@Test
		void returns_empty_string_for_empty_array() {
			assertEquals("", HMAC.byteToHex(new byte[0]));
		}

		@Test
		void throws_on_null_input() {
			assertThrows(NullPointerException.class, () -> HMAC.byteToHex(null));
		}

	}

	@Nested
	class HexToByte {

		@Test
		void parses_plain_hex_string() {
			assertArrayEquals(new byte[] { 0x00, (byte) 0xff }, HMAC.hexToByte("00ff"));
		}

		@Test
		void strips_0x_prefix() {
			assertArrayEquals(new byte[] { 0x00, (byte) 0xff }, HMAC.hexToByte("0x00ff"));
		}

		@Test
		void strips_dashes() {
			assertArrayEquals(new byte[] { 0x00, (byte) 0xff }, HMAC.hexToByte("00-ff"));
		}

		@Test
		void pads_odd_length_with_leading_zero() {
			assertArrayEquals(new byte[] { 0x0f }, HMAC.hexToByte("f"));
		}

		@Test
		void returns_empty_array_for_empty_string() {
			assertArrayEquals(new byte[0], HMAC.hexToByte(""));
		}

		@Test
		void throws_on_invalid_hex_character() {
			assertThrows(IllegalArgumentException.class, () -> HMAC.hexToByte("zz"));
		}

		@Test
		void throws_on_null_input() {
			assertThrows(NullPointerException.class, () -> HMAC.hexToByte(null));
		}

		@Test
		void round_trips_with_byteToHex() {
			assertEquals("abcd1234", HMAC.byteToHex(HMAC.hexToByte("abcd1234")));
		}

	}

	@Nested
	class ValidateKey {

		@Test
		void accepts_32_char_hex_key() {
			assertTrue(HMAC.validateKey("0123456789abcdef0123456789abcdef"));
		}

		@Test
		void accepts_dashed_key_that_is_32_hex_chars() {
			assertTrue(HMAC.validateKey("01234567-89abcdef0123456789abcdef"));
		}

		@Test
		void rejects_key_with_wrong_length() {
			assertFalse(HMAC.validateKey("abcd"));
		}

		@Test
		void rejects_key_with_invalid_character() {
			assertFalse(HMAC.validateKey("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"));
		}

	}

	@Nested
	class ShowHexKey {

		@Test
		void formats_key_into_dashed_groups() {
			assertEquals("01234567-89ab-cdef-0123-456789abcdef", HMAC.showHexKey("0123456789abcdef0123456789abcdef"));
		}

	}

	@Nested
	class GenerateRandomKey {

		@Test
		void produces_a_key_of_the_expected_byte_length() {
			assertEquals(HMAC.KEY_BYTE_LENGTH, HMAC.generateRandomKey().length);
		}

		@Test
		void produces_different_keys_on_successive_calls() {
			assertFalse(java.util.Arrays.equals(HMAC.generateRandomKey(), HMAC.generateRandomKey()));
		}

	}

	@Nested
	class UidHash {

		@Test
		void is_deterministic_for_the_same_key_and_input() {
			HMAC hmac1 = new HMAC(KEY);
			HMAC hmac2 = new HMAC(KEY);

			assertEquals(hmac1.uidHash("1.2.3.4.5"), hmac2.uidHash("1.2.3.4.5"));
		}

		@Test
		void produces_a_25_root_uid() {
			assertTrue(new HMAC(KEY).uidHash("1.2.3.4.5").startsWith("2.25."));
		}

		@Test
		void maps_different_inputs_to_different_uids() {
			HMAC hmac = new HMAC(KEY);

			assertNotEquals(hmac.uidHash("1.2.3.4.5"), hmac.uidHash("1.2.3.4.6"));
		}

		@Test
		void returns_null_for_blank_input() {
			HMAC hmac = new HMAC(KEY);

			assertNull(hmac.uidHash(""));
		}

	}

	@Nested
	class ScaleHash {

		@Test
		void returns_a_value_within_the_requested_range() {
			HMAC hmac = new HMAC(KEY);

			for (int i = 0; i < 50; i++) {
				double value = hmac.scaleHash("value-" + i, 0, 100);
				assertTrue(value >= 0 && value < 100, "out of range: " + value);
			}
		}

		@Test
		void is_deterministic_for_the_same_input() {
			HMAC hmac = new HMAC(KEY);

			assertEquals(hmac.scaleHash("patient", 0, 365), hmac.scaleHash("patient", 0, 365));
		}

	}

	@Nested
	class FromHashContext {

		@Test
		void uses_the_secret_carried_by_the_context() {
			HashContext context = new HashContext(KEY, "PATIENT-1");
			HMAC hmac = new HMAC(context);

			assertEquals("PATIENT-1", hmac.getHashContext().patientID());
			assertEquals(new HMAC(KEY).uidHash("1.2.3"), hmac.uidHash("1.2.3"));
		}

	}

}
