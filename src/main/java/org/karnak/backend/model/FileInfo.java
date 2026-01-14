/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model;

public final class FileInfo {

	private final String iuid;

	private final String cuid;

	private final String tsuid;

	private final String filename;

	public FileInfo(String filename, String iuid, String cuid, String tsuid) {
		this.filename = filename;
		this.iuid = iuid;
		this.cuid = cuid;
		this.tsuid = tsuid;
	}

	public String getFilename() {
		return filename;
	}

	public String getCuid() {
		return cuid;
	}

	public String getIuid() {
		return iuid;
	}

	public String getTsuid() {
		return tsuid;
	}

}
