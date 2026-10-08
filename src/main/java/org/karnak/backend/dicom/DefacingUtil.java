/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
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
import org.weasis.dicom.geom.ImageOrientation;
import org.weasis.dicom.geom.ImageOrientation.Plan;
import org.weasis.dicom.geom.Vector3;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;
import org.weasis.opencv.op.ImageAnalyzer;
import org.weasis.opencv.op.ImageTransformer;

public class DefacingUtil {

	// CT Image Storage and its derived SOP Classes (Enhanced CT, ...)
	private static final String CT_SOP_CLASS_UID_PREFIX = "1.2.840.10008.5.1.4.1.1.2.";

	private static final SecureRandom RANDOM = new SecureRandom();

	private DefacingUtil() {
	}

	public static int randomY(int minY, int maxY, int bound) {
		return (int) Math.floor(RANDOM.nextDouble() * (maxY - minY + bound) + minY);
	}

	public static double pickRndYPxlColor(int xInit, int minY, int maxY, PlanarImage imgToPick) {
		int yRand = DefacingUtil.randomY(minY, maxY, 1);
		return imgToPick.toMat().get(yRand, xInit)[0];
	}

	public static PlanarImage transformToByte(PlanarImage srcImg) {
		ImageCV imgTransform = new ImageCV();
		srcImg.toMat().copyTo(imgTransform);

		MinMaxLocResult minMaxLocResult = ImageAnalyzer.findMinMaxValues(imgTransform.toMat());
		double min = minMaxLocResult.minVal;
		double max = minMaxLocResult.maxVal;
		double slope = 255.0 / (max - min);
		double yint = 255.0 - slope * max;
		imgTransform = ImageTransformer.rescaleToByte(imgTransform.toImageCV(), slope, yint);
		return imgTransform;
	}

	public static double hounsfieldToPxlValue(Attributes attributes, double hounsfield) {
		double intercept = attributes.getDouble(Tag.RescaleIntercept, 0);
		double slope = attributes.getDouble(Tag.RescaleSlope, 1.0);
		return (hounsfield - intercept) / slope;
	}

	public static boolean isCT(Attributes attributes) {
		String sopClassUID = attributes.getString(Tag.SOPClassUID);
		return sopClassUID != null && (sopClassUID + ".").startsWith(CT_SOP_CLASS_UID_PREFIX);
	}

	public static boolean isAxial(Attributes attributes) {
		double[] iop = attributes.getDoubles(Tag.ImageOrientationPatient);
		if (iop == null || iop.length < 6) {
			return false;
		}
		return ImageOrientation.getPlan(Vector3.of(iop, 0), Vector3.of(iop, 3)) == Plan.TRANSVERSE;
	}

}
