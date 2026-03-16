/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom.echo;

import org.karnak.backend.enums.MessageFormat;
import org.karnak.backend.enums.MessageLevel;
import org.karnak.backend.model.dicom.Message;
import org.karnak.backend.util.DicomNodeUtil;

public class DicomEchoSelectionLogic {

	// DIALOG
	private final DicomEchoSelectionDialog dialog;

	public DicomEchoSelectionLogic(DicomEchoSelectionDialog dialog) {
		this.dialog = dialog;
	}

	public void loadDicomNodeList() {
		try {
			dialog.removeMessage();
			dialog.loadDicomNodeTypes(DicomNodeUtil.getAllDicomNodeTypesDefinedLocally());
		}
		catch (Exception e) {
			Message message = new Message(MessageLevel.ERROR, MessageFormat.TEXT,
					"Cannot read the list of DICOM nodes");
			dialog.displayMessage(message);
		}
	}

}
