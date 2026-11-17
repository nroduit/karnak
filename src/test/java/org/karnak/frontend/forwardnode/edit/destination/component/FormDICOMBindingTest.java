/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.destination.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.enums.DestinationType;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Round trip of the DICOM destination form through its binder, without a UI: the bindings
 * must accept the entity defaults (notably null optional numbers) on read and write back
 * what the user typed.
 */
class FormDICOMBindingTest {

	private NewUpdateDestination newUpdateDestination;

	private Binder<DestinationEntity> binder;

	private FormDICOM form;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		newUpdateDestination = new NewUpdateDestination();
		binder = (Binder<DestinationEntity>) ReflectionTestUtils.getField(newUpdateDestination, "binderFormDICOM");
		form = (FormDICOM) ReflectionTestUtils.getField(newUpdateDestination, "formDICOM");
	}

	private TextField field(String name) {
		return (TextField) ReflectionTestUtils.getField(form, name);
	}

	private void fillMandatoryFields() {
		field("aeTitle").setValue("PACS");
		field("hostname").setValue("pacs.example.org");
		field("port").setValue("104");
	}

	@Test
	void new_dicom_destination_loads_with_empty_response_timeout() {
		newUpdateDestination.load(null, DestinationType.dicom);

		assertEquals("", field("cstoreResponseTimeout").getValue());
		assertEquals("1", field("concurrentConnections").getValue());
	}

	@Test
	void empty_response_timeout_is_written_as_null() throws Exception {
		newUpdateDestination.load(null, DestinationType.dicom);
		fillMandatoryFields();
		DestinationEntity entity = newUpdateDestination.getCurrentDestinationEntity();

		assertTrue(binder.writeBeanIfValid(entity));

		assertNull(entity.getCstoreResponseTimeout());
	}

	@Test
	void response_timeout_round_trips() throws Exception {
		newUpdateDestination.load(null, DestinationType.dicom);
		fillMandatoryFields();
		field("cstoreResponseTimeout").setValue("180");
		DestinationEntity entity = newUpdateDestination.getCurrentDestinationEntity();

		assertTrue(binder.writeBeanIfValid(entity));
		assertEquals(180, entity.getCstoreResponseTimeout());

		newUpdateDestination.load(entity, DestinationType.dicom);
		assertEquals("180", field("cstoreResponseTimeout").getValue());

		entity.setCstoreResponseTimeout(null);
		newUpdateDestination.load(entity, DestinationType.dicom);
		assertEquals("", field("cstoreResponseTimeout").getValue());
	}

	@Test
	void response_timeout_out_of_range_is_rejected() {
		newUpdateDestination.load(null, DestinationType.dicom);
		fillMandatoryFields();
		field("cstoreResponseTimeout").setValue("4000");

		assertFalse(binder.writeBeanIfValid(newUpdateDestination.getCurrentDestinationEntity()));
	}

}
