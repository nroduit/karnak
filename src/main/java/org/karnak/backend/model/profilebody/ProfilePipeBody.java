/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profilebody;

import java.util.List;

public class ProfilePipeBody {

	private String name;

	private String version;

	private String minimumKarnakVersion;

	private String defaultIssuerOfPatientID;

	private List<ProfileElementBody> profiles;

	private List<MaskBody> masks;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getMinimumKarnakVersion() {
		return minimumKarnakVersion;
	}

	public void setMinimumKarnakVersion(String minimumKarnakVersion) {
		this.minimumKarnakVersion = minimumKarnakVersion;
	}

	public List<ProfileElementBody> getProfileElements() {
		return profiles;
	}

	public void setProfileElements(List<ProfileElementBody> profiles) {
		this.profiles = profiles;
	}

	public String getDefaultIssuerOfPatientID() {
		return defaultIssuerOfPatientID;
	}

	public void setDefaultIssuerOfPatientID(String defaultIssuerOfPatientID) {
		this.defaultIssuerOfPatientID = defaultIssuerOfPatientID;
	}

	public List<MaskBody> getMasks() {
		return masks;
	}

	public void setMasks(List<MaskBody> masks) {
		this.masks = masks;
	}

}
