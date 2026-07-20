/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.destination.component;

import com.vaadin.flow.component.html.Div;
import org.weasis.core.util.annotations.Generated;

@Generated()
public class ProfileLabel extends Div {

	public ProfileLabel() {
		// Render the resolved profile as secondary helper text under the project field
		addClassName("karnak-secondary-text");
		getStyle().set("font-size", "0.875rem");
	}

	public void setShowValue(String text) {
		removeAll();
		add(text);
	}

}
