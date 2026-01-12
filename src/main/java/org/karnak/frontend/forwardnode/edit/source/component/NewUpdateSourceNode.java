/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.source.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import org.karnak.backend.data.entity.DicomSourceNodeEntity;
import org.karnak.frontend.forwardnode.edit.component.ButtonSaveDeleteCancel;

public class NewUpdateSourceNode extends VerticalLayout {

	@Getter
	private final Binder<DicomSourceNodeEntity> binderFormSourceNode;

	private final FormSourceNode formSourceNode;

	@Getter
	private final ButtonSaveDeleteCancel buttonSaveDeleteCancel;

	@Getter
	private DicomSourceNodeEntity currentSourceNode;

	public NewUpdateSourceNode() {
		currentSourceNode = null;
		binderFormSourceNode = new BeanValidationBinder<>(DicomSourceNodeEntity.class);
		buttonSaveDeleteCancel = new ButtonSaveDeleteCancel();
		formSourceNode = new FormSourceNode(binderFormSourceNode, buttonSaveDeleteCancel);
	}

	public void setView() {
		removeAll();
		binderFormSourceNode.readBean(currentSourceNode);
		add(formSourceNode);
	}

	public void load(DicomSourceNodeEntity sourceNode) {
		if (sourceNode != null) {
			currentSourceNode = sourceNode;
			buttonSaveDeleteCancel.getDelete().setEnabled(true);
		}
		else {
			currentSourceNode = DicomSourceNodeEntity.ofEmpty();
			buttonSaveDeleteCancel.getDelete().setEnabled(false);
		}
		setView();
	}

	public Button getButtonCancel() {
		return buttonSaveDeleteCancel.getCancel();
	}

}
