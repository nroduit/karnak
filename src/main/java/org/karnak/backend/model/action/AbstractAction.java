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

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.VR;
import org.dcm4che3.util.TagUtils;
import org.jspecify.annotations.NullUnmarked;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.weasis.core.util.StringUtil;

@Slf4j
@NullUnmarked
public abstract class AbstractAction implements ActionItem {

	private static final Marker CLINICAL_MARKER = MarkerFactory.getMarker("CLINICAL");

	private static final String PATTERN_WITH_INOUT = "SOPInstanceUID_OLD={} TAG={} ACTION={} OLD={} NEW={}";

	private static final String PATTERN_WITH_IN = "SOPInstanceUID_OLD={} TAG={} ACTION={} OLD={}";

	@Getter
	protected final String symbol;

	@Setter
	@Getter
	protected String dummyValue;

	protected int newTag;

	@Setter
	@Getter
	protected VR vr;

	protected AbstractAction(String symbol) {
		this.symbol = symbol;
		this.dummyValue = null;
		this.vr = null;
	}

	protected AbstractAction(String symbol, String dummyValue) {
		this.symbol = symbol;
		this.dummyValue = dummyValue;
		this.vr = null;
	}

	protected AbstractAction(String symbol, VR vr, String dummyValue) {
		this.symbol = symbol;
		this.vr = vr;
		this.dummyValue = dummyValue;
	}

	protected AbstractAction(String symbol, int newTag, VR vr, String dummyValue) {
		this.symbol = symbol;
		this.newTag = newTag;
		this.vr = vr;
		this.dummyValue = dummyValue;
	}

	public static AbstractAction convertAction(String action) {
		if (action == null) {
			return null;
		}
		return switch (action) {
			case "Z" -> new ReplaceNull("Z");
			case "X" -> new Remove("X");
			case "K" -> new Keep("K");
			case "U" -> new UID("U");
			case "DDum" -> new DefaultDummy("DDum");
			case "D" -> new Replace("D");
			default -> null;
		};
	}

	public static String getStringValue(Attributes dcm, int tag) {
		if (dcm != null) {
			VR vr = dcm.getVR(tag);
			if (vr.isInlineBinary()) {
				return "Binary Data";
			}
			else if (vr == VR.SQ) {
				return "Sequence Data";
			}
			else {
				return dcm.getString(tag, StringUtil.EMPTY_STRING);
			}
		}
		return StringUtil.EMPTY_STRING;
	}

	/**
	 * Trace-logs the action with the current tag value (read only when tracing is
	 * enabled).
	 */
	protected void traceIn(Attributes dcm, int tag) {
		if (log.isTraceEnabled()) {
			log.trace(CLINICAL_MARKER, PATTERN_WITH_IN, MDC.get("SOPInstanceUID"), TagUtils.toString(tag), symbol,
					getStringValue(dcm, tag));
		}
	}

	/**
	 * Trace-logs the action with the current tag value as old value and the given new
	 * value.
	 */
	protected void traceInOut(Attributes dcm, int tag, String newValue) {
		if (log.isTraceEnabled()) {
			log.trace(CLINICAL_MARKER, PATTERN_WITH_INOUT, MDC.get("SOPInstanceUID"), TagUtils.toString(tag), symbol,
					getStringValue(dcm, tag), newValue);
		}
	}

	/** Trace-logs the action with explicit old and new values. */
	protected void traceInOut(int tag, String oldValue, String newValue) {
		if (log.isTraceEnabled()) {
			log.trace(CLINICAL_MARKER, PATTERN_WITH_INOUT, MDC.get("SOPInstanceUID"), TagUtils.toString(tag), symbol,
					oldValue, newValue);
		}
	}

}
