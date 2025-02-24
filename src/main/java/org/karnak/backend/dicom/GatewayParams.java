/*
 * Copyright (c) 2009-2019 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.dicom;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.weasis.dicom.param.AbstractListenerParams;
import org.weasis.dicom.param.AdvancedParams;
import org.weasis.dicom.param.DicomNode;

public class GatewayParams extends AbstractListenerParams {

	public GatewayParams(boolean bindCallingAet) {
		super(null, bindCallingAet, null);
	}

	public GatewayParams(AdvancedParams params, boolean bindCallingAet) {
		super(params, bindCallingAet, null);
	}

	public GatewayParams(AdvancedParams params, boolean bindCallingAet, URL transferCapabilityFile,
			String... acceptedCallingAETitles) {
		super(params, bindCallingAet, transferCapabilityFile, acceptedCallingAETitles);
	}

	public static String[] getAcceptedCallingAETitles(Map<ForwardDicomNode, List<ForwardDestination>> destinations) {
		Set<ForwardDicomNode> set = destinations.keySet();
		if (set.stream().anyMatch(s -> s.getAcceptedSourceNodes().isEmpty())) {
			return null;
		}
		return set.stream()
			.map(ForwardDicomNode::getAcceptedSourceNodes)
			.flatMap(Set<DicomNode>::stream)
			.map(DicomNode::getAet)
			.distinct()
			.toArray(String[]::new);
	}

}
