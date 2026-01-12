/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
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

	// private Checkbox savePseudonym;

	public PseudonymInDicomTagComponent(Binder<DestinationEntity> destinationBinder) {
		this.destinationBinder = destinationBinder;
		setWidthFull();
		setElements();
		// TODO TELIMA-289: reactive save pseudonym
		add(UIS.setWidthFull(
				new HorizontalLayout(tag, delimiter, position/* , savePseudonym */)));
	}

	public void setElements() {
		delimiter = new TextField("Delimiter");
		tag = new TextField("Tag");
		position = new NumberField("Position");
		position.setStepButtonsVisible(true);
		position.setMin(0);
		position.setStep(1);

		// TODO: currently deactivate: to reactivate when implementing pseudonym-service
		// in Karnak
		/*
		 * savePseudonym = new Checkbox("Save pseudonym in pseudonym service");
		 * savePseudonym.getStyle().set("margin-top", "30px");
		 * savePseudonym.setValue(true);
		 */
	}

	public void clear() {
		tag.clear();
		delimiter.clear();
		position.clear();
		// savePseudonym.clear();
	}

}
