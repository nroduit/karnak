/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom;

import com.vaadin.flow.component.textfield.TextField;
import org.weasis.core.util.annotations.Generated;

/**
 * Text field for a DICOM Application Entity Title, capped at the 16-character AE Title
 * limit.
 */
@Generated()
public class AETField extends TextField {

	private static final int MAX_LENGTH = 16;

	public AETField() {
		super();
		setMaxLength(MAX_LENGTH);
	}

	public AETField(String label) {
		super(label);
		setMaxLength(MAX_LENGTH);
	}

}
