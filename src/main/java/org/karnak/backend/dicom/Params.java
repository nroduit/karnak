/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.dicom;

import java.io.InputStream;
import org.dcm4che3.net.Association;

public final class Params {

	private final String iuid;

	private final String cuid;

	private final String tsuid;

	private final InputStream data;

	private final Association as;

	private final int priority;

	public Params(String iuid, String cuid, String tsuid, int priority, InputStream data, Association as) {
		super();
		this.iuid = iuid;
		this.cuid = cuid;
		this.tsuid = tsuid;
		this.priority = priority;
		this.as = as;
		this.data = data;
	}

	public String getIuid() {
		return iuid;
	}

	public String getCuid() {
		return cuid;
	}

	public String getTsuid() {
		return tsuid;
	}

	public int getPriority() {
		return priority;
	}

	public Association getAs() {
		return as;
	}

	public InputStream getData() {
		return data;
	}

}
