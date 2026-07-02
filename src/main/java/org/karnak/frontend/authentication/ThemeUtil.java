/*
 * Copyright (c) 2025-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.authentication;

import com.vaadin.flow.component.UI;
import org.weasis.core.util.annotations.Generated;

@Generated()
public final class ThemeUtil {

	private static final String THEME_COLOR_KEY = "theme-variant";

	// Aura selects light/dark via the CSS color-scheme property, not the Lumo "theme"
	// attribute.
	private static final String DARK = "dark";

	private static final String LIGHT = "light";

	private ThemeUtil() {
		// Utility class
	}

	public static void initializeTheme() {
		UI.getCurrent()
			.getPage()
			.executeJs("return localStorage.getItem($0)", THEME_COLOR_KEY)
			.then(String.class, ThemeUtil::applyTheme);
	}

	private static void applyTheme(String colorScheme) {
		if (isValidTheme(colorScheme)) {
			UI.getCurrent().getPage().executeJs("document.documentElement.style.colorScheme = $0", colorScheme);
			UI.getCurrent().getPage().executeJs("localStorage.setItem($0, $1)", THEME_COLOR_KEY, colorScheme);
		}
	}

	private static boolean isValidTheme(String theme) {
		return DARK.equals(theme) || LIGHT.equals(theme);
	}

}
