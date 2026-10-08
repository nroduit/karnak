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

import org.jspecify.annotations.Nullable;
import org.karnak.backend.enums.DicomWebServiceType;

/**
 * Outcome of a non-invasive probe of one DICOMweb service: whether the endpoint appears
 * to support it, inferred from the HTTP response to a lightweight request (no study is
 * uploaded or retrieved). Support is intentionally tri-state because OPTIONS / error
 * behaviour varies across servers — {@link Support#INCONCLUSIVE} is common and not a
 * failure.
 *
 * @param type the probed DICOMweb service
 * @param support the inferred support level
 * @param status HTTP status of the probe (0 when it could not be sent)
 * @param detail short human-readable explanation
 */
public record WebServiceProbe(DicomWebServiceType type, Support support, int status, @Nullable String detail) {

	/** Inferred support level for a DICOMweb service. */
	public enum Support {

		SUPPORTED("supported"), UNSUPPORTED("not supported"), INCONCLUSIVE("inconclusive");

		private final String label;

		Support(String label) {
			this.label = label;
		}

		public String getLabel() {
			return this.label;
		}

	}

	public String getSummary() {
		String head = this.type.getDisplayName() + ": " + this.support.getLabel();
		return (this.detail != null) ? head + " — " + this.detail : head;
	}

}
