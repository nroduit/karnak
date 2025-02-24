/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.validator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.enums.DestinationType;

/**
 * @see https://stackoverflow.com/questions/27173960/jsr303-apply-all-validation-groups-defined-in-sequence
 */
public class DestinationGroupSequenceProvider implements DefaultGroupSequenceProvider<DestinationEntity> {

	@Override
	public List<Class<?>> getValidationGroups(DestinationEntity destinationEntity) {
		if (destinationEntity != null) {
			DestinationType type = destinationEntity.getDestinationType();
			if (type != null) {
				switch (type) {
					case dicom:
						return TYPE_DICOM_GROUPS;
					case stow:
						return TYPE_STOW_GROUPS;
				}
			}
		}

		return DEFAULT_GROUPS;
	}

	public interface DestinationDicomGroup {

	}

	private static final List<Class<?>> DEFAULT_GROUPS = //
			Collections.singletonList(DestinationEntity.class);

	private static final List<Class<?>> TYPE_DICOM_GROUPS = //
			Arrays.asList(DestinationEntity.class, DestinationDicomGroup.class);

	private static final List<Class<?>> TYPE_STOW_GROUPS = //
			Arrays.asList(DestinationEntity.class, DestinationStowGroup.class);

	public interface DestinationStowGroup {

	}

}
