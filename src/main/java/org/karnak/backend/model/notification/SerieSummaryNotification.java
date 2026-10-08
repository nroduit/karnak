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
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.constant.Notification;

/**
 * Model used for serie summary notification
 */
@Setter
@Getter
@EqualsAndHashCode
@ToString
@NullUnmarked
public class SerieSummaryNotification {

	private String serieUid;

	private String serieDescription;

	private LocalDateTime serieDate;

	private long nbTransferSent;

	private long nbTransferNotSent;

	private boolean containsError;

	private Set<String> unTransferedReasons;

	private Set<String> transferredModalities;

	private Set<String> transferredSopClassUid;

	public String toStringUnTransferredReasons() {
		return String.join(Notification.COMMA_SEPARATOR, unTransferedReasons);
	}

	public String toStringTransferredModalities() {
		return String.join(Notification.COMMA_SEPARATOR, transferredModalities);
	}

	public String toStringTransferredSopClassUid() {
		return String.join(Notification.COMMA_SEPARATOR, transferredSopClassUid);
	}

}
