/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicom;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.enums.Modality;

@DisplayNameGeneration(ReplaceUnderscores.class)
class WorkListQueryDataTest {

	@Test
	void initialises_with_defaults() {
		WorkListQueryData data = new WorkListQueryData();

		assertEquals("DCM-TOOLS", data.getCallingAet());
		assertEquals(Modality.ALL, data.getScheduledModality());
	}

	@Test
	void stores_all_query_fields() {
		WorkListQueryData data = new WorkListQueryData();
		LocalDate from = LocalDate.of(2026, 1, 1);
		LocalDate to = LocalDate.of(2026, 1, 31);
		data.setCallingAet("CALLER");
		data.setWorkListAet("WL");
		data.setWorkListHostname("host");
		data.setWorkListPort(11112);
		data.setScheduledStationAet("STATION");
		data.setScheduledModality(Modality.CT);
		data.setPatientId("PID");
		data.setAdmissionId("ADM");
		data.setScheduledFrom(from);
		data.setScheduledTo(to);
		data.setPatientName("Doe^John");
		data.setAccessionNumber("ACC");

		assertEquals("CALLER", data.getCallingAet());
		assertEquals("WL", data.getWorkListAet());
		assertEquals("host", data.getWorkListHostname());
		assertEquals(11112, data.getWorkListPort());
		assertEquals("STATION", data.getScheduledStationAet());
		assertEquals(Modality.CT, data.getScheduledModality());
		assertEquals("PID", data.getPatientId());
		assertEquals("ADM", data.getAdmissionId());
		assertEquals(from, data.getScheduledFrom());
		assertEquals(to, data.getScheduledTo());
		assertEquals("Doe^John", data.getPatientName());
		assertEquals("ACC", data.getAccessionNumber());
	}

	@Test
	void reset_restores_the_calling_aet_and_modality_defaults() {
		WorkListQueryData data = new WorkListQueryData();
		data.setCallingAet("CALLER");
		data.setScheduledModality(Modality.MR);

		data.reset();

		assertEquals("DCM-TOOLS", data.getCallingAet());
		assertEquals(Modality.ALL, data.getScheduledModality());
	}

}