/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom;

import org.karnak.backend.enums.MessageFormat;
import org.karnak.backend.enums.MessageLevel;

public class Message {

	private MessageLevel level;

	private MessageFormat format;

	private String text;

	public Message(MessageLevel level, MessageFormat format, String text) {
		this.level = level;
		this.format = format;
		this.text = text;
	}

	public MessageLevel getLevel() {
		return level;
	}

	public void setLevel(MessageLevel level) {
		this.level = level;
	}

	public MessageFormat getFormat() {
		return format;
	}

	public void setFormat(MessageFormat format) {
		this.format = format;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

}
