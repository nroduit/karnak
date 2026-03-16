/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.action;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.VR;
import org.karnak.backend.model.profilepipe.HMAC;

public interface ActionItem {

	String getSymbol();

	String getDummyValue();

	void setDummyValue(String dummyValue);

	VR getVr();

	void setVr(VR vr);

	void execute(Attributes dcm, int tag, HMAC hmac);

}
