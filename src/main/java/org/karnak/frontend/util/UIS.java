/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.util;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;

public interface UIS {

	static <T extends HasSize> T setWidthFull(T component) {
		component.setWidthFull();
		return component;
	}

	static <T> T configure(T base, Consumer<T> consumer) {
		consumer.accept(base);
		return base;
	}

	static void setTooltip(Component component, String tooltipText) {
		component.getElement().setAttribute("title", tooltipText);
	}

	static boolean containsNoWhitespace(CharSequence seq) {
		return !StringUtils.containsWhitespace(seq);
	}

}
