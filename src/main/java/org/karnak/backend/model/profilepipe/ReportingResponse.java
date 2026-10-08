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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents the JSON response returned by the external de-identification image API (POST
 * /reporting).
 *
 * <p>
 * Expected JSON structure: <pre>{@code
 * {
 *   "detected_tags": ["PatientName", "PatientAge"],
 *   "message": "2 sensitive tags detected",
 *   "sop_instance_uid": "2.25.251867431509614238946512793485716204981"
 * }
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReportingResponse(@JsonProperty("detected_tags") List<String> detectedTags, String message,
		@JsonProperty("sop_instance_uid") String sopInstanceUid) {

}
