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

import lombok.Getter;
import lombok.Setter;
import org.karnak.backend.enums.MessageFormat;
import org.karnak.backend.enums.MessageLevel;

@Setter
@Getter
public class Message {

	private MessageLevel level;

	private MessageFormat format;

	private String text;

	public Message(MessageLevel level, MessageFormat format, String text) {
		this.level = level;
		this.format = format;
		this.text = text;
	}

}
