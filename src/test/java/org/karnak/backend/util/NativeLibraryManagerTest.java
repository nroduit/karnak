/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayNameGeneration(ReplaceUnderscores.class)
class NativeLibraryManagerTest {

	// matches src/test/resources/lib/karnak-test-os/libopencv_java.so
	private static final String SYSTEM_WITH_RESOURCE = "karnak-test-os";

	private static final String SYSTEM_WITHOUT_RESOURCE = "karnak-no-such-os";

	@AfterEach
	void clearNativeCodecProperty() {
		System.clearProperty("dicom.native.codec");
	}

	@Test
	void library_file_name_maps_windows_to_a_dll() {
		assertEquals("opencv_java.dll", NativeLibraryManager.libraryFileName("windows-x86-64"));
	}

	@Test
	void library_file_name_maps_mac_to_a_dylib() {
		assertEquals("libopencv_java.dylib", NativeLibraryManager.libraryFileName("macosx-x86-64"));
	}

	@Test
	void library_file_name_maps_other_systems_to_a_so() {
		assertEquals("libopencv_java.so", NativeLibraryManager.libraryFileName("linux-x86-64"));
	}

	@Test
	void resolve_library_directory_returns_the_dicom_opencv_segment() {
		String libraryPath = String.join(File.pathSeparator, "/usr/lib", "/opt/karnak/dicom-opencv", "/lib");

		assertEquals(Path.of("/opt/karnak/dicom-opencv"), NativeLibraryManager.resolveLibraryDirectory(libraryPath));
	}

	@Test
	void resolve_library_directory_throws_when_no_segment_matches() {
		assertThrows(IllegalStateException.class, () -> NativeLibraryManager.resolveLibraryDirectory("/usr/lib"));
	}

	@Test
	void prepare_library_copies_the_library_from_the_classpath(@TempDir Path tempDir) throws IOException {
		Path directory = tempDir.resolve(NativeLibraryManager.OPENCV_FOLDER);

		Path libraryFile = NativeLibraryManager.prepareLibrary(SYSTEM_WITH_RESOURCE, directory.toString());

		assertEquals(directory.resolve("libopencv_java.so"), libraryFile);
		assertTrue(Files.isReadable(libraryFile));
		assertEquals("dummy-native-lib-content", Files.readString(libraryFile));
		assertEquals(directory.toString(), System.getProperty("dicom.native.codec"));
	}

	@Test
	void prepare_library_does_not_mark_a_preexisting_directory_for_deletion(@TempDir Path tempDir) throws IOException {
		// The portable package installs the library inside the app image, which must
		// survive
		// the shutdown cleanup in GatewayService#destroy.
		Path directory = Files.createDirectories(tempDir.resolve(NativeLibraryManager.OPENCV_FOLDER));

		Path libraryFile = NativeLibraryManager.prepareLibrary(SYSTEM_WITH_RESOURCE, directory.toString());

		assertTrue(Files.isReadable(libraryFile));
		assertNull(System.getProperty("dicom.native.codec"));
	}

	@Test
	void prepare_library_does_not_overwrite_an_installed_library(@TempDir Path tempDir) throws IOException {
		// The portable package ships the library inside the signed app image, so it must
		// not be
		// rewritten from the classpath even when the jar still carries a copy.
		Path directory = Files.createDirectories(tempDir.resolve(NativeLibraryManager.OPENCV_FOLDER));
		Files.writeString(directory.resolve("libopencv_java.so"), "installed-by-the-packager");

		Path libraryFile = NativeLibraryManager.prepareLibrary(SYSTEM_WITH_RESOURCE, directory.toString());

		assertEquals("installed-by-the-packager", Files.readString(libraryFile));
		assertNull(System.getProperty("dicom.native.codec"));
	}

	@Test
	void prepare_library_keeps_an_already_extracted_library(@TempDir Path tempDir) throws IOException {
		Path directory = Files.createDirectories(tempDir.resolve(NativeLibraryManager.OPENCV_FOLDER));
		Path existing = Files.writeString(directory.resolve("libopencv_java.so"), "already-here");

		Path libraryFile = NativeLibraryManager.prepareLibrary(SYSTEM_WITHOUT_RESOURCE, directory.toString());

		assertEquals(existing, libraryFile);
		assertEquals("already-here", Files.readString(libraryFile));
		assertNull(System.getProperty("dicom.native.codec"));
	}

	@Test
	void prepare_library_throws_when_library_is_missing_everywhere(@TempDir Path tempDir) throws IOException {
		Path directory = Files.createDirectories(tempDir.resolve(NativeLibraryManager.OPENCV_FOLDER));

		assertThrows(IllegalStateException.class,
				() -> NativeLibraryManager.prepareLibrary(SYSTEM_WITHOUT_RESOURCE, directory.toString()));
		assertFalse(Files.exists(directory.resolve("libopencv_java.so")));
	}

}