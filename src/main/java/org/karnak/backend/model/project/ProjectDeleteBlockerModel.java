/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.project;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A destination that still references a project and prevents its deletion. Both project
 * associations of the destination are reported, so the caller can see the other project
 * involved when the destination uses a different project on the other side.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProjectDeleteBlockerModel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private UUID destinationUuid;

	private UUID forwardNodeUuid;

	private UUID deIdentificationProjectUuid;

	private UUID tagMorphingProjectUuid;

}
