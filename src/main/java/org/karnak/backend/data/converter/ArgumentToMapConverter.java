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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.karnak.backend.data.entity.ArgumentEntity;

public class ArgumentToMapConverter extends StdConverter<List<ArgumentEntity>, Map<String, String>> {

	@Override
	public Map<String, String> convert(List<ArgumentEntity> argumentEntities) {
		Map<String, String> argumentMap = new HashMap<>();
		argumentEntities.forEach(argument -> {
			argumentMap.put(argument.getArgumentKey(), argument.getArgumentValue());
		});
		return argumentMap;
	}

}
