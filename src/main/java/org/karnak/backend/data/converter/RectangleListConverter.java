/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class RectangleListConverter implements AttributeConverter<List<Rectangle>, String> {

	private static final String SPLIT_CHAR = ";";

	public static String rectangleToString(Rectangle rect) {
		return String.join(" ", String.valueOf(rect.x), String.valueOf(rect.y), String.valueOf(rect.width),
				String.valueOf(rect.height));
	}

	public static Rectangle stringToRectangle(String rectangle) {
		String[] vals = rectangle.trim().split("\\s+");
		if (vals.length == 4) {
			return new Rectangle(Integer.parseInt(vals[0]), Integer.parseInt(vals[1]), Integer.parseInt(vals[2]),
					Integer.parseInt(vals[3]));
		}
		return null;
	}

	@Override
	public String convertToDatabaseColumn(List<Rectangle> stringList) {
		return stringList.stream()
			.map(RectangleListConverter::rectangleToString)
			.collect(Collectors.joining(SPLIT_CHAR));
	}

	@Override
	public List<Rectangle> convertToEntityAttribute(String string) {
		return Arrays.asList(string.split(SPLIT_CHAR))
			.stream()
			.map(RectangleListConverter::stringToRectangle)
			.collect(Collectors.toList());
	}

}
