/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicominnolitics;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Reads a bundled DICOM-standard JSON resource into the matching generated POJO array.
 */
final class JsonStandardReader {

	private static final Gson GSON = new Gson();

	private JsonStandardReader() {
	}

	static <T> T read(String fileName, Class<T> type) {
		try (InputStream in = JsonStandardReader.class.getResourceAsStream(fileName);
				Reader reader = new InputStreamReader(Objects.requireNonNull(in, fileName), StandardCharsets.UTF_8)) {
			return GSON.fromJson(reader, type);
		}
		catch (Exception e) {
			throw new JsonParseException("Cannot parse json %s correctly".formatted(fileName), e);
		}
	}

}