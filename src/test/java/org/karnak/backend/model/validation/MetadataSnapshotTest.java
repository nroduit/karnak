/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(ReplaceUnderscores.class)
class MetadataSnapshotTest {

	private static final int OVERLAY_DATA_60XX = 0x60023000;

	private static Attributes datasetWithBulkData() {
		var dcm = new Attributes();
		dcm.setString(Tag.SOPClassUID, VR.UI, "1.2.840.10008.5.1.4.1.1.2");
		dcm.setString(Tag.PatientName, VR.PN, "PSEUDO^A");
		dcm.setBytes(Tag.PixelData, VR.OW, new byte[100_000]);
		dcm.setBytes(OVERLAY_DATA_60XX, VR.OB, new byte[16]);
		dcm.setString(0x00091001, VR.LO, "private vendor value");

		Sequence sequence = dcm.newSequence(Tag.IconImageSequence, 1);
		var item = new Attributes();
		item.setInt(Tag.Rows, VR.US, 4);
		item.setBytes(Tag.PixelData, VR.OW, new byte[10_000]);
		sequence.add(item);
		return dcm;
	}

	@Test
	void strips_pixel_data_but_records_its_presence() {
		var snapshot = MetadataSnapshot.of(datasetWithBulkData());

		assertTrue(snapshot.metadata().contains(Tag.PixelData));
		assertFalse(snapshot.metadata().containsValue(Tag.PixelData));
		assertTrue(snapshot.bulkPresentTags().contains(Tag.PixelData));
	}

	@Test
	void strips_repeating_group_overlay_data() {
		var snapshot = MetadataSnapshot.of(datasetWithBulkData());

		assertFalse(snapshot.metadata().containsValue(OVERLAY_DATA_60XX));
		assertTrue(snapshot.bulkPresentTags().contains(OVERLAY_DATA_60XX));
	}

	@Test
	void copies_small_attributes_and_sequences() {
		var snapshot = MetadataSnapshot.of(datasetWithBulkData());

		assertEquals("PSEUDO^A", snapshot.metadata().getString(Tag.PatientName));
		Sequence sequence = snapshot.metadata().getSequence(Tag.IconImageSequence);
		assertEquals(1, sequence.size());
		assertEquals(4, sequence.get(0).getInt(Tag.Rows, 0));
	}

	@Test
	void strips_bulk_data_nested_in_sequence_items() {
		var snapshot = MetadataSnapshot.of(datasetWithBulkData());

		Attributes item = snapshot.metadata().getSequence(Tag.IconImageSequence).get(0);
		assertFalse(item.containsValue(Tag.PixelData));
	}

	@Test
	void retains_private_metadata() {
		var snapshot = MetadataSnapshot.of(datasetWithBulkData());

		assertEquals("private vendor value", snapshot.metadata().getString(0x00091001));
	}

	@Test
	void copies_sequence_items_of_a_dataset_declaring_a_specific_character_set() {
		var dcm = new Attributes();
		dcm.setString(Tag.SpecificCharacterSet, VR.CS, "ISO_IR 100");
		dcm.setString(Tag.SOPClassUID, VR.UI, "1.2.840.10008.5.1.4.1.1.2");
		dcm.setString(Tag.PatientName, VR.PN, "Dupont^Jérôme");
		var item = new Attributes();
		item.setBytes(Tag.CodeValue, VR.SH, "T-D3000".getBytes(StandardCharsets.ISO_8859_1));
		item.setBytes(Tag.CodeMeaning, VR.LO, "Poumon supérieur".getBytes(StandardCharsets.ISO_8859_1));
		dcm.newSequence(Tag.AnatomicRegionSequence, 1).add(item);

		var snapshot = MetadataSnapshot.of(dcm);

		assertEquals("Dupont^Jérôme", snapshot.metadata().getString(Tag.PatientName));
		Attributes copiedItem = snapshot.metadata().getSequence(Tag.AnatomicRegionSequence).get(0);
		assertEquals("T-D3000", copiedItem.getString(Tag.CodeValue));
		assertEquals("Poumon supérieur", copiedItem.getString(Tag.CodeMeaning));
	}

	@Test
	void copies_nested_sequence_items_of_a_dataset_declaring_a_specific_character_set() {
		var dcm = new Attributes();
		dcm.setString(Tag.SpecificCharacterSet, VR.CS, "ISO_IR 100");
		var inner = new Attributes();
		inner.setString(Tag.CodeValue, VR.SH, "T-D3000");
		inner.setString(Tag.CodeMeaning, VR.LO, "Poumon supérieur");
		var outer = new Attributes();
		outer.newSequence(Tag.ConceptNameCodeSequence, 1).add(inner);
		dcm.newSequence(Tag.ContentSequence, 1).add(outer);

		var snapshot = MetadataSnapshot.of(dcm);

		Attributes copiedInner = snapshot.metadata()
			.getSequence(Tag.ContentSequence)
			.get(0)
			.getSequence(Tag.ConceptNameCodeSequence)
			.get(0);
		assertEquals("Poumon supérieur", copiedInner.getString(Tag.CodeMeaning));
	}

	@Test
	void strips_private_bulk_values() {
		var dcm = new Attributes();
		dcm.setBytes(0x00091002, VR.OB, new byte[20_000]);

		var snapshot = MetadataSnapshot.of(dcm);

		assertTrue(snapshot.metadata().contains(0x00091002));
		assertFalse(snapshot.metadata().containsValue(0x00091002));
		assertTrue(snapshot.bulkPresentTags().contains(0x00091002));
	}

	@Test
	void large_unknown_binaries_are_stripped_as_bulk() {
		var dcm = new Attributes();
		dcm.setBytes(Tag.RedPaletteColorLookupTableData, VR.OW, new byte[65_536]);

		var snapshot = MetadataSnapshot.of(dcm);

		assertFalse(snapshot.metadata().containsValue(Tag.RedPaletteColorLookupTableData));
		assertTrue(snapshot.bulkPresentTags().contains(Tag.RedPaletteColorLookupTableData));
	}

}
