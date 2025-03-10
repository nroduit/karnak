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

/**
 * Constants for notification
 */
public class Notification {

	// Default
	public static final String DEFAULT_SUBJECT_ERROR_PREFIX = "**ERROR**";

	public static final String DEFAULT_SUBJECT_PATTERN = "[Karnak Notification] %s %.30s";

	public static final String DEFAULT_SUBJECT_VALUES = "PatientID,StudyDescription";

	public static final String DEFAULT_INTERVAL = "45";

	// Thymeleaf
	public static final String CONTEXT_THYMELEAF = "notif";

	public static final String TEMPLATE_THYMELEAF = "transferNotificationEmail";

	// Separators
	public static final String COMMA_SEPARATOR = ",";

	public static final String EMPTY_STRING = "";

	public static final String SPACE = " ";

	// PARAMS
	public static final String PARAM_STUDY_DATE = "StudyDate";

	public static final String PARAM_STUDY_INSTANCE_UID = "StudyInstanceUID";

	public static final String PARAM_STUDY_DESCRIPTION = "StudyDescription";

	public static final String PARAM_PATIENT_ID = "PatientID";

	// Various
	public static final long EXTRA_TIMER_DELAY = 10;

	private Notification() {
	}

}
