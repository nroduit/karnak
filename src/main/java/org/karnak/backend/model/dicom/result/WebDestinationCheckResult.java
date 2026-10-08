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

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * Outcome of a non-invasive reachability check for a DICOMweb (STOW-RS) destination: TCP
 * reach to the URL host, the TLS handshake / certificate for {@code https} endpoints, and
 * the HTTP status returned by an {@code OPTIONS} probe. No study is uploaded.
 */
@Builder(toBuilder = true)
@Getter
@NullUnmarked
public class WebDestinationCheckResult {

	private String url;

	private @Nullable String host;

	private int port;

	private boolean secure;

	private boolean tcpReachable;

	/**
	 * TLS handshake details for an {@code https} endpoint (null for plain HTTP or no
	 * reach).
	 */
	private @Nullable TlsCertificateInfo tls;

	/** HTTP status returned by the probe (0 when the endpoint did not respond). */
	private int httpStatus;

	private boolean httpResponded;

	/**
	 * OAuth token check when the destination references an auth configuration (else
	 * null).
	 */
	private @Nullable AuthCheckResult auth;

	/**
	 * Per-DICOMweb-service probes (empty for direct URL checks that do not probe them).
	 */
	@Singular
	private List<WebServiceProbe> serviceProbes;

	private @Nullable String unexpectedErrorMessage;

	/**
	 * The endpoint is reachable and answered HTTP, its certificate (when secured) is
	 * within its validity window, and — when an auth configuration is referenced — an
	 * access token could be obtained. An auth-required HTTP status (401/403) still counts
	 * as reachable; trust is reported separately because internal CAs are common.
	 */
	public boolean isSuccessful() {
		return this.tcpReachable && this.httpResponded && (this.tls == null || !this.tls.expired())
				&& (this.auth == null || this.auth.acquired());
	}

	public boolean isUnexpectedError() {
		return this.unexpectedErrorMessage != null;
	}

}