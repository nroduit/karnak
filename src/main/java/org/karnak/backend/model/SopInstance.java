/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model;

import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

@Getter
@NullUnmarked
public class SopInstance {

	@NonNull private final String sopInstanceUID;

	@Setter
	private Integer instanceNumber;

	@Setter
	private String sopClassUID;

	@Setter
	private boolean sent;

	public SopInstance(String sopInstanceUID) {
		this.sopInstanceUID = Objects.requireNonNull(sopInstanceUID, "sopInstanceIUID is null");
	}

	public static void addSopInstance(Map<String, SopInstance> sopInstanceMap, SopInstance s) {
		if (s != null && sopInstanceMap != null) {
			sopInstanceMap.put(s.getSopInstanceUID(), s);
		}
	}

	public static @Nullable SopInstance removeSopInstance(Map<String, SopInstance> sopInstanceMap, String sopUID) {
		if (sopUID == null || sopInstanceMap == null) {
			return null;
		}
		return sopInstanceMap.remove(sopUID);
	}

	public static @Nullable SopInstance getSopInstance(Map<String, SopInstance> sopInstanceMap, String sopUID) {
		if (sopUID == null || sopInstanceMap == null) {
			return null;
		}
		return sopInstanceMap.get(sopUID);
	}

}
