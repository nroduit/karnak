/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.monitoring.component;

import lombok.Getter;
import lombok.Setter;
import org.weasis.core.util.annotations.Generated;

/**
 * Model used to collect the export settings of the user
 */
@Setter
@Getter
@Generated()
public class ExportSettings {

	private String delimiter;

	public static final char DEFAULT_CSV_DELIMITER = ',';

	/**
	 * Constructor with default values
	 */
	public ExportSettings() {
		this.delimiter = String.valueOf(DEFAULT_CSV_DELIMITER);
	}

}
