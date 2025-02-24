/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.thread;

import java.util.concurrent.Callable;
import org.karnak.backend.model.dicom.WadoNode;
import org.karnak.frontend.dicom.Util;

public class WadoResponse implements Callable<String> {

	private final WadoNode node;

	public WadoResponse(WadoNode node) {
		this.node = node;
	}

	@Override
	public String call() throws Exception {
		StringBuilder result = new StringBuilder();

		result.append("<h6>WADO HTTP-GET: ");
		result.append(node.toString());
		result.append("</h6>");
		result.append("<small>");
		Util.getWadoResponse(result, node, true, "HTML", 10000);
		result.append("</small>");
		result.append("<hr>");

		return result.toString();
	}

}
