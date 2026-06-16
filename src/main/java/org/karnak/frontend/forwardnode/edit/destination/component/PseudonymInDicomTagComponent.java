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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.frontend.util.UIS;

@Getter
public class PseudonymInDicomTagComponent extends Div {

	private final Binder<DestinationEntity> destinationBinder;

	private TextField delimiter;

	private TextField tag;

	private NumberField position;

	public PseudonymInDicomTagComponent(Binder<DestinationEntity> destinationBinder) {
		this.destinationBinder = destinationBinder;
		setWidthFull();
		setElements();
		add(UIS.setWidthFull(new HorizontalLayout(tag, delimiter, position)));
	}

	public void setElements() {
		delimiter = new TextField("Delimiter");
		tag = new TextField("Tag");
		position = new NumberField("Position");
		position.setStepButtonsVisible(true);
		position.setMin(0);
		position.setStep(1);
	}

	public void clear() {
		tag.clear();
		delimiter.clear();
		position.clear();
	}
}
