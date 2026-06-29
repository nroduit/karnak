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

import java.text.MessageFormat;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/**
 * Outcome of the low-level network reachability check for a node: whether the hostname
 * could be resolved / pinged, whether the DICOM port accepts TCP connections and the
 * measured connection quality, together with human-readable messages explaining each
 * result.
 */
@Builder
@Getter
@NullUnmarked
public class NetworkCheckResult {

	private static final String REACHABLE_HOSTNAME_TEXT = "{0}/{1} machine is turned on and can be pinged";

	private static final String UNRESOLVED_HOSTNAME_TEXT = "{0}/{1} host address and host name are equal, meaning the host name could not be resolved";

	private static final String UNREACHABLE_HOSTNAME_TEXT = "{0}/{1} machine is known in a DNS lookup but cannot be pinged";

	private static final String PORT_OPEN_TEXT = "{0} is listening on port {1}";

	private static final String PORT_CLOSED_TEXT = "{0} is not listening on port {1}";

	private static final String UNEXPECTED_ERROR_TEXT = "Unexpected error: {0}";

	private String hostname;

	private @Nullable String hostAddress;

	private int port;

	private boolean hostnameReachable;

	private boolean portOpen;

	private boolean unresolvedHostname;

	private boolean unexpectedError;

	private @Nullable String unexpectedErrorMessage;

	/**
	 * TCP connection-quality sample against the DICOM port (null when the port is
	 * closed).
	 */
	private @Nullable ConnectionLatency connectionLatency;

	public boolean isSuccessful() {
		return this.hostnameReachable;
	}

	public String getCheckHostnameMessage() {
		String message;

		if (this.hostnameReachable) {
			message = MessageFormat.format(REACHABLE_HOSTNAME_TEXT, this.hostname, this.hostAddress);
		}
		else if (this.unexpectedError) {
			message = MessageFormat.format(UNEXPECTED_ERROR_TEXT, this.unexpectedErrorMessage);
		}
		else if (this.unresolvedHostname) {
			message = MessageFormat.format(UNRESOLVED_HOSTNAME_TEXT, this.hostAddress, this.hostname);
		}
		else {
			message = MessageFormat.format(UNREACHABLE_HOSTNAME_TEXT, this.hostname, this.hostAddress);
		}

		return message;
	}

	public String getCheckPortMessage() {
		String message;

		if (this.portOpen) {
			message = MessageFormat.format(PORT_OPEN_TEXT, this.hostname, this.port);
		}
		else {
			message = MessageFormat.format(PORT_CLOSED_TEXT, this.hostname, this.port);
		}

		return message;
	}

	public @Nullable String getCheckQualityMessage() {
		if (this.connectionLatency == null) {
			return null;
		}

		ConnectionLatency latency = this.connectionLatency;
		return "Connection time min/avg/max " + latency.minMs() + "/" + latency.avgMs() + "/" + latency.maxMs()
				+ " ms over " + latency.attempts() + " attempt(s), " + latency.packetLossPercent() + "% loss";
	}

}