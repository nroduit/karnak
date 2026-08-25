/*
 * Copyright (c) 2009-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.img.op.MaskArea;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.io.DicomInputStream.IncludeBulkData;
import org.dcm4che3.io.DicomOutputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.dicom.Defacer;
import org.karnak.backend.dicom.DicomForwardDestination;
import org.karnak.backend.dicom.ForwardDicomNode;
import org.karnak.backend.dicom.Params;
import org.springframework.context.ApplicationEventPublisher;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.opencv.natives.NativeLibrary;

/**
 * Image-processing forwarding over real associations: a pixel-bearing object is forwarded
 * through {@link ForwardService} while an editor configures a mask, a transcoding output
 * transfer syntax, or defacing. The destination SCP's stored pixels / transfer syntax are
 * read back to assert the transformation actually happened on the wire.
 *
 * <p>
 * These tests exercise the native OpenCV codec path. The library is loaded in
 * {@link #loadOpenCv()} from {@code java.library.path} (which the Maven build points at
 * the bundled {@code libopencv_java}); the test hard-fails if it is absent, so run it
 * through Maven (or set the library path manually in the IDE).
 */
@org.junit.jupiter.api.Tag("integration")
@org.junit.jupiter.api.Tag("image-processing")
@DisplayNameGeneration(ReplaceUnderscores.class)
class ImageProcessingForwardIntegrationTest extends GatewayItTestSupport {

	private static final String CUID_CT = UID.CTImageStorage;

	private ForwardDicomNode fwdNode;

	private ForwardService forwardService;

	@BeforeAll
	static void loadOpenCv() {
		NativeLibrary.loadLibraryFromLibraryName();
	}

	@BeforeEach
	void setUp() {
		fwdNode = new ForwardDicomNode("SOURCE-IT");
		forwardService = new ForwardService(mock(ApplicationEventPublisher.class), null);
	}

	@Test
	void mask_blackens_the_configured_rectangle_and_leaves_the_rest_untouched() throws Exception {
		Scp scp = startScp();
		int rows = 64;
		int cols = 64;
		byte[] pixels = filled(rows, cols, (byte) 200);
		String iuid = "1.2.826.0.1.3680043.8.498.1";
		byte[] object = serialize8bit(iuid, CUID_SC, rows, cols, pixels);

		// Black out a 16x16 rectangle at (col=16, row=16).
		Rectangle rect = new Rectangle(16, 16, 16, 16);
		DicomForwardDestination dest = destination(scp, null, maskEditor(rect, Color.BLACK));
		try {
			forwardService.storeMultipleDestination(fwdNode, List.of(dest), params8bit(iuid, object));
		}
		finally {
			dest.stop();
		}

		byte[] received = receivedPixelData(scp.storageDir(), iuid);
		assertEquals(200, pixel(received, cols, 8, 8), "a pixel outside the mask must be unchanged");
		assertEquals(0, pixel(received, cols, 24, 24), "a pixel inside the mask must be blacked out");
	}

	@Test
	void transcode_rewrites_an_uncompressed_object_to_a_compressed_transfer_syntax() throws Exception {
		Scp scp = startScp();
		int rows = 64;
		int cols = 64;
		String iuid = "1.2.826.0.1.3680043.8.498.2";
		byte[] object = serialize8bit(iuid, CUID_SC, rows, cols, gradient(rows, cols));

		DicomForwardDestination dest = destination(scp, UID.JPEGLosslessSV1, NOOP);
		try {
			forwardService.storeMultipleDestination(fwdNode, List.of(dest), params8bit(iuid, object));
		}
		finally {
			dest.stop();
		}

		assertEquals(UID.JPEGLosslessSV1, receivedTransferSyntaxes(scp.storageDir()).get(iuid),
				"the object should be stored in the configured compressed transfer syntax");
	}

	@Test
	void defacing_modifies_an_axial_ct_image() throws Exception {
		// NOTE: synthetic CT (a bright soft-tissue blob on an air background), not a real
		// head.
		// It validates that the defacing path runs end-to-end through the gateway and
		// modifies
		// the stored pixels; it does not assess face-detection quality. Drop a real axial
		// CT
		// fixture in for that.
		Scp defaced = startScp();
		Scp untouched = startScp();
		int size = 128;
		String iuid = "1.2.826.0.1.3680043.8.498.3";
		byte[] object = serializeAxialCt(iuid, size, syntheticHead(size));

		DicomForwardDestination defacingDest = destination(defaced, null, defacingEditor());
		DicomForwardDestination plainDest = destination(untouched, null, NOOP);
		try {
			forwardService.storeMultipleDestination(fwdNode, List.of(defacingDest), params(iuid, CUID_CT, object));
			forwardService.storeMultipleDestination(fwdNode, List.of(plainDest), params(iuid, CUID_CT, object));
		}
		finally {
			defacingDest.stop();
			plainDest.stop();
		}

		byte[] defacedPixels = receivedPixelData(defaced.storageDir(), iuid);
		byte[] plainPixels = receivedPixelData(untouched.storageDir(), iuid);
		assertEquals(plainPixels.length, defacedPixels.length, "image geometry must be preserved");
		assertFalse(java.util.Arrays.equals(defacedPixels, plainPixels), "defacing must change the stored pixels");
		// The forwarded-as-is image is the original; defacing must differ from it too.
		assertNotEquals(0, defacedPixels.length);
	}

	// --- editors
	// --------------------------------------------------------------------------

	private static AttributeEditor maskEditor(Rectangle rect, Color color) {
		List<Shape> shapes = List.of(rect);
		return (attributes, context) -> context.setMaskArea(new MaskArea(shapes, color));
	}

	private static AttributeEditor defacingEditor() {
		return (attributes, context) -> context.getProperties().setProperty(Defacer.APPLY_DEFACING, "true");
	}

