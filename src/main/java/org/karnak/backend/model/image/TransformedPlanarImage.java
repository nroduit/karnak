/*
 * Copyright (c) 2024-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.image;

import org.dcm4che3.img.util.Editable;
import org.weasis.opencv.data.PlanarImage;

public class TransformedPlanarImage {

	Editable<PlanarImage> editablePlanarImage;

	PlanarImage planarImage;

	public Editable<PlanarImage> getEditablePlanarImage() {
		return editablePlanarImage;
	}

	public void setEditablePlanarImage(Editable<PlanarImage> editablePlanarImage) {
		this.editablePlanarImage = editablePlanarImage;
	}

	public PlanarImage getPlanarImage() {
		return planarImage;
	}

	public void setPlanarImage(PlanarImage planarImage) {
		this.planarImage = planarImage;
	}

}
