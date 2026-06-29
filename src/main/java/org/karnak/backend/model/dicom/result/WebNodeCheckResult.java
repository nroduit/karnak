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

import org.karnak.backend.model.dicom.WebDestinationNode;

/**
 * Pairs a configured {@link WebDestinationNode} with its
 * {@link WebDestinationCheckResult}, so the Monitor grid can show the destination's
 * description alongside the reachability outcome. One instance maps to one row.
 *
 * @param node the checked DICOMweb destination
 * @param result the reachability outcome
 */
public record WebNodeCheckResult(WebDestinationNode node, WebDestinationCheckResult result) {
}
