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
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.profilepipe.HMAC;

public interface ProfileItem {

	ActionItem getAction(Attributes dcm, Attributes dcmCopy, int tag, HMAC hmac);

	ActionItem put(int tag, ActionItem action);

	ActionItem remove(int tag);

	void clearTagMap();

	String getName();

	String getCodeName();

	String getCondition();

	String getOption();

	List<ArgumentEntity> getArguments();

	Integer getPosition();

	void profileValidation() throws ProfileException;

}
