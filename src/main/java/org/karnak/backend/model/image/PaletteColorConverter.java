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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Fragments;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.img.stream.BytesWithImageDescriptor;
import org.dcm4che3.img.stream.ImageDescriptor;
import org.dcm4che3.img.util.PaletteColorUtils;
import org.dcm4che3.img.util.PixelDataUtils;
import org.jspecify.annotations.Nullable;
import org.opencv.core.CvType;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.LookupTableCV;
import org.weasis.opencv.data.PlanarImage;

/**
 * Applies the Palette Color LUT of an uncompressed instance and rewrites it as a plain
 * RGB instance.
 *
 * <p>
 * This is needed before masking or defacing a {@code PALETTE COLOR} image: the codec
 * decodes such an image to 8-bit RGB, but the writer takes {@code Bits Allocated} /
 * {@code Bits Stored} from the source dataset while taking {@code Samples per Pixel} from
 * the decoded image. A 16-bit palette instance is therefore stored as "3 samples of 16
 * bits" holding 8-bit samples: the receiver reads several frames as one and the colors
 * shift. Converting the dataset up front keeps the whole pipeline consistent, and loses
 * nothing since the transformation already outputs RGB.
 */
@Slf4j
public final class PaletteColorConverter {

	private static final String PALETTE_COLOR = "PALETTE COLOR";

	private static final int RGB_SAMPLES = 3;

	/** Palette attributes made obsolete once the LUT is applied. */
	private static final int[] PALETTE_TAGS = { Tag.RedPaletteColorLookupTableDescriptor,
			Tag.GreenPaletteColorLookupTableDescriptor, Tag.BluePaletteColorLookupTableDescriptor,
			Tag.PaletteColorLookupTableUID, Tag.RedPaletteColorLookupTableData, Tag.GreenPaletteColorLookupTableData,
			Tag.BluePaletteColorLookupTableData, Tag.SegmentedRedPaletteColorLookupTableData,
			Tag.SegmentedGreenPaletteColorLookupTableData, Tag.SegmentedBluePaletteColorLookupTableData };

	private PaletteColorConverter() {
	}

	/**
	 * Converts, in place, an uncompressed {@code PALETTE COLOR} dataset to RGB.
	 * @param attributes the dataset about to be transformed
	 * @return a descriptor over the converted pixel data, or {@code null} when the
	 * dataset is left untouched (not a palette image, compressed pixel data, unusable
	 * LUT, …)
	 */
	public static @Nullable BytesWithImageDescriptor convertToRgb(Attributes attributes) {
		if (attributes == null || !PALETTE_COLOR.equals(attributes.getString(Tag.PhotometricInterpretation))) {
			return null;
		}

		Object pixelData = attributes.getValue(Tag.PixelData);
		if (pixelData == null || pixelData instanceof Fragments) {
			// Compressed pixel data is decoded then re-encoded by the codec, which
			// derives every tag from the decoded image: nothing to fix up front.
			return null;
		}

		int rows = attributes.getInt(Tag.Rows, 0);
		int columns = attributes.getInt(Tag.Columns, 0);
		int bitsAllocated = attributes.getInt(Tag.BitsAllocated, 0);
		int frames = Math.max(attributes.getInt(Tag.NumberOfFrames, 1), 1);
		if (rows <= 0 || columns <= 0 || (bitsAllocated != 8 && bitsAllocated != 16)) {
			log.warn(
					"Cannot apply the Palette Color LUT of instance {}: unsupported geometry "
							+ "(rows={}, columns={}, bits allocated={})",
					attributes.getString(Tag.SOPInstanceUID), rows, columns, bitsAllocated);
			return null;
		}

		LookupTableCV lut = paletteLookupTable(attributes);
		if (lut == null) {
			return null;
		}

		byte[] indexes;
		try {
			indexes = attributes.getBytes(Tag.PixelData);
		}
		catch (IOException e) {
			log.warn("Cannot read the pixel data of PALETTE COLOR instance {}: {}",
					attributes.getString(Tag.SOPInstanceUID), e.getMessage());
			return null;
		}

		int frameLength = rows * columns * (bitsAllocated / 8);
		if (indexes == null || indexes.length < frames * frameLength) {
			log.warn("Pixel data of PALETTE COLOR instance {} holds {} bytes for {} frame(s) of {} bytes",
					attributes.getString(Tag.SOPInstanceUID), indexes == null ? 0 : indexes.length, frames,
					frameLength);
			return null;
		}

		byte[] rgb = applyLut(indexes, lut, rows, columns, frames, bitsAllocated);
		updateAttributes(attributes, rgb);
		log.info("PALETTE COLOR instance {} converted to RGB before the image transformation",
				attributes.getString(Tag.SOPInstanceUID));
		// The image descriptor must describe the converted dataset, and the converted
		// pixels are held in memory: the library only reads them from a bulk-data
		// reference or from fragments.
		return new RgbBytesDescriptor(new ImageDescriptor(attributes), rgb, rows * columns * RGB_SAMPLES);
	}

