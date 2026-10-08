/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.ExcludedTagEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.profilepipe.HMAC;

/**
 * {@link Expression} evaluates a SpEL action expression for tags it includes (minus the
 * excepted ones). The constructor also validates the {@code expr} argument.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class ExpressionProfileTest {

	private static final HMAC HMAC_KEY = new HMAC(HMAC.hexToByte("0123456789abcdef0123456789abcdef"));

	private static ProfileElementEntity element(String expr) {
		ProfileElementEntity element = new ProfileElementEntity();
		element.setName("Expression");
		element.setCodename("expression.on.tags");
		element.setPosition(0);
		element.addArgument(new ArgumentEntity("expr", expr, element));
		element.addIncludedTag(new IncludedTagEntity("(0010,0010)", element));
		return element;
	}

	private static Attributes patient() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.PatientName, VR.PN, "Doe^John");
		return dcm;
	}

	@Test
	void evaluates_the_expression_for_an_included_tag() throws ProfileException {
		Expression profile = new Expression(element("Keep()"));

		ActionItem action = profile.getAction(patient(), patient(), Tag.PatientName, HMAC_KEY);

		assertNotNull(action);
		assertEquals("K", action.getSymbol());
	}

	@Test
	void returns_null_for_a_tag_that_is_not_included() throws ProfileException {
		Expression profile = new Expression(element("Keep()"));

		assertNull(profile.getAction(patient(), patient(), Tag.StudyDate, HMAC_KEY));
	}

	@Test
	void returns_null_for_an_excepted_tag() throws ProfileException {
		ProfileElementEntity element = element("Keep()");
		element.addExceptedtags(new ExcludedTagEntity("(0010,0010)", element));
		Expression profile = new Expression(element);

		assertNull(profile.getAction(patient(), patient(), Tag.PatientName, HMAC_KEY));
	}

	@Test
	void rejects_a_missing_expr_argument() {
		ProfileElementEntity element = new ProfileElementEntity();
		element.setName("Expression");
		element.setCodename("expression.on.tags");
		element.addArgument(new ArgumentEntity("notExpr", "Keep()", element));
		element.addIncludedTag(new IncludedTagEntity("(0010,0010)", element));

		assertThrows(IllegalArgumentException.class, () -> new Expression(element));
	}

	@Test
	void rejects_an_invalid_expr_argument() {
		assertThrows(IllegalArgumentException.class, () -> new Expression(element("this is not valid spel (((")));
	}

}