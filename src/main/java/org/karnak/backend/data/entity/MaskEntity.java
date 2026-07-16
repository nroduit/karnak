/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.awt.Rectangle;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;

import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.converter.RectangleListConverter;
import org.karnak.backend.data.converter.RectangleListToStringListConverter;

@Entity(name = "Masks")
@Table(name = "masks")
@NullUnmarked
@Getter
@Setter
public class MaskEntity implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonIgnore
	private Long id;

	@ManyToOne
	@JoinColumn(name = "profile_id", nullable = false)
	@JsonIgnore
	private ProfileEntity profileEntity;

	private String stationName;

	private Long imageWidth;

	private Long imageHeight;

	private String color;

	@Convert(converter = RectangleListConverter.class)
	private List<Rectangle> rectangles = new ArrayList<>();

	public MaskEntity() {
	}

	public MaskEntity(String stationName, Long imageWidth, Long imageHeight, String color,
			ProfileEntity profileEntity) {
		this.stationName = stationName;
		this.color = color;
		this.profileEntity = profileEntity;
		this.imageHeight = imageHeight;
		this.imageWidth = imageWidth;
	}

	public MaskEntity(String stationName, String color, ProfileEntity profileEntity) {
		this(stationName, null, null, color, profileEntity);
	}

	public void addRectangle(String rectangle) {
		Rectangle rect = RectangleListConverter.stringToRectangle(rectangle);
		if (rect != null) {
			rectangles.add(rect);
		}
	}

	public void addRectangle(Rectangle rect) {
		rectangles.add(rect);
	}

	@JsonSerialize(converter = RectangleListToStringListConverter.class)
	public List<Rectangle> getRectangles() {
		return rectangles;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MaskEntity that = (MaskEntity) o;
		return Objects.equals(id, that.id) && Objects.equals(stationName, that.stationName)
				&& Objects.equals(imageWidth, that.imageWidth) && Objects.equals(imageHeight, that.imageHeight)
				&& Objects.equals(color, that.color) && Objects.equals(rectangles, that.rectangles);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, stationName, imageWidth, imageHeight, color, rectangles);
	}

}
