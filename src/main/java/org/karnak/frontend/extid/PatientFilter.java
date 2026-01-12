/*
 * Copyright (c) 2024 Karnak Team and other contributors.
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

// TODO: currently not used but should be used to replace filters in ExternalIDGrid
@Setter
@Getter
public class PatientFilter {

	private String extidFilter;

	private String patientIdFilter;

	private String patientFirstNameFilter;

	private String patientLastNameFilter;

	private String issuerOfPatientIDFilter;

}
