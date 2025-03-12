/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.expression;

import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

// https://docs.spring.io/spring/docs/3.0.x/reference/expressions.html
public class ExpressionResult {

	private ExpressionResult() {
	}

	public static Object get(String condition, ExpressionItem expressionItem, Class<?> typeOfReturn) {
		try {
			final ExpressionParser parser = new SpelExpressionParser();
			final EvaluationContext context = new StandardEvaluationContext(expressionItem);
			context.setVariable("VR", VR.class);
			context.setVariable("Tag", Tag.class);
			final Expression exp = parser.parseExpression(condition);
			return exp.getValue(context, typeOfReturn);
		}
		catch (final Exception e) {
			throw new IllegalStateException(
					String.format("Cannot execute the parser expression for this expression: %s", condition));
		}
	}

	public static ExpressionError isValid(String condition, ExpressionItem expressionItem, Class<?> typeOfReturn) {
		try {
			final ExpressionParser parser = new SpelExpressionParser();
			final EvaluationContext context = new StandardEvaluationContext(expressionItem);
			context.setVariable("VR", VR.class);
			context.setVariable("Tag", Tag.class);
			final Expression exp = parser.parseExpression(condition);
			Object o = exp.getValue(context, typeOfReturn);
			return new ExpressionError(true, null);
		}
		catch (final Exception e) {
			return new ExpressionError(false, String.format("Expression is not valid: \n\r%s", e.getMessage()));
		}
	}

}
