/*
 * Copyright (c) 2025 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;

/**
 * Application shell configuration for the Vaadin UI.
 *
 * All app-shell-level annotations such as @Push, @PWA, @Viewport, etc. must be placed on
 * a class implementing AppShellConfigurator.
 */
@Push // You can customize mode: @Push(PushMode.AUTOMATIC) or MANUAL if needed
@Theme("common-theme")
public class AppShellConfig implements AppShellConfigurator {

	// Add other app-shell settings here later (meta tags, viewport, etc.)

}
