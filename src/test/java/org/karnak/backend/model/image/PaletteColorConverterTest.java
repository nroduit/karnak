/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.image;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.img.stream.BytesWithImageDescriptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.weasis.opencv.natives.NativeLibrary;

@DisplayNameGeneration(ReplaceUnderscores.class)
class PaletteColorConverterTest {

	@BeforeAll
	static void loadOpenCv() {
		NativeLibrary.loadLibraryFromLibraryName();
	}

	@Test
	void a_monochrome_instance_is_left_untouched() {
		Attributes attributes = baseImage(2, 2, 8);
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "MONOCHROME2");
		attributes.setBytes(Tag.PixelData, VR.OB, new byte[] { 1, 2, 3, 4 });

		assertThat(PaletteColorConverter.convertToRgb(attributes)).isNull();
		assertThat(attributes.getString(Tag.PhotometricInterpretation)).isEqualTo("MONOCHROME2");
	}

	@Test
	void a_palette_instance_without_lut_is_left_untouched() {
		Attributes attributes = baseImage(2, 2, 8);
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		attributes.setBytes(Tag.PixelData, VR.OB, new byte[] { 1, 2, 3, 4 });

		assertThat(PaletteColorConverter.convertToRgb(attributes)).isNull();
		assertThat(attributes.getString(Tag.PhotometricInterpretation)).isEqualTo("PALETTE COLOR");
	}

	@Test
	void a_palette_instance_with_a_malformed_descriptor_is_left_untouched() {
		Attributes attributes = baseImage(2, 2, 8);
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		addRampPalette(attributes);
		// 12 bits per entry is not a legal value: the library rejects it by throwing
		attributes.setInt(Tag.RedPaletteColorLookupTableDescriptor, VR.US, 256, 0, 12);
		attributes.setBytes(Tag.PixelData, VR.OB, new byte[] { 1, 2, 3, 4 });

		assertThat(PaletteColorConverter.convertToRgb(attributes)).isNull();
		assertThat(attributes.getString(Tag.PhotometricInterpretation)).isEqualTo("PALETTE COLOR");
		assertThat(attributes.getInt(Tag.BitsAllocated, 0)).isEqualTo(8);
	}

	@Test
	void a_palette_instance_with_truncated_lut_data_is_left_untouched() {
		Attributes attributes = baseImage(2, 2, 8);
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		addRampPalette(attributes);
		// The descriptor declares 256 entries but only 100 are present
		attributes.setBytes(Tag.RedPaletteColorLookupTableData, VR.OW, new byte[100]);
		attributes.setBytes(Tag.PixelData, VR.OB, new byte[] { 1, 2, 3, 4 });

		assertThat(PaletteColorConverter.convertToRgb(attributes)).isNull();
		assertThat(attributes.getString(Tag.PhotometricInterpretation)).isEqualTo("PALETTE COLOR");
		assertThat(attributes.contains(Tag.RedPaletteColorLookupTableData)).isTrue();
	}

	@Test
	void a_compressed_palette_instance_is_left_untouched() {
		Attributes attributes = baseImage(2, 2, 8);
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		addRampPalette(attributes);
		attributes.newFragments(Tag.PixelData, VR.OB, 2);

		assertThat(PaletteColorConverter.convertToRgb(attributes)).isNull();
		assertThat(attributes.getString(Tag.PhotometricInterpretation)).isEqualTo("PALETTE COLOR");
	}

	@Test
	void an_8bit_palette_instance_is_converted_to_rgb() throws IOException {
		Attributes attributes = baseImage(1, 2, 8);
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		addRampPalette(attributes);
		attributes.setBytes(Tag.PixelData, VR.OB, new byte[] { 0, 10 });

		BytesWithImageDescriptor descriptor = PaletteColorConverter.convertToRgb(attributes);

		assertThat(descriptor).isNotNull();
		assertThat(attributes.getString(Tag.PhotometricInterpretation)).isEqualTo("RGB");
		assertThat(attributes.getInt(Tag.SamplesPerPixel, 0)).isEqualTo(3);
		assertThat(attributes.getInt(Tag.BitsAllocated, 0)).isEqualTo(8);
		assertThat(attributes.getInt(Tag.BitsStored, 0)).isEqualTo(8);
		assertThat(attributes.getInt(Tag.HighBit, 0)).isEqualTo(7);
		assertThat(attributes.getInt(Tag.PlanarConfiguration, -1)).isZero();
		assertThat(attributes.contains(Tag.RedPaletteColorLookupTableData)).isFalse();
		assertThat(attributes.contains(Tag.RedPaletteColorLookupTableDescriptor)).isFalse();

		// The ramp palette maps the index i to (i, 0, 255 - i), in RGB order
		assertThat(attributes.getBytes(Tag.PixelData)).containsExactly(0, 0, 255, 10, 0, 245);
		assertThat(descriptor.getTransferSyntax()).isEqualTo(UID.ExplicitVRLittleEndian);
		assertThat(descriptor.getPixelDataVR()).isEqualTo(VR.OB);
		assertThat(descriptor.getImageDescriptor().getSamples()).isEqualTo(3);
		assertThat(descriptor.getImageDescriptor().getBitsAllocated()).isEqualTo(8);
	}

	@Test
	void a_16bit_multiframe_palette_instance_is_converted_frame_by_frame() throws IOException {
		int rows = 1;
		int columns = 2;
		int frames = 2;
		Attributes attributes = baseImage(rows, columns, 16);
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "PALETTE COLOR");
		attributes.setInt(Tag.NumberOfFrames, VR.IS, frames);
		addRampPalette(attributes);
		// 16-bit little-endian indexes: frame 0 = [1, 2], frame 1 = [3, 4]
		attributes.setBytes(Tag.PixelData, VR.OW, new byte[] { 1, 0, 2, 0, 3, 0, 4, 0 });

		BytesWithImageDescriptor descriptor = PaletteColorConverter.convertToRgb(attributes);

		assertThat(descriptor).isNotNull();
		assertThat(attributes.getInt(Tag.BitsAllocated, 0)).isEqualTo(8);
		assertThat(attributes.getBytes(Tag.PixelData)).hasSize(rows * columns * 3 * frames)
			.containsExactly(1, 0, 254, 2, 0, 253, 3, 0, 252, 4, 0, 251);

		// Each frame is exposed separately to the codec. The reader takes the whole
		// backing array of the buffer, so it must hold that frame and nothing else.
		assertThat(descriptor.getBytes(0).array()).containsExactly(1, 0, 254, 2, 0, 253);
		assertThat(descriptor.getBytes(1).array()).containsExactly(3, 0, 252, 4, 0, 251);
	}

	private static Attributes baseImage(int rows, int columns, int bitsAllocated) {
		Attributes attributes = new Attributes();
		attributes.setString(Tag.SOPInstanceUID, VR.UI, "1.2.3.4");
		attributes.setInt(Tag.Rows, VR.US, rows);
		attributes.setInt(Tag.Columns, VR.US, columns);
		attributes.setInt(Tag.SamplesPerPixel, VR.US, 1);
		attributes.setInt(Tag.BitsAllocated, VR.US, bitsAllocated);
		attributes.setInt(Tag.BitsStored, VR.US, bitsAllocated);
		attributes.setInt(Tag.HighBit, VR.US, bitsAllocated - 1);
		attributes.setInt(Tag.PixelRepresentation, VR.US, 0);
		return attributes;
	}

	/** Maps the index i to the color (i, 0, 255 - i). */
	private static void addRampPalette(Attributes attributes) {
		int[] descriptor = { 256, 0, 8 };
		attributes.setInt(Tag.RedPaletteColorLookupTableDescriptor, VR.US, descriptor);
		attributes.setInt(Tag.GreenPaletteColorLookupTableDescriptor, VR.US, descriptor);
		attributes.setInt(Tag.BluePaletteColorLookupTableDescriptor, VR.US, descriptor);
		byte[] red = new byte[256];
		byte[] green = new byte[256];
		byte[] blue = new byte[256];
		for (int i = 0; i < 256; i++) {
			red[i] = (byte) i;
			blue[i] = (byte) (255 - i);
		}
		attributes.setBytes(Tag.RedPaletteColorLookupTableData, VR.OW, red);
		attributes.setBytes(Tag.GreenPaletteColorLookupTableData, VR.OW, green);
		attributes.setBytes(Tag.BluePaletteColorLookupTableData, VR.OW, blue);
	}

}
