/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.enums.MessageFormat;
import org.karnak.backend.enums.MessageLevel;

@DisplayNameGeneration(ReplaceUnderscores.class)
class MessageTest {

	@Test
	void exposes_constructor_values_and_supports_mutation() {
		Message message = new Message(MessageLevel.WARN, MessageFormat.HTML, "hello");

		assertEquals(MessageLevel.WARN, message.getLevel());
		assertEquals(MessageFormat.HTML, message.getFormat());
		assertEquals("hello", message.getText());

		message.setLevel(MessageLevel.ERROR);
		message.setFormat(MessageFormat.TEXT);
		message.setText("bye");

		assertEquals(MessageLevel.ERROR, message.getLevel());
		assertEquals(MessageFormat.TEXT, message.getFormat());
		assertEquals("bye", message.getText());
	}

}