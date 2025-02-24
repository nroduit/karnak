/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profiles;

import java.util.List;
import org.dcm4che3.data.Attributes;
import org.karnak.backend.config.AppConfig;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.TagActionMap;
import org.karnak.backend.model.standard.ConfidentialityProfiles;

public class BasicProfile extends AbstractProfileItem {

	private final List<ProfileItem> listProfiles;

	private final TagActionMap actionMap;

	public BasicProfile(ProfileElementEntity profileElementEntity) {
		super(profileElementEntity);
		ConfidentialityProfiles confidentialityProfiles = AppConfig.getInstance().getConfidentialityProfile();
		actionMap = confidentialityProfiles.getActionMap();
		listProfiles = confidentialityProfiles.getListProfiles();
	}

	@Override
	public ActionItem getAction(Attributes dcm, Attributes dcmCopy, int tag, HMAC hmac) {
		ActionItem action = actionMap.get(tag);
		if (action == null) {
			for (ProfileItem p : listProfiles) {
				ActionItem val = p.getAction(dcm, dcmCopy, tag, hmac);
				if (val != null) {
					return val;
				}
			}
			return null;
		}
		return action;
	}

}
