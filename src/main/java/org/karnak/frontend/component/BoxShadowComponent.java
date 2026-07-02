/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import org.weasis.core.util.annotations.Generated;

@Generated()
public class BoxShadowComponent extends Div {

	public BoxShadowComponent(Component... component) {
		getElement().getStyle().set("box-shadow", "var(--aura-shadow-s)");
		getElement().getStyle().set("border-radius", "var(--vaadin-radius-m)");
		getElement().getStyle().set("background-color", "hsla(245, 100%, 100%, 0.03)");
		add(component);
	}

}
