/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
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

	public static final String DESTINATIONS_PATH = "/destinations";

	public static final String API_PATH = "/api";

	public static final String ECHO_PATH = API_PATH + "/echo";

	public static final String FORWARD_NODES_PATH = API_PATH + "/forward-nodes";

	public static final String PROFILES_PATH = API_PATH + "/profiles";

	public static final String PROJECTS_PATH = API_PATH + "/projects";

	public static final String MONITORING_PATH = API_PATH + "/monitoring";

	public static final String PSEUDONYMS_PATH = API_PATH + "/pseudonyms";

	public static final String ALL_REMAINING_PATH = "/**";

	// Params
	public static final String SRC_AET_PARAM = "srcAet";

	private EndPoint() {
	}

}
