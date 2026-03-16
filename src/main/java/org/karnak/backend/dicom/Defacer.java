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

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.weasis.core.util.MathUtil;
import org.weasis.opencv.data.ImageCV;
import org.weasis.opencv.data.PlanarImage;
import org.weasis.opencv.op.ImageProcessor;

public class Defacer {

	public static final String APPLY_DEFACING = "defacing";

	private Defacer() {
	}

	public static PlanarImage apply(Attributes attributes, PlanarImage image) {
		PlanarImage faceDetectionImg = faceDetection(attributes, image);
		PlanarImage randPxlLineImg = addRandPxlLine(image, faceDetectionImg, attributes);
		PlanarImage mergedImg = mergeImg(image, randPxlLineImg, faceDetectionImg);
		return blurImg(mergedImg, faceDetectionImg);
	}

	public static PlanarImage filterBySkin(Attributes attributes, PlanarImage srcImg) {
		ImageCV skinImg = new ImageCV();
		srcImg.toMat().copyTo(skinImg);

		MinMaxLocResult minMaxLutFaceDetectionImg = ImageProcessor.findMinMaxValues(skinImg);

		Imgproc.threshold(skinImg.toImageCV(), skinImg.toMat(), DefacingUtil.hounsfieldToPxlValue(attributes, 100),
				minMaxLutFaceDetectionImg.maxVal, Imgproc.THRESH_TOZERO);
		Imgproc.threshold(skinImg.toImageCV(), skinImg.toMat(), DefacingUtil.hounsfieldToPxlValue(attributes, 300),
				minMaxLutFaceDetectionImg.maxVal, Imgproc.THRESH_TOZERO_INV);
		return skinImg;
	}

	public static PlanarImage faceDetection(Attributes attributes, PlanarImage srcImg) {
		ImageCV faceDetectionImg = new ImageCV();
		srcImg.toMat().copyTo(faceDetectionImg);

		MinMaxLocResult minMaxLocResult = ImageProcessor.findMinMaxValues(faceDetectionImg.toMat());

		Imgproc.threshold(faceDetectionImg.toImageCV(), faceDetectionImg.toMat(),
				DefacingUtil.hounsfieldToPxlValue(attributes, -500), minMaxLocResult.maxVal, Imgproc.THRESH_BINARY);

		// ERODE
		Mat kernel = new Mat();
		int kernelSize = 5;
		Mat ones = Mat.ones(kernelSize, kernelSize, CvType.CV_32F);
		Core.multiply(ones, new Scalar(1 / (double) (kernelSize * kernelSize)), kernel);
		Imgproc.erode(faceDetectionImg.toImageCV(), faceDetectionImg.toMat(), kernel);

		// RESCALE 8BIT
		faceDetectionImg = DefacingUtil.transformToByte(faceDetectionImg).toImageCV();

		// CANNY DETECT CONTOUR
		Imgproc.Canny(faceDetectionImg.toImageCV(), faceDetectionImg.toMat(), 240, 260);

		// DRAW BLACK RECT 1/3
		int rectProportion = (int) (faceDetectionImg.height() / 2.9);
		Rect rect = new Rect(0, rectProportion, faceDetectionImg.width(), faceDetectionImg.height());
		Imgproc.rectangle(faceDetectionImg.toImageCV(), rect, new Scalar(255), Imgproc.FILLED);

		for (int x = 0; x < faceDetectionImg.width(); x++) {
			for (int y = 0; y < faceDetectionImg.height(); y++) {
				if (MathUtil.isEqual(faceDetectionImg.toMat().get(y, x)[0], 255)) {
					Imgproc.line(faceDetectionImg.toImageCV(), new Point(x, y + 1.0),
							new Point(x, faceDetectionImg.height()), new Scalar(0));
					break;
				}
			}
		}

		return faceDetectionImg;
	}

	public static PlanarImage addRandPxlLine(PlanarImage srcImg, PlanarImage faceDetectImg, Attributes attributes) {
		ImageCV randPxlLineImg = new ImageCV();
		srcImg.toMat().copyTo(randPxlLineImg);
		double pixelSpacing = attributes.getDouble(Tag.PixelSpacing, 0.5);
		int minThicknessSkin = (int) (1 / pixelSpacing); // 1mm
		int maxThicknessSkin = (int) (3 / pixelSpacing); // 3mm
		// DRAW A LINE WITH RANDOM VALUE WHEN FACE DETECTED
		int yOffsetRand = 1;
		// scan the image from left to right and bottom to top until the face is detected
		// in Y
		for (int x = 0; x < faceDetectImg.width(); x++) {
			boolean faceDetected = false;
			int yFaceDetected = 0;
			int thicknessSkin = DefacingUtil.randomY(minThicknessSkin, maxThicknessSkin, 1);
			int margeY = maxThicknessSkin * 2;

			for (int y = faceDetectImg.height() - 1; y > 0; y--) {
				double faceDetectPixelValue = faceDetectImg.toMat().get(y, x)[0];
				if (MathUtil.isEqual(faceDetectPixelValue, 255.0)) {
					faceDetected = true;
					yFaceDetected = y;
				}

				if (faceDetected) {
					// Put random color after the face detection
					int minY = yFaceDetected + yOffsetRand;
					int maxY = yFaceDetected + yOffsetRand + margeY;
					double randomPixelColor = DefacingUtil.pickRndYPxlColor(x, minY, maxY, srcImg);
					randPxlLineImg.toMat().put(y + thicknessSkin, x, randomPixelColor);
				}
				else {
					randPxlLineImg.toMat().put(y, x, 0.0);
				}
			}
		}
		return randPxlLineImg;
	}

	public static PlanarImage blurImg(PlanarImage srcImg, PlanarImage faceDetectImg) {
		ImageCV bluredImgRandPxlLine = new ImageCV();
		srcImg.toMat().copyTo(bluredImgRandPxlLine);
		Imgproc.blur(bluredImgRandPxlLine.toImageCV(), bluredImgRandPxlLine.toMat(), new Size(5, 5));

		int marginBlurSkin = 20;

		for (int x = 0; x < faceDetectImg.width(); x++) {
			boolean faceDetected = true;
			int yNoBlurImg = 0;
			for (int y = 0; y < faceDetectImg.height(); y++) {
				if (MathUtil.isDifferentFromZero(faceDetectImg.toMat().get(y, x)[0])) {
					faceDetected = false;
					yNoBlurImg = y + marginBlurSkin;
				}

				if (!faceDetected) {
					bluredImgRandPxlLine.toMat().put(yNoBlurImg, x, srcImg.toMat().get(yNoBlurImg, x)[0]);
				}
			}
		}

		return bluredImgRandPxlLine;
	}

	public static PlanarImage mergeImg(PlanarImage srcImg, PlanarImage randPxlLineImg, PlanarImage faceDetectImg) {
		// MERGE IMAGE SRC AND RAND PIXEL LINE
		ImageCV newImg = new ImageCV();
		srcImg.toMat().copyTo(newImg);

		for (int x = 0; x < faceDetectImg.width(); x++) {
			for (int y = faceDetectImg.height() - 1; y > 0; y--) {
				if (MathUtil.isDifferentFromZero(randPxlLineImg.toMat().get(y, x)[0])) {
					newImg.toMat().put(y, x, randPxlLineImg.toMat().get(y, x)[0]);
				}
				else {
					newImg.toMat().put(y, x, srcImg.toMat().get(y, x)[0]);
				}
			}
		}
		return newImg;
	}

}
