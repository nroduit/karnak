/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.action;

import java.util.List;
import org.dcm4che3.data.Attributes;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.util.ShiftRangeDate;

public class DefaultDummy extends AbstractAction {

	static final List<ArgumentEntity> argumentEntities = List.of(new ArgumentEntity("max_days", "365"),
			new ArgumentEntity("max_seconds", "86400"));

	public DefaultDummy(String symbol) {
		super(symbol);
	}

	public DefaultDummy(String symbol, String dummyValue) {
		super(symbol, dummyValue);
	}

	@Override
	public void execute(Attributes dcm, int tag, HMAC hmac) {
		String defaultDummyValue = switch (dcm.getVR(tag)) {
			case AE, CS, LO, LT, PN, SH, ST, UN, UT, UC, UR -> "UNKNOWN";
			case DS, IS -> "0";
			case AS, DA, DT, TM -> ShiftRangeDate.shift(dcm, tag, argumentEntities, hmac);
			case UI -> hmac.uidHash(dcm.getString(tag));
			default -> null;
		};
		ActionItem replace = new Replace(symbol, defaultDummyValue);
		replace.execute(dcm, tag, hmac);
	}

}
