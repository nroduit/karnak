/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profilepipe;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.karnak.backend.model.profilebody.MaskBody;

/**
 * Represents the JSON response returned by the external de-identification image API
 * (POST /desidentify-image).
 *
 * <p>Expected JSON structure:
 * <pre>{@code
 * {
 *   "message": "Sensitive data detected",
 *   "masks": [
 *     {
 *       "stationName": "*",
 *       "color": "2e2d2d",
 *       "rectangles": [
 *         "229 16 69 19",
 *         "568 15 44 21"
 *       ]
 *     },
 *     {
 *       "stationName": "*",
 *       "color": "202020",
 *       "rectangles": [
 *         "231 44 271 20"
 *       ]
 *     }
 *   ],
 *   "sop_instance_uid": "1.2.840.114340.1762135411026254060.0.1.3.3.3"
 * }
 * }</pre>
 *
 * <p>If no sensitive data is detected, the API returns a response with no {@code mask}
 * field:
 * <pre>{@code
 * {
 *   "message": "No sensitive data detected",
 *   "sop_instance_uid": "1.2.840.114340.1762135411026254060.0.1.3.3.3"
 * }
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DeidentifyImageResponse(String message, List<MaskBody> masks,
                                      @JsonProperty("sop_instance_uid") String sopInstanceUid) {

}
