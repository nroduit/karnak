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

import lombok.Getter;
import lombok.Setter;
import org.dcm4che3.img.util.Editable;
import org.jspecify.annotations.NullUnmarked;
import org.weasis.opencv.data.PlanarImage;

@Setter
@Getter
@NullUnmarked
public class TransformedPlanarImage {

	private Editable<PlanarImage> editablePlanarImage;

	private PlanarImage planarImage;

}
