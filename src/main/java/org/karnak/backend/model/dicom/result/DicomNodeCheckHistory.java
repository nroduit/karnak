/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom.result;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * Immutable view of one persisted DICOM Echo check, mapped from
 * {@code DicomNodeCheckHistoryEntity} so the UI never touches JPA entities directly. One
 * instance maps to one row in the history grid.
 */
@Getter
@Builder
public class DicomNodeCheckHistory {

	private final Instant checkedAt;

	private final @Nullable String callingAeTitle;

	private final @Nullable String calledDescription;

	private final String calledAeTitle;

	private final String calledHostname;

	private final int calledPort;

	private final boolean echoSuccessful;

	private final @Nullable String echoStatusHex;

	private final @Nullable String echoStatusMessage;

	private final @Nullable String echoErrorMessage;

	private final @Nullable String echoRejectionReason;

	private final @Nullable String echoVerificationUnsupportedMessage;

	private final @Nullable String remoteImplVersionName;

	private final @Nullable String remoteImplClassUid;

	private final @Nullable Long connectionMs;

	private final @Nullable Long executionMs;

	private final boolean networkReachable;

	private final boolean portOpen;

	private final @Nullable String networkHostnameMessage;

	private final @Nullable String networkPortMessage;

	private final @Nullable String networkQualityMessage;

	public String getCalledNetworkDetails() {
		return this.calledAeTitle + " " + this.calledHostname + " " + this.calledPort;
	}

}