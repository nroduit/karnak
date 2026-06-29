/*
 * Copyright (c) 2024-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.extid;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;
import org.weasis.core.util.annotations.Generated;

// Holds the live filter terms applied to the ExternalIDGrid data view
@Setter
@Getter
@Generated()
@NullUnmarked
public class PatientFilter {

	private String extidFilter;

	private String patientIdFilter;

	private String patientFirstNameFilter;

	private String patientLastNameFilter;

	private String issuerOfPatientIDFilter;

}
