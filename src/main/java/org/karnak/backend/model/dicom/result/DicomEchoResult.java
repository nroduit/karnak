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

import java.time.Duration;
import java.util.HexFormat;
import lombok.Builder;
import lombok.Getter;
import org.dcm4che3.net.Status;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.weasis.dicom.param.DicomState;

/**
 * Outcome of a single DICOM C-ECHO against a node: the resulting {@link DicomState}, the
 * measured connection / execution durations, the association-level diagnostics (a peer
 * rejection reason, or the peer's advertised implementation identity), and any unexpected
 * error raised while performing the echo.
 */
@Builder
@Getter
@NullUnmarked
public class DicomEchoResult {

	private static final int NUMBER_OF_HEXADECIMAL_DIGITS = 4;

	private @Nullable DicomState dicomState;

	private @Nullable Duration connectionDuration;

	private @Nullable Duration executionDuration;

	private boolean unexpectedError;

	private @Nullable String unexpectedErrorMessage;

	/**
	 * Decoded A-ASSOCIATE-RJ reason when the peer rejected the association (null when the
	 * association was accepted). Pinpoints AE-Title / protocol / congestion problems that
	 * a bare echo failure would not distinguish.
	 */
	private @Nullable String rejectionReason;

	/**
	 * Set when the peer accepted the association but did not accept the Verification SOP
	 * Class, so no C-ECHO could be performed (the node is reachable and associable, but
	 * does not advertise the verification capability). Null otherwise.
	 */
	private @Nullable String verificationUnsupportedMessage;

	/** Implementation Version Name advertised by the peer in the A-ASSOCIATE-AC. */
	private @Nullable String remoteImplementationVersionName;

	/** Implementation Class UID advertised by the peer in the A-ASSOCIATE-AC. */
	private @Nullable String remoteImplementationClassUid;

	public @Nullable Long getConnectionDurationInMs() {
		return (this.connectionDuration != null) ? this.connectionDuration.toMillis() : null;
	}

	public @Nullable Long getExecutionDurationInMs() {
		return (this.executionDuration != null) ? this.executionDuration.toMillis() : null;
	}

	public boolean isSuccessful() {
		return (this.dicomState != null) && this.dicomState.getStatus() == Status.Success;
	}

	public @Nullable String getDicomStatusInHex() {
		String result = null;

		if (this.dicomState != null) {
			HexFormat hexFormat = HexFormat.of();
			result = hexFormat.withUpperCase().toHexDigits(this.dicomState.getStatus(), NUMBER_OF_HEXADECIMAL_DIGITS);
		}

		return result;
	}

	public @Nullable String getDicomStatusMessage() {
		return (this.dicomState != null) ? this.dicomState.getMessage() : null;
	}

	public boolean isRejected() {
		return this.rejectionReason != null;
	}

	public boolean isVerificationUnsupported() {
		return this.verificationUnsupportedMessage != null;
	}

}