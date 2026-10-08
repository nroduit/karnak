/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.enums;

import lombok.Getter;

@Getter
public enum Modality {

	ALL("No filter"),

	AU("Audio"),

	BI("Biomagnetic imaging"),

	CD("Color flow Doppler"),

	DD("Duplex Doppler"),

	DG("Diaphanography"),

	CR("Computed Radiography"),

	CT("Computed Tomography"),

	DX("Digital Radiography"),

	ECG("Electrocardiography"),

	EPS("Cardiac Electrophysiology"),

	ES("Endoscopy"),

	GM("General Microscopy"),

	HC("Hard Copy"),

	HD("Hemodynamic Waveform"),

	IO("Intra-oral Radiography"),

	IVUS("Intravascular Ultrasound"),

	LS("Laser surface scan"),

	MG("Mammography"),

	MR("Magnetic Resonance"),

	NM("Nuclear Medicine"),

	OT("Other"),

	OP("Ophthalmic Photography"),

	PR("Presentation State"),

	PX("Panoramic X-Ray"),

	PT("Positron emission tomography (PET)"),

	RF("Radio Fluoroscopy"),

	RG("Radiographic imaging (conventional film/screen)"),

	RTDOSE("Radiotherapy Dose"),

	RTIMAGE("Radiotherapy Image"),

	RTPLAN("Radiotherapy Plan"),

	RTRECORD("RT Treatment Record"),

	RTSTRUCT("Radiotherapy Structure Set"),

	SC("Secondary Capture"),

	SM("Slide Microscopy"),

	SMR("Stereometric Relationship"),

	SR("SR Document"),

	ST("Single-photon emission computed tomography (SPECT)"),

	TG("Thermography"),

	US("Ultrasound"),

	XA("X-Ray Angiography"),

	XC("External-camera Photography");

	private final String description;

	Modality(String description) {
		this.description = description;
	}

	public static Modality getModality(String modality) {
		if (modality != null) {
			try {
				return Modality.valueOf(modality);
			}
			catch (IllegalArgumentException e) {
				// Unsupported modality: fall back to ALL
			}
		}
		return ALL;
	}

	@Override
	public String toString() {
		return name() + " (" + description + ")";
	}

}
