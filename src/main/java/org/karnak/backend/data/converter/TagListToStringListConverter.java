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

import com.fasterxml.jackson.databind.util.StdConverter;
import java.util.ArrayList;
import java.util.List;
import org.karnak.backend.data.entity.TagEntity;

// https://www.logicbig.com/tutorials/misc/jackson/json-serialize-deserialize-converter.html
public class TagListToStringListConverter extends StdConverter<List<TagEntity>, List<String>> {

	@Override
	public List<String> convert(List<TagEntity> tagEntities) {
		List strArray = new ArrayList();
		tagEntities.forEach(tagEntity -> {
			strArray.add(tagEntity.getTagValue());
		});
		return strArray;
	}

}
