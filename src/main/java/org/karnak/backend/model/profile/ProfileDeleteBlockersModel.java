/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profile;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Projects that block deleting a profile until the caller assigns them another profile or
 * deletes them explicitly.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProfileDeleteBlockersModel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private UUID profileUuid;

	private List<ProfileDeleteBlockerModel> blockers = new ArrayList<>();

	public boolean hasBlockers() {
		return blockers != null && !blockers.isEmpty();
	}

}
