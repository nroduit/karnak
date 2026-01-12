/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.destination.component;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import org.karnak.backend.data.entity.DestinationEntity;

/**
 * Create a transfer syntax component
 */
@Getter
public class TranscodeOnlyUncompressedComponent extends VerticalLayout {

	private Checkbox transcodeOnlyUncompressedCheckBox;

	public TranscodeOnlyUncompressedComponent() {

		// In order to not have a padding around the component
		setPadding(false);

		// Build TranscodeOnlyUncompressed components
		buildComponents();

		// Add components
		addComponents();

		// Align this component to the bottom of the container (aesthetical purpose)
		getStyle().set("align-self", "flex-end");
	}

	/**
	 * Add components in TranscodeOnlyUncompressed
	 */
	private void addComponents() {
		add(transcodeOnlyUncompressedCheckBox);
	}

	/**
	 * Build components used in Transfer Syntax component
	 */
	private void buildComponents() {
		transcodeOnlyUncompressedCheckBox = new Checkbox("Transcode only uncompressed");
		transcodeOnlyUncompressedCheckBox.getElement().getStyle().set("margin-left", "66px");
		transcodeOnlyUncompressedCheckBox.getElement().getStyle().set("line-height", "31.5px");
	}

	public void init(Binder<DestinationEntity> binder) {
		binder.forField(transcodeOnlyUncompressedCheckBox)
			.bind(DestinationEntity::isTranscodeOnlyUncompressed, DestinationEntity::setTranscodeOnlyUncompressed);
	}

}
