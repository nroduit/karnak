/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.event;

import org.karnak.backend.model.monitoring.MonitoringEntry;
import org.springframework.context.ApplicationEvent;

/**
 * Transfer monitoring event used to fold a transfer outcome asynchronously into the
 * aggregated transfer_series_status table.
 */
public class TransferMonitoringEvent extends ApplicationEvent {

	public TransferMonitoringEvent(MonitoringEntry monitoringEntry) {
		super(monitoringEntry);
	}

	public MonitoringEntry getEntry() {
		return (MonitoringEntry) getSource();
	}

}
