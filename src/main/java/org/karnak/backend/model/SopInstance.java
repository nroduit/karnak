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

public class SopInstance {

	private final String sopInstanceUID;

	private Integer instanceNumber;

	private String sopClassUID;

	private boolean sent;

	public SopInstance(String sopInstanceUID) {
		this.sopInstanceUID = Objects.requireNonNull(sopInstanceUID, "sopInstanceIUID is null");
	}

	public static void addSopInstance(Map<String, SopInstance> sopInstanceMap, SopInstance s) {
		if (s != null && sopInstanceMap != null) {
			sopInstanceMap.put(s.getSopInstanceUID(), s);
		}
	}

	public static SopInstance removeSopInstance(Map<String, SopInstance> sopInstanceMap, String sopUID) {
		if (sopUID == null || sopInstanceMap == null) {
			return null;
		}
		return sopInstanceMap.remove(sopUID);
	}

	public static SopInstance getSopInstance(Map<String, SopInstance> sopInstanceMap, String sopUID) {
		if (sopUID == null || sopInstanceMap == null) {
			return null;
		}
		return sopInstanceMap.get(sopUID);
	}

	public String getSopInstanceUID() {
		return sopInstanceUID;
	}

	public Integer getInstanceNumber() {
		return instanceNumber;
	}

	public void setInstanceNumber(Integer instanceNumber) {
		this.instanceNumber = instanceNumber;
	}

	public String getSopClassUID() {
		return sopClassUID;
	}

	public void setSopClassUID(String sopClassUID) {
		this.sopClassUID = sopClassUID;
	}

	public boolean isSent() {
		return sent;
	}

	public void setSent(boolean sent) {
		this.sent = sent;
	}

}
