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

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import org.karnak.backend.data.entity.DestinationEntity;

@Getter
public class FilterBySOPClassesForm extends HorizontalLayout {

	private final MultiSelectComboBox<String> sopFilter;

	private final Checkbox filterBySOPClassesCheckbox;

	private Binder<DestinationEntity> binder;

	public FilterBySOPClassesForm() {
		this.filterBySOPClassesCheckbox = new Checkbox("Authorized SOPs");
		this.sopFilter = new MultiSelectComboBox<>();
	}

	public void init(Binder<DestinationEntity> binder) {
		this.binder = binder;
		setElements();
		setPadding(true);
		add(filterBySOPClassesCheckbox, sopFilter);
	}

	private void setElements() {
		filterBySOPClassesCheckbox.setMinWidth("14%");
		sopFilter.setMinWidth("80%");
		sopFilter.getElement().getStyle().set("padding-left", "5%");

		filterBySOPClassesCheckbox.setValue(false);
		sopFilter.onEnabledStateChanged(false);

		filterBySOPClassesCheckbox.addValueChangeListener(checkboxBooleanComponentValueChangeEvent -> sopFilter
			.onEnabledStateChanged(checkboxBooleanComponentValueChangeEvent.getValue()));
	}

}
