/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.service.profilepipe;

import java.util.Objects;

public class MaskStationCondition {

    private String stationName;

    private Long imageWidth;

    private Long imageHeight;

    public MaskStationCondition(String stationName, Long imageWidth, Long imageHeight) {
        this.stationName = stationName;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    public MaskStationCondition(String stationName, String imageWidth, String imageHeight) {
        this(stationName, imageWidth != null ? Long.valueOf(imageWidth) : null, imageHeight != null ? Long.valueOf(imageHeight) : null);
    }

    public MaskStationCondition(String stationName) {
        this(stationName, (Long) null, null);
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public Long getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(Long imageWidth) {
        this.imageWidth = imageWidth;
    }

    public Long getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(Long imageHeight) {
        this.imageHeight = imageHeight;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MaskStationCondition that = (MaskStationCondition) o;
        return Objects.equals(stationName, that.stationName) && Objects.equals(imageWidth, that.imageWidth) && Objects.equals(imageHeight, that.imageHeight);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stationName, imageWidth, imageHeight);
    }
}
