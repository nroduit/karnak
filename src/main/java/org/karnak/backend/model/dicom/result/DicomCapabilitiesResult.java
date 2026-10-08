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
 * Outcome of a non-invasive DICOM capability probe: a single association is negotiated
 * proposing a curated set of SOP Classes and transfer syntaxes, and the peer's accepted
 * presentation contexts (read from the A-ASSOCIATE-AC, with no data ever exchanged) are
 * reported as the {@link SopClassCapability capabilities} list, along with the negotiated
 * maximum PDU length and the peer's advertised implementation identity.
 */
@Builder
@Getter
@NullUnmarked
public class DicomCapabilitiesResult {

	/**
	 * Whether the association was established (i.e. the probe could read capabilities).
	 */
	private boolean associated;

	/** Decoded A-ASSOCIATE-RJ reason when the peer rejected the association. */
	private @Nullable String rejectionReason;

	/** Message of any unexpected error raised while probing. */
	private @Nullable String unexpectedErrorMessage;

	/** Maximum PDU length negotiated by the peer (0 when not associated). */
	private int maxPduLength;

	private @Nullable String remoteImplementationVersionName;

	private @Nullable String remoteImplementationClassUid;

	@Singular
	private List<SopClassCapability> capabilities;

	public boolean isRejected() {
		return this.rejectionReason != null;
	}

	public boolean isUnexpectedError() {
		return this.unexpectedErrorMessage != null;
	}

}