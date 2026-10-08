/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.notification;

import java.time.LocalDateTime;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;

/**
 * Model used to build transfer monitoring notification
 */
@Setter
@Getter
@EqualsAndHashCode
@NullUnmarked
public class TransferMonitoringNotification {

	private String subject;

	private String from;

	private String to;

	private String patientId;

	private String studyUid;

	private String accessionNumber;

	private String studyDescription;

	private LocalDateTime studyDate;

	private String source;

	private String destination;

	private List<SerieSummaryNotification> serieSummaryNotifications;

	private boolean emailSendingEnabled;

}
