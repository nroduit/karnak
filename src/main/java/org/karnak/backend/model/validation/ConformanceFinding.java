/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.validation;

/**
 * A single conformance finding. Record equality is the natural deduplication key when the
 * same issue repeats across the instances of a study.
 *
 * @param tagPath display form of the attribute location, e.g. {@code (0010,0010)} or
 * {@code (0040,0275) > (0008,0050)} for an attribute inside a sequence item
 * @param attributeName attribute (or module) name from the DICOM standard
 * @param moduleId identifier of the IOD module that defines the requirement, may be null
 * @param expected the expected Type/VR/VM/value as text
 * @param found a short summary of what was actually found
 */
public record ConformanceFinding(String tagPath, String attributeName, String moduleId, Severity severity,
		CheckKind kind, String expected, String found) {

}