	/**
	 * In-memory view of the converted pixel data, as expected by
	 * {@link org.dcm4che3.img.stream.ImageAdapter#buildDataWriter}.
	 */
	private record RgbBytesDescriptor(ImageDescriptor imageDescriptor, byte[] pixelData,
			int frameLength) implements BytesWithImageDescriptor {

		@Override
		public ImageDescriptor getImageDescriptor() {
			return imageDescriptor;
		}

		@Override
		public ByteBuffer getBytes(int frame) throws IOException {
			int offset = frame * frameLength;
			if (frame < 0 || offset + frameLength > pixelData.length) {
				throw new IOException("Frame " + frame + " exceeds the converted pixel data");
			}
			// The frame must be copied into its own array: the reader takes the whole
			// backing array of the buffer, ignoring its position and array offset, so a
			// view over the multi-frame buffer would decode frame 0 every time.
			return ByteBuffer.wrap(Arrays.copyOfRange(pixelData, offset, offset + frameLength));
		}

		@Override
		public String getTransferSyntax() {
			// The converted samples are raw little-endian bytes
			return UID.ExplicitVRLittleEndian;
		}

		@Override
		public VR getPixelDataVR() {
			return VR.OB;
		}
	}

	/**
	 * Builds the Palette Color LUT of the instance, or returns {@code null} when it
	 * cannot be built.
	 *
	 * <p>
	 * The library reports an unusable palette either by returning {@code null} (missing
	 * descriptors or data) or by throwing {@link IllegalArgumentException} (descriptor
	 * holding something else than 3 values, bits per entry outside {8, 16}, entry count
	 * inconsistent with the data). Neither must abort the transfer: an instance whose
	 * palette cannot be applied is forwarded with its pixel data untouched.
	 */
	private static @Nullable LookupTableCV paletteLookupTable(Attributes attributes) {
		LookupTableCV lut;
		try {
			lut = PaletteColorUtils.getPaletteColorLookupTable(attributes);
		}
		catch (IllegalArgumentException e) {
			log.warn("PALETTE COLOR instance {} has a malformed LUT ({}) - keeping the pixel data as is",
					attributes.getString(Tag.SOPInstanceUID), e.getMessage());
			return null;
		}
		if (lut == null) {
			log.warn("PALETTE COLOR instance {} has no usable LUT - keeping the pixel data as is",
					attributes.getString(Tag.SOPInstanceUID));
		}
		return lut;
	}

	private static byte[] applyLut(byte[] indexes, LookupTableCV lut, int rows, int columns, int frames,
			int bitsAllocated) {
		int frameLength = rows * columns * (bitsAllocated / 8);
		int rgbFrameLength = rows * columns * RGB_SAMPLES;
		byte[] rgb = new byte[rgbFrameLength * frames];
		byte[] frameBuffer = new byte[rgbFrameLength];

		for (int frame = 0; frame < frames; frame++) {
			ImageCV indexImage = indexImage(indexes, frame * frameLength, rows, columns, bitsAllocated);
			PlanarImage colorImage = null;
			PlanarImage rgbImage = null;
			try {
				// The palette LUT is built with OpenCV's BGR band order: the samples
				// must be swapped back to the RGB order the dataset will declare.
				colorImage = PaletteColorUtils.getRGBImageFromPaletteColorModel(indexImage, lut);
				rgbImage = PixelDataUtils.bgr2rgb(colorImage);
				rgbImage.toMat().get(0, 0, frameBuffer);
				System.arraycopy(frameBuffer, 0, rgb, frame * rgbFrameLength, rgbFrameLength);
			}
			finally {
				release(indexImage, colorImage, rgbImage);
			}
		}
		return rgb;
	}

	private static ImageCV indexImage(byte[] indexes, int offset, int rows, int columns, int bitsAllocated) {
		if (bitsAllocated == 8) {
			ImageCV image = new ImageCV(rows, columns, CvType.CV_8UC1);
			byte[] frame = new byte[rows * columns];
			System.arraycopy(indexes, offset, frame, 0, frame.length);
			image.put(0, 0, frame);
			return image;
		}
		ImageCV image = new ImageCV(rows, columns, CvType.CV_16UC1);
		short[] frame = new short[rows * columns];
		ByteBuffer.wrap(indexes, offset, frame.length * Short.BYTES)
			.order(ByteOrder.LITTLE_ENDIAN)
			.asShortBuffer()
			.get(frame);
		image.put(0, 0, frame);
		return image;
	}

	private static void release(PlanarImage... images) {
		for (PlanarImage image : images) {
			if (image != null && !image.isReleased()) {
				image.release();
			}
		}
	}

	private static void updateAttributes(Attributes attributes, byte[] rgb) {
		attributes.setInt(Tag.SamplesPerPixel, VR.US, RGB_SAMPLES);
		attributes.setString(Tag.PhotometricInterpretation, VR.CS, "RGB");
		attributes.setInt(Tag.PlanarConfiguration, VR.US, 0);
		attributes.setInt(Tag.BitsAllocated, VR.US, 8);
		attributes.setInt(Tag.BitsStored, VR.US, 8);
		attributes.setInt(Tag.HighBit, VR.US, 7);
		attributes.setInt(Tag.PixelRepresentation, VR.US, 0);
		for (int tag : PALETTE_TAGS) {
			attributes.remove(tag);
		}
		attributes.setBytes(Tag.PixelData, VR.OB, rgb);
	}

}
