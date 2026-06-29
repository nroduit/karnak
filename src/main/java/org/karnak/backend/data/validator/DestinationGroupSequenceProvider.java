/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.data.validator;

import java.util.List;
import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.backend.enums.DestinationType;

/**
 * @see <a href=
 * "https://stackoverflow.com/questions/27173960/jsr303-apply-all-validation-groups-defined-in-sequence">Group
 * validation</a>
 */
public class DestinationGroupSequenceProvider implements DefaultGroupSequenceProvider<DestinationEntity> {

	private static final List<Class<?>> DEFAULT_GROUPS = List.of(DestinationEntity.class);

	private static final List<Class<?>> TYPE_DICOM_GROUPS = List.of(DestinationEntity.class,
			DestinationDicomGroup.class);

	private static final List<Class<?>> TYPE_STOW_GROUPS = List.of(DestinationEntity.class, DestinationStowGroup.class);

	@Override
	public List<Class<?>> getValidationGroups(Class<?> beanType, DestinationEntity destinationEntity) {
		DestinationType type = destinationEntity != null ? destinationEntity.getDestinationType() : null;
		if (type == null) {
			return DEFAULT_GROUPS;
		}
		// A virtual (report-only) destination never forwards anything, so the delivery
		// fields (AETitle/host/port or URL) are irrelevant: skip the type-specific
		// mandatory-field groups and keep only the default constraints.
		if (destinationEntity.isVirtualDestination()) {
			return DEFAULT_GROUPS;
		}
		return switch (type) {
			case dicom -> TYPE_DICOM_GROUPS;
			case stow -> TYPE_STOW_GROUPS;
		};
	}

	public interface DestinationDicomGroup {

	}

	public interface DestinationStowGroup {

	}

}
