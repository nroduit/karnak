/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.weasis.core.util.FileUtil;
import org.weasis.core.util.annotations.Generated;
import org.weasis.opencv.natives.NativeLibrary;

@Slf4j
@Generated
public class NativeLibraryManager {

	/** Marker folder that must appear in {@code java.library.path}. */
	static final String OPENCV_FOLDER = "dicom-opencv";

	private static final String LIBRARY_PATH_PROPERTY = "java.library.path";

	private static final String NATIVE_CODEC_PROPERTY = "dicom.native.codec";

	private NativeLibraryManager() {
	}

	/** Extracts (if needed) and loads the native OpenCV library. */
	public static void initNativeLibs() {
		String system = NativeLibrary.getNativeLibSpecification();
		Path libraryFile = prepareLibrary(system, System.getProperty(LIBRARY_PATH_PROPERTY, ""));
		NativeLibrary.loadLibraryFromAbsolutePath(libraryFile);
	}

	/**
	 * Resolves the OpenCV library file, copying it from the classpath when available.
	 *
	 * <p>
	 * When running from the build output (IntelliJ, Maven) the library is on the
	 * classpath under {@code /lib/<system>/<filename>} and is copied next to
	 * {@code java.library.path}. In the portable package it has already been extracted
	 * into the {@code dicom-opencv} folder (and may have been stripped from the jar), so
	 * whatever is there is used as-is.
	 * @param system the native library specification (e.g. {@code linux-x86-64})
	 * @param libraryPath the {@code java.library.path} value to search
	 * @return the absolute path of the library to load
	 */
	static Path prepareLibrary(String system, String libraryPath) {
		Path directory = resolveLibraryDirectory(libraryPath);
		Path outputFile = directory.resolve(libraryFileName(system));
		System.setProperty(NATIVE_CODEC_PROPERTY, directory.toString());

		String resourcePath = "/lib/" + system + "/" + outputFile.getFileName();
		try (InputStream in = NativeLibraryManager.class.getResourceAsStream(resourcePath)) {
			if (in != null) {
				Files.createDirectories(directory);
				FileUtil.writeStream(in, outputFile, true);
			}
			else if (!Files.isReadable(outputFile)) {
				throw new IllegalStateException("Native OpenCV library is neither on the classpath (" + resourcePath
						+ ") nor already present at " + outputFile);
			}
		}
		catch (IOException e) {
			throw new IllegalStateException("Cannot copy the native OpenCV library to " + outputFile, e);
		}
		return outputFile;
	}

	/** Finds the {@code dicom-opencv} folder declared in {@code java.library.path}. */
	static Path resolveLibraryDirectory(String libraryPath) {
		return Arrays.stream(libraryPath.split(File.pathSeparator))
			.filter(segment -> segment.contains(OPENCV_FOLDER))
			.findFirst()
			.map(Path::of)
			.orElseThrow(() -> new IllegalStateException("OpenCV library folder (a path containing \"" + OPENCV_FOLDER
					+ "\") is not configured in " + LIBRARY_PATH_PROPERTY));
	}

	/** Maps a native library specification to the platform-specific file name. */
	static String libraryFileName(String system) {
		if (system.startsWith("win")) {
			return "opencv_java.dll";
		}
		if (system.startsWith("mac")) {
			return "libopencv_java.dylib";
		}
		return "libopencv_java.so";
	}

}
