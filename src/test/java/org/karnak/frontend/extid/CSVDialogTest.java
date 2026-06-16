/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.extid;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ProjectEntity;

class CSVDialogTest {

	@Test
	void should_create_csv_dialog() {
		// Init data
		ProjectEntity projectEntity = new ProjectEntity();
		char separator = ';';

		InputStream inputStream = new ByteArrayInputStream(new byte[0]);

		// Call service
		CSVDialog csvDialog = new CSVDialog(inputStream, separator, projectEntity);

		// Test results
		assertNotNull(csvDialog);
	}

}
