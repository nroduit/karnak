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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.model.expression.ExprCondition;
import org.karnak.backend.model.expression.ExpressionError;
import org.karnak.backend.model.expression.ExpressionResult;
import org.karnak.frontend.util.UIS;

public class DestinationCondition extends Div {

	private static final String LABEL_CONDITION = "Condition (Leave blank if no condition)";

	private final TextField condition;

	private final Span textErrorConditionMsg;

	public DestinationCondition() {
		this.setWidthFull();

		this.condition = new TextField(LABEL_CONDITION);
		this.textErrorConditionMsg = new Span();
	}

	public void init(Binder<DestinationEntity> binder) {
		setElements();
		setBinder(binder);
		this.add(new HorizontalLayout(UIS.setWidthFull(condition)),
				new HorizontalLayout(UIS.setWidthFull(textErrorConditionMsg)));
	}

	private void setElements() {
		condition.setWidthFull();
		textErrorConditionMsg.setText("");
		textErrorConditionMsg.getStyle().set("color", "red");
		textErrorConditionMsg.getStyle().set("font-size", "0.8em");
	}

	private void setBinder(Binder<DestinationEntity> binder) {
		binder.forField(condition).withValidator(value -> {
			if (!condition.getValue().isEmpty()) {
				ExpressionError expressionError = ExpressionResult.isValid(condition.getValue(), new ExprCondition(),
						Boolean.class);
				textErrorConditionMsg.setText(expressionError.getMsg());
				return expressionError.isValid();
			}
			return true;
		}, "Condition not valid").withValidationStatusHandler(status -> {
			if (!status.isError()) {
				textErrorConditionMsg.setText("");
			}
		}).bind(DestinationEntity::getCondition, DestinationEntity::setCondition);
	}

}
