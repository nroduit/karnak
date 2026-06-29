/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.dicominnolitics;

import lombok.Getter;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.action.DefaultDummy;
import org.karnak.backend.model.action.Keep;
import org.karnak.backend.model.action.MultipleActions;
import org.karnak.backend.model.action.Remove;
import org.karnak.backend.model.action.Replace;
import org.karnak.backend.model.action.ReplaceNull;
import org.karnak.backend.model.action.UID;

@NullUnmarked
public class JsonConfidentialityProfiles {

	@Getter
	private String id;

	@Getter
	private String name;

	@Getter
	private String tag;

	private String basicProfile;

	private String stdCompIOD;

	private String cleanDescOpt;

	public ActionItem getBasicProfile() {
		return convertAction(basicProfile);
	}

	public ActionItem getStdCompIOD() {
		return convertAction(stdCompIOD);
	}

	public ActionItem getCleanDescOpt() {
		return convertAction(cleanDescOpt);
	}

	private static ActionItem convertAction(String strAction) {
		return switch (strAction) {
			case "D" -> new DefaultDummy("DDum");
			case "Z" -> new ReplaceNull("Z");
			case "X" -> new Remove("X");
			case "K" -> new Keep("K");
			case "U" -> new UID("U");
			case "Z/D", "X/D", "X/Z/D", "X/Z", "X/Z/U", "X/Z/U*" -> new MultipleActions(strAction);
			default -> new Replace("D");
		};
	}

}
