/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom.result;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class WebDestinationCheckResultTest {

	@Test
	void reachable_http_endpoint_is_successful() {
		WebDestinationCheckResult result = WebDestinationCheckResult.builder()
			.url("http://pacs/dicom-web")
			.host("pacs")
			.port(80)
			.tcpReachable(true)
			.httpResponded(true)
			.httpStatus(200)
			.build();

		assertTrue(result.isSuccessful());
	}

	@Test
	void auth_required_status_is_still_reachable() {
		WebDestinationCheckResult result = WebDestinationCheckResult.builder()
			.url("https://pacs/dicom-web")
			.secure(true)
			.tcpReachable(true)
			.httpResponded(true)
			.httpStatus(401)
			.tls(new TlsCertificateInfo("TLSv1.3", "CN=pacs", "CN=ca", "2099-01-01", 9999, false, true))
			.build();

		assertTrue(result.isSuccessful());
	}

	@Test
	void expired_certificate_is_not_successful() {
		WebDestinationCheckResult result = WebDestinationCheckResult.builder()
			.url("https://pacs/dicom-web")
			.secure(true)
			.tcpReachable(true)
			.httpResponded(true)
			.httpStatus(200)
			.tls(new TlsCertificateInfo("TLSv1.3", "CN=pacs", "CN=ca", "2020-01-01", -100, true, true))
			.build();

		assertFalse(result.isSuccessful());
	}

	@Test
	void unreachable_endpoint_is_not_successful() {
		WebDestinationCheckResult result = WebDestinationCheckResult.builder()
			.url("http://pacs/dicom-web")
			.host("pacs")
			.port(80)
			.tcpReachable(false)
			.build();

		assertFalse(result.isSuccessful());
	}

	@Test
	void unacquired_token_makes_the_result_unsuccessful() {
		WebDestinationCheckResult result = WebDestinationCheckResult.builder()
			.url("https://pacs/dicom-web")
			.secure(true)
			.tcpReachable(true)
			.httpResponded(true)
			.httpStatus(401)
			.auth(new AuthCheckResult("idp", false, "invalid_client"))
			.build();

		assertFalse(result.isSuccessful());
	}

	@Test
	void acquired_token_keeps_the_result_successful() {
		WebDestinationCheckResult result = WebDestinationCheckResult.builder()
			.url("https://pacs/dicom-web")
			.secure(true)
			.tcpReachable(true)
			.httpResponded(true)
			.httpStatus(200)
			.auth(new AuthCheckResult("idp", true, null))
			.build();

		assertTrue(result.isSuccessful());
	}

	@Test
	void invalid_url_reports_error() {
		WebDestinationCheckResult result = WebDestinationCheckResult.builder()
			.url("not a url")
			.unexpectedErrorMessage("Invalid URL")
			.build();

		assertTrue(result.isUnexpectedError());
		assertFalse(result.isSuccessful());
	}

}
