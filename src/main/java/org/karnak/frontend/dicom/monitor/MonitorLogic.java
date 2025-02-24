/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom.monitor;

import java.util.concurrent.ExecutionException;
import org.karnak.backend.enums.MessageFormat;
import org.karnak.backend.enums.MessageLevel;
import org.karnak.backend.model.dicom.DicomNodeList;
import org.karnak.backend.model.dicom.Message;
import org.karnak.backend.model.dicom.WadoNodeList;
import org.karnak.backend.service.dicom.DicomEchoService;
import org.karnak.backend.service.dicom.WadoService;

public class MonitorLogic {

	// PAGE
	private final MonitorView view;

	// SERVICES
	private final DicomEchoService dicomEchoService;

	private final WadoService wadoService;

	// DATA
	private DicomNodeList dicomNodeListSelected;

	private WadoNodeList wadoNodeListSelected;

	public MonitorLogic(MonitorView view) {
		this.view = view;

		dicomEchoService = new DicomEchoService();
		wadoService = new WadoService();
	}

	public void dicomNodeListSelected(DicomNodeList dicomNodeList) {
		this.dicomNodeListSelected = dicomNodeList;
	}

	public void wadoNodeListSelected(WadoNodeList wadoNodeList) {
		this.wadoNodeListSelected = wadoNodeList;
	}

	public void dicomEcho() {
		try {
			String result = dicomEchoService.dicomEcho(dicomNodeListSelected);
			view.displayStatus(result);
		}
		catch (InterruptedException e) {
			Message message = new Message(MessageLevel.ERROR, MessageFormat.TEXT, "Execution was interrupted");
			view.displayMessage(message);
			Thread.currentThread().interrupt();
		}
		catch (ExecutionException e) {
			Message message = new Message(MessageLevel.ERROR, MessageFormat.TEXT, "Execution failed");
			view.displayMessage(message);
		}
	}

	public void wado() {
		try {
			String result = wadoService.checkWado(wadoNodeListSelected);
			view.displayStatus(result);
		}
		catch (InterruptedException e) {
			Message message = new Message(MessageLevel.ERROR, MessageFormat.TEXT, "Execution was interrupted");
			view.displayMessage(message);
			Thread.currentThread().interrupt();
		}
		catch (ExecutionException e) {
			Message message = new Message(MessageLevel.ERROR, MessageFormat.TEXT, "Execution failed");
			view.displayMessage(message);
		}
	}

}
