/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.service.TransferMonitoringService;
import org.mockito.Mockito;

class MonitoringLogicTest {

	// Service mock
	private final TransferMonitoringService transferMonitoringServiceMock = Mockito
		.mock(TransferMonitoringService.class);

	// Logic under test
	private MonitoringLogic monitoringLogic;

	@BeforeEach
	void setUp() {
		monitoringLogic = new MonitoringLogic(transferMonitoringServiceMock);
	}

	@Test
	void shouldDeleteAllTransferStatus() {
		// Call logic
		monitoringLogic.deleteAllTransferStatus();

		// Verify the service method is called exactly once
		Mockito.verify(transferMonitoringServiceMock, Mockito.times(1)).deleteAllTransferStatus();
	}

}
