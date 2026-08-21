/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.expression;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.jspecify.annotations.Nullable;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.weasis.core.util.StringUtil;

// https://docs.spring.io/spring/docs/3.0.x/reference/expressions.html
@Slf4j
public class ExpressionResult {

	/** Prefix of every message reported by {@link #isValid}. */
	public static final String INVALID_EXPRESSION = "Expression is not valid:";

	/**
	 * {@code SpelExpressionParser} is stateless and thread safe, so one instance is
	 * enough.
	 */
	private static final ExpressionParser PARSER = new SpelExpressionParser();

	/**
	 * Above this many distinct expressions the cache is emptied instead of growing. The
	 * key space is bounded by the configuration, so the cap is only a guard against an
	 * unforeseen source of distinct strings.
	 */
	private static final int MAX_CACHED_EXPRESSIONS = 500;

	/**
	 * Expressions already parsed, keyed by their source.
	 *
	 * <p>
	 * A profile evaluates its conditions and expressions once per visited tag of every
	 * instance, so the same handful of strings would otherwise be re-parsed thousands of
	 * times per study, while the parsed form is immutable and holds nothing of the object
	 * it was evaluated against. Parsing is not the dominant cost — reflective evaluation
	 * is — so this saves in the order of 10%, measured over repeated evaluations of a
	 * two-clause condition.
	 */
	private static final Map<String, Expression> PARSED_EXPRESSIONS = new ConcurrentHashMap<>();

	private ExpressionResult() {
	}

	public static Object get(String condition, ExpressionItem expressionItem, Class<?> typeOfReturn) {
		try {
			Object result = evaluate(condition, expressionItem, typeOfReturn);
			return result != null ? result : "";
		}
		catch (final Exception e) {
			throw new IllegalStateException(
					String.format("Cannot execute the parser expression for this expression: %s", condition), e);
		}
	}

	/**
	 * Tells whether an expression can be parsed and evaluated, reporting the failure
	 * instead of throwing it.
	 *
	 * <p>
	 * The expression is not only parsed but also evaluated against
	 * {@code expressionItem}, which is how the result type is checked against
	 * {@code typeOfReturn}. Callers validating a profile pass an empty probe
	 * ({@code new ExprCondition()}, {@code new ExprAction(1, VR.AE, new Attributes())}),
	 * so the evaluation exercises the structure of the expression rather than any real
	 * value: an expression whose result depends on the dataset is reported valid, and one
	 * calling an unknown method or returning an incompatible type is not.
	 * @param condition expression to check, must not be blank
	 * @param expressionItem object the expression is evaluated against, usually an empty
	 * probe
	 * @param typeOfReturn type the expression must be convertible to
	 * @return the outcome, carrying a message meant to be displayed as is when invalid
	 */
	public static ExpressionError isValid(String condition, ExpressionItem expressionItem, Class<?> typeOfReturn) {
		if (!StringUtil.hasText(condition)) {
			return new ExpressionError(false, INVALID_EXPRESSION + " it is empty");
		}
		try {
			evaluate(condition, expressionItem, typeOfReturn);
			return new ExpressionError(true, null);
		}
		catch (final Exception e) {
			return new ExpressionError(false, String.format("%s%n%s", INVALID_EXPRESSION, describeFailure(e)));
		}
	}

	private static @Nullable Object evaluate(String condition, ExpressionItem expressionItem, Class<?> typeOfReturn) {
		final EvaluationContext context = new StandardEvaluationContext(expressionItem);
		context.setVariable("VR", VR.class);
		context.setVariable("Tag", Tag.class);
		return parse(condition).getValue(context, typeOfReturn);
	}

	/**
	 * Returns the parsed form of an expression, parsing it on the first call only. An
	 * expression that does not parse is not cached, so a later call reports the failure
	 * again.
	 * @param condition expression to parse
	 * @return the parsed expression, safe to evaluate against any object
	 */
	static Expression parse(String condition) {
		Expression parsed = PARSED_EXPRESSIONS.get(condition);
		if (parsed != null) {
			return parsed;
		}
		parsed = PARSER.parseExpression(condition);
		if (PARSED_EXPRESSIONS.size() >= MAX_CACHED_EXPRESSIONS) {
			log.warn("More than {} distinct expressions have been parsed, the cache is emptied",
					MAX_CACHED_EXPRESSIONS);
			PARSED_EXPRESSIONS.clear();
		}
		PARSED_EXPRESSIONS.put(condition, parsed);
		return parsed;
	}

	/** Empties the cache of parsed expressions. */
	static void clearCache() {
		PARSED_EXPRESSIONS.clear();
	}

	/** Returns how many expressions are currently cached. */
	static int cacheSize() {
		return PARSED_EXPRESSIONS.size();
	}

	/**
	 * Builds the detail shown to the user: the message of the SpEL failure, which names
	 * the position in the expression, followed by the root cause when the expression
	 * itself threw — the SpEL message alone only says that a method call failed.
	 */
	private static String describeFailure(Exception e) {
		// A null message would otherwise be reported as the literal "null"
		String detail = e.getMessage() != null ? e.getMessage() : e.toString();
		Throwable root = e;
		while (root.getCause() != null && root.getCause() != root) {
			root = root.getCause();
		}
		return root == e ? detail : String.format("%s%nCaused by: %s", detail, root);
	}

}