	// --- destinations / params
	// ------------------------------------------------------------

	private DicomForwardDestination destination(Scp scp, String outputTs, AttributeEditor editor) throws IOException {
		return new DicomForwardDestination((long) scp.port(), advancedParams(), fwdNode, scp.node(), false, null,
				List.of(editor), outputTs, true, 1);
	}

	private Params params8bit(String iuid, byte[] object) {
		return params(iuid, CUID_SC, object);
	}

	private Params params(String iuid, String cuid, byte[] object) {
		return new Params(iuid, cuid, TS_EXPLICIT, 0, new java.io.ByteArrayInputStream(object), null);
	}

	// --- pixel-bearing fixtures
	// -----------------------------------------------------------

	private static byte[] filled(int rows, int cols, byte value) {
		byte[] pixels = new byte[rows * cols];
		java.util.Arrays.fill(pixels, value);
		return pixels;
	}

	private static byte[] gradient(int rows, int cols) {
		byte[] pixels = new byte[rows * cols];
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				pixels[r * cols + c] = (byte) ((r + c) & 0xFF);
			}
		}
		return pixels;
	}

	/**
	 * A bright disc (soft tissue) centred in the upper half over an air background, as
	 * 16-bit little-endian pixel bytes.
	 */
	private static byte[] syntheticHead(int size) {
		byte[] pixels = new byte[size * size * 2];
		int cx = size / 2;
		int cy = size / 3;
		int radius = size / 4;
		for (int r = 0; r < size; r++) {
			for (int c = 0; c < size; c++) {
				int dx = c - cx;
				int dy = r - cy;
				// Air everywhere (~ -1000 HU), soft tissue inside the disc (~ +40 HU).
				// Stored
				// with a -1024 rescale intercept, so the pixel value is HU + 1024.
				int value = dx * dx + dy * dy <= radius * radius ? 1064 : 24;
				int i = (r * size + c) * 2;
				pixels[i] = (byte) (value & 0xFF);
				pixels[i + 1] = (byte) ((value >> 8) & 0xFF);
			}
		}
		return pixels;
	}

	private static byte[] serialize8bit(String iuid, String cuid, int rows, int cols, byte[] pixels)
			throws IOException {
		Attributes data = baseImage(iuid, cuid, rows, cols);
		data.setInt(Tag.BitsAllocated, VR.US, 8);
		data.setInt(Tag.BitsStored, VR.US, 8);
		data.setInt(Tag.HighBit, VR.US, 7);
		data.setInt(Tag.PixelRepresentation, VR.US, 0);
		data.setBytes(Tag.PixelData, VR.OB, pixels);
		return toDataset(data);
	}

	private static byte[] serializeAxialCt(String iuid, int size, byte[] pixels) throws IOException {
		Attributes data = baseImage(iuid, CUID_CT, size, size);
		data.setInt(Tag.BitsAllocated, VR.US, 16);
		data.setInt(Tag.BitsStored, VR.US, 16);
		data.setInt(Tag.HighBit, VR.US, 15);
		data.setInt(Tag.PixelRepresentation, VR.US, 0);
		data.setString(Tag.Modality, VR.CS, "CT");
		data.setDouble(Tag.ImageOrientationPatient, VR.DS, 1, 0, 0, 0, 1, 0); // axial
		data.setDouble(Tag.RescaleIntercept, VR.DS, -1024);
		data.setDouble(Tag.RescaleSlope, VR.DS, 1);
		data.setDouble(Tag.PixelSpacing, VR.DS, 0.5, 0.5);
		data.setBytes(Tag.PixelData, VR.OW, pixels);
		return toDataset(data);
	}

	private static Attributes baseImage(String iuid, String cuid, int rows, int cols) {
		Attributes data = new Attributes();
		data.setString(Tag.SOPClassUID, VR.UI, cuid);
		data.setString(Tag.SOPInstanceUID, VR.UI, iuid);
		data.setString(Tag.StudyInstanceUID, VR.UI, "1.2.826.0.1.3680043.8.498.100");
		data.setString(Tag.SeriesInstanceUID, VR.UI, "1.2.826.0.1.3680043.8.498.200");
		data.setString(Tag.PatientID, VR.LO, "IT-PATIENT");
		data.setString(Tag.PatientName, VR.PN, "Integration^Test");
		data.setString(Tag.Modality, VR.CS, "OT");
		data.setInt(Tag.SamplesPerPixel, VR.US, 1);
		data.setString(Tag.PhotometricInterpretation, VR.CS, "MONOCHROME2");
		data.setInt(Tag.Rows, VR.US, rows);
		data.setInt(Tag.Columns, VR.US, cols);
		return data;
	}

	private static byte[] toDataset(Attributes data) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (DicomOutputStream dos = new DicomOutputStream(baos, TS_EXPLICIT)) {
			dos.writeDataset(null, data);
		}
		return baos.toByteArray();
	}

	// --- read-back
	// ------------------------------------------------------------------------

	private static byte[] receivedPixelData(Path dir, String iuid) throws IOException {
		try (Stream<Path> files = Files.walk(dir)) {
			for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator) {
				try (DicomInputStream dis = new DicomInputStream(file.toFile())) {
					dis.setIncludeBulkData(IncludeBulkData.YES);
					Attributes data = dis.readDataset();
					if (iuid.equals(data.getString(Tag.SOPInstanceUID))) {
						return data.getBytes(Tag.PixelData);
					}
				}
			}
		}
		throw new IllegalStateException("No stored object found for " + iuid + " under " + dir);
	}

	private static int pixel(byte[] pixels, int cols, int row, int col) {
		return pixels[row * cols + col] & 0xFF;
	}

}
