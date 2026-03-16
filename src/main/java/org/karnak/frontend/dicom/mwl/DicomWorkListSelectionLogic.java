/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom.mwl;

import org.karnak.backend.enums.MessageFormat;
import org.karnak.backend.enums.MessageLevel;
import org.karnak.backend.model.dicom.Message;
import org.karnak.backend.util.DicomNodeUtil;

public class DicomWorkListSelectionLogic {

	// DIALOG
	private final DicomWorkListSelectionDialog dialog;

	public DicomWorkListSelectionLogic(DicomWorkListSelectionDialog view) {
		this.dialog = view;
	}

	public void loadDicomNodeList() {
		try {
			dialog.removeMessage();
			dialog.loadWorkListNodes(DicomNodeUtil.getAllWorkListNodesDefinedLocally());
		}
		catch (Exception e) {
			Message message = new Message(MessageLevel.ERROR, MessageFormat.TEXT, "Cannot read the set of worklists");
			dialog.displayMessage(message);
		}
	}

}
