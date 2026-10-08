/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Covers the value/getter behaviour of the small cross-cutting enums. */
@DisplayNameGeneration(ReplaceUnderscores.class)
class SimpleEnumsTest {

	@Nested
	class TransferStatus {

		@Test
		void exposes_the_sent_error_predicates_and_label() {
			assertEquals(Boolean.TRUE, TransferStatusType.SENT.getSent());
			assertEquals(Boolean.FALSE, TransferStatusType.SENT.getError());
			assertEquals("Sent", TransferStatusType.SENT.getLabel());
			assertNull(TransferStatusType.ALL.getSent());
			assertEquals("Error", TransferStatusType.ERROR.getLabel());
		}

	}

	@Nested
	class PseudonymTypes {

		@Test
		void exposes_its_value() {
			assertTrue(PseudonymType.CACHE_EXTID.getValue().toLowerCase().contains("karnak"));
		}

	}

	@Nested
	class AuthConfig {

		@Test
		void exposes_its_code() {
			assertEquals("OAuth 2.0", AuthConfigType.OAUTH2.getCode());
		}

	}

	@Nested
	class SecurityRoles {

		@Test
		void from_code_and_from_type_resolve_the_role() {
			assertEquals(SecurityRole.ADMIN_ROLE, SecurityRole.fromCode("ROLE_admin"));
			assertEquals(SecurityRole.ADMIN_ROLE, SecurityRole.fromType("admin"));
			assertEquals("ROLE_admin", SecurityRole.ADMIN_ROLE.getRole());
			assertEquals("admin", SecurityRole.ADMIN_ROLE.getType());
		}

		@Test
		void from_code_and_from_type_return_null_for_unknown_or_null() {
			assertNull(SecurityRole.fromCode("ROLE_unknown"));
			assertNull(SecurityRole.fromCode(null));
			assertNull(SecurityRole.fromType("unknown"));
			assertNull(SecurityRole.fromType(null));
		}

	}

	@Nested
	class EnvironmentAndProfile {

		@Test
		void expose_their_codes() {
			assertEquals("IDP", EnvironmentVariable.IDP.getCode());
			assertEquals("oidc", ApplicationProfile.OIDC.getCode());
		}

	}

	@Nested
	class DestinationAndMessageTypes {

		@Test
		void enumerate_their_constants() {
			assertEquals(2, DestinationType.values().length);
			assertEquals(DestinationType.dicom, DestinationType.valueOf("dicom"));
			assertEquals(MessageType.NOTIFICATION_MESSAGE, MessageType.valueOf("NOTIFICATION_MESSAGE"));
		}

	}

}