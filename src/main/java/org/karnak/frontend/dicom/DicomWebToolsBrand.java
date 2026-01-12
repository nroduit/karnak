/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

public class DicomWebToolsBrand extends Composite<Div> {

	private static final String TEXT = "Dicom Web Tools";

	private Span textSpan;

	public DicomWebToolsBrand() {
		Div div = getContent();
		div.getStyle().set("display", "contents");

		createText();

		div.add(textSpan);
	}

	private void createText() {
		textSpan = new Span(TEXT);
		textSpan.getStyle().set("padding-left", "1em");
		textSpan.getStyle().set("padding-right", "1em");
		textSpan.getStyle().set("white-space", "nowrap");
	}

}
