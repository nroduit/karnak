/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.profilepipe;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

public class MaskStationCondition {

	private @Nullable String stationName;

	private @Nullable Long imageWidth;

	private @Nullable Long imageHeight;

	public MaskStationCondition(@Nullable String stationName, @Nullable Long imageWidth, @Nullable Long imageHeight) {
		this.stationName = stationName;
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
	}

	public MaskStationCondition(@Nullable String stationName, @Nullable String imageWidth,
			@Nullable String imageHeight) {
		this(stationName, imageWidth != null ? Long.valueOf(imageWidth) : null,
				imageHeight != null ? Long.valueOf(imageHeight) : null);
	}

	public MaskStationCondition(@Nullable String stationName) {
		this(stationName, (Long) null, null);
	}

	public @Nullable String getStationName() {
		return stationName;
	}

	public void setStationName(@Nullable String stationName) {
		this.stationName = stationName;
	}

	public @Nullable Long getImageWidth() {
		return imageWidth;
	}

	public void setImageWidth(@Nullable Long imageWidth) {
		this.imageWidth = imageWidth;
	}

	public @Nullable Long getImageHeight() {
		return imageHeight;
	}

	public void setImageHeight(@Nullable Long imageHeight) {
		this.imageHeight = imageHeight;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MaskStationCondition that = (MaskStationCondition) o;
		return Objects.equals(stationName, that.stationName) && Objects.equals(imageWidth, that.imageWidth)
				&& Objects.equals(imageHeight, that.imageHeight);
	}

	@Override
	public int hashCode() {
		return Objects.hash(stationName, imageWidth, imageHeight);
	}

}
