/*
 * Copyright (c) 2025 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.authentication;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.theme.lumo.Lumo;

public final class ThemeUtil {

	private static final String THEME_COLOR_KEY = "theme-variant";

	private ThemeUtil() {
		// Utility class
	}

	public static void initializeTheme() {
		UI.getCurrent()
			.getPage()
			.executeJs("return localStorage.getItem($0)", THEME_COLOR_KEY)
			.then(String.class, ThemeUtil::applyTheme);
	}

	private static void applyTheme(String themeColor) {
		if (isValidTheme(themeColor)) {
			UI.getCurrent().getElement().setAttribute("theme", themeColor);
			UI.getCurrent().getPage().executeJs("localStorage.setItem($0, $1)", THEME_COLOR_KEY, themeColor);
		}
	}

	private static boolean isValidTheme(String theme) {
		return Lumo.DARK.equals(theme) || Lumo.LIGHT.equals(theme);
	}

}
