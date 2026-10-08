/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.event;

import org.karnak.backend.model.validation.InstanceConformanceData;
import org.springframework.context.ApplicationEvent;

/**
 * Published for every instance forwarded to a destination whose
 * {@code buildConformanceReport} option is enabled.
 */
public class ConformanceCollectEvent extends ApplicationEvent {

	public ConformanceCollectEvent(InstanceConformanceData instanceConformanceData) {
		super(instanceConformanceData);
	}

	public InstanceConformanceData getInstanceConformanceData() {
		return (InstanceConformanceData) getSource();
	}

}
