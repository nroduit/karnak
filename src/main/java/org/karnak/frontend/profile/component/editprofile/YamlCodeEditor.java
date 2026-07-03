/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.profile.component.editprofile;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

/**
 * Server-side wrapper for the {@code <karnak-yaml-editor>} Lit web component: a monospace
 * text editor that renders YAML with syntax highlighting (keys, strings, numbers,
 * booleans, comments, ...). It is a drop-in replacement for a plain {@code TextArea} for
 * the raw-YAML profile editing mode, exposing a two-way {@code value} and a
 * {@code readonly} flag.
 */
@Tag("karnak-yaml-editor")
@JsModule("./components/yaml-code-editor.ts")
public class YamlCodeEditor extends Component implements HasSize {

	public void setValue(String value) {
		getElement().setProperty("value", value == null ? "" : value);
	}

	@Synchronize(property = "value", value = "value-changed")
	public String getValue() {
		return getElement().getProperty("value", "");
	}

	public void setReadOnly(boolean readOnly) {
		getElement().setProperty("readonly", readOnly);
	}

	public boolean isReadOnly() {
		return getElement().getProperty("readonly", false);
	}

}