/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.dicom;

import java.security.SecureRandom;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.opencv.core.Core.MinMaxLocResult;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;
import org.weasis.opencv.op.ImageProcessor;

public class DefacingUtil {

	private DefacingUtil() {
	}

	public static int randomY(int minY, int maxY, int bound) {
		SecureRandom random = new SecureRandom();
		return (int) Math.floor(random.nextDouble() * (maxY - minY + bound) + minY);
	}

	public static double pickRndYPxlColor(int xInit, int minY, int maxY, PlanarImage imgToPick) {
		int yRand = DefacingUtil.randomY(minY, maxY, 1);
		return imgToPick.toMat().get(yRand, xInit)[0];
	}

	public static double pickRndYPxlColorConvolution(int xInit, int minY, int maxY, PlanarImage imgToPick) {
		int yRand = DefacingUtil.randomY(minY, maxY, 1);
		int size = 5;
		double mean = 0;
		int sum = 0;
		int imgWidth = imgToPick.width();
		int imgHeight = imgToPick.height();

		// convolution
		for (int x = xInit - (size / 2); x < xInit + (size / 2) + 1; x++) {
			for (int y = yRand; y < yRand + size + 1; y++) {
				int xPickColor = checkBoundsOfImageX(x, imgWidth);
				int yPickColor = checkBoundsOfImageY(y, imgHeight);
				double color = imgToPick.toMat().get(yPickColor, xPickColor)[0];

				mean = mean + color;
				sum++;
			}
		}
		if (sum != 0) {
			return mean / sum;
		}
		return mean;
	}

	public static int checkBoundsOfImageX(int x, int imgWidth) {
		if (x < 0) {
			return 0;
		}
		if (x >= imgWidth) {
			return imgWidth - 1;
		}
		return x;
	}

	public static int checkBoundsOfImageY(int y, int imgHeight) {
		if (y < 0) {
			return 0;
		}
		if (y >= imgHeight) {
			return imgHeight - 1;
		}
		return y;
	}

	public static PlanarImage transformToByte(PlanarImage srcImg) {
		ImageCV imgTransform = new ImageCV();
		srcImg.toMat().copyTo(imgTransform);

		MinMaxLocResult minMaxLocResult = ImageProcessor.findMinMaxValues(imgTransform.toMat());
		double min = minMaxLocResult.minVal;
		double max = minMaxLocResult.maxVal;
		double slope = 255.0 / (max - min);
		double yint = 255.0 - slope * max;
		imgTransform = ImageProcessor.rescaleToByte(imgTransform.toImageCV(), slope, yint);
		return imgTransform;
	}

	public static double hounsfieldToPxlValue(Attributes attributes, double hounsfield) {
		double intercept = attributes.getDouble(Tag.RescaleIntercept, 0);
		double slope = attributes.getDouble(Tag.RescaleSlope, 1.0);
		return (hounsfield - intercept) / slope;
	}

	public static PlanarImage rescaleForVisualizing(PlanarImage srcImg, Double contrast, Double brigtness) {
		ImageCV imageForVisualizing = new ImageCV();
		srcImg.toMat().copyTo(imageForVisualizing);
		PlanarImage transformImg = transformToByte(imageForVisualizing);
		transformImg = ImageProcessor.rescaleToByte(transformImg.toImageCV(), contrast / 100.0, brigtness);
		return transformImg;
	}

	public static boolean isCT(Attributes attributes) {
		String sopClassUID = attributes.getString(Tag.SOPClassUID);
		return (sopClassUID + ".").startsWith("1.2.840.10008.5.1.4.1.1.2.");
	}

	public static boolean isAxial(Attributes attributes) {
		double[] vector = attributes.getDoubles(Tag.ImageOrientationPatient);
		ImageOrientation.Label label = ImageOrientation.makeImageOrientationLabelFromImageOrientationPatient(vector);
		return label.equals(ImageOrientation.Label.AXIAL);
	}

}
