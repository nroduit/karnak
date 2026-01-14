/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.monitoring.component;

import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import java.util.EnumSet;

/**
 * Class used to handle the mapping strategy of the monitoring export CSV
 *
 * @param <T>
 */
public class MonitoringCsvMappingStrategy<T> extends ColumnPositionMappingStrategy<T> {

	/**
	 * Constructor
	 */
	public MonitoringCsvMappingStrategy() {
		setColumnMapping(EnumSet.allOf(MonitoringCsvMapping.class)
			.stream()
			.map(MonitoringCsvMapping::getNameFieldEntity)
			.toArray(String[]::new));
	}

	@Override
	public String[] generateHeader(T bean) throws CsvRequiredFieldEmptyException {
		setType((Class<? extends T>) bean.getClass());
		super.generateHeader(bean);
		return EnumSet.allOf(MonitoringCsvMapping.class)
			.stream()
			.map(MonitoringCsvMapping::getLabelCsv)
			.toArray(String[]::new);
	}

}
