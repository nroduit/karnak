/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.constant;

public class EndPoint {

	// Paths
	public static final String ECHO_PATH = "/api/echo";

	public static final String DESTINATIONS_PATH = "/destinations";

	// public static final String FILE_PATH = "/file";
	// public static final String DOWNLOAD_SOPUID_PATH = "/download/aet/sopuid";

	// Params
	public static final String SRC_AET_PARAM = "srcAet";

	// public static final String AET_PARAM = "aet";
	// public static final String SOP_UID_PARAM = "sopUid";

	public static final String ALL_REMAINING_PATH = "/**";

	private EndPoint() {
	}

}
