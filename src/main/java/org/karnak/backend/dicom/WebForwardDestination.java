/*
 * Copyright (c) 2018-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.dicom;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.weasis.core.util.StreamUtil;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.param.DicomProgress;
import org.weasis.dicom.param.DicomState;
import org.weasis.dicom.web.ContentType;
import org.weasis.dicom.web.DicomStowRS;

public class WebForwardDestination extends ForwardDestination {

	private final ForwardDicomNode callingNode;

	@Getter
	private final DicomState state;

	private final DicomStowRS stowRS;

	public WebForwardDestination(ForwardDicomNode fwdNode, String requestURL) {
		this(fwdNode, requestURL, null);
	}

	public WebForwardDestination(ForwardDicomNode fwdNode, String requestURL, @Nullable List<AttributeEditor> editors) {
		this(fwdNode, requestURL, null, editors);
	}

	public WebForwardDestination(ForwardDicomNode fwdNode, String requestURL, @Nullable DicomProgress progress,
			@Nullable List<AttributeEditor> editors) {
		this(fwdNode, requestURL, null, progress, editors);
	}

	public WebForwardDestination(ForwardDicomNode fwdNode, String requestURL, @Nullable Map<String, String> headers,
			@Nullable DicomProgress progress, @Nullable List<AttributeEditor> editors) {
		this(null, fwdNode, requestURL, headers, progress, editors, null, true, HttpClient.Version.HTTP_1_1);
	}

	public WebForwardDestination(@Nullable Long id, ForwardDicomNode fwdNode, String requestURL,
			@Nullable Map<String, String> headers, @Nullable DicomProgress progress,
			@Nullable List<AttributeEditor> editors, @Nullable String outputTransferSyntax,
			boolean transcodeOnlyUncompressed, HttpClient.@Nullable Version httpVersion) {
		super(id, editors);
		this.callingNode = fwdNode;
		this.state = new DicomState(progress == null ? new DicomProgress() : progress);
		// HTTP version is configurable per destination and defaults to HTTP/1.1: over
		// HTTP/2 a reverse proxy in front of the archive (e.g. KHEOPS / nginx with the
		// default
		// http2_max_requests=1000) sends GOAWAY after 1000 requests on the connection,
		// and the JDK HttpClient does not retry the non-idempotent STOW POSTs, so
		// instances
		// beyond the 1000th are silently dropped. See KheopsApi for the same default.
		this.stowRS = new DicomStowRS(requestURL, ContentType.APPLICATION_DICOM, null, headers,
				httpVersion == null ? HttpClient.Version.HTTP_1_1 : httpVersion);
		setOutputTransferSyntax(outputTransferSyntax);
		setTranscodeOnlyUncompressed(transcodeOnlyUncompressed);
	}

	public WebForwardDestination(ForwardDicomNode fwdNode, DicomStowRS uploadManager, @Nullable DicomProgress progress,
			@Nullable List<AttributeEditor> editors) {
		this(null, fwdNode, uploadManager, progress, editors);
	}

	public WebForwardDestination(@Nullable Long id, ForwardDicomNode fwdNode, DicomStowRS uploadManager,
			@Nullable DicomProgress progress, @Nullable List<AttributeEditor> editors) {
		super(id, editors);
		this.callingNode = fwdNode;
		this.state = new DicomState(progress == null ? new DicomProgress() : progress);
		this.stowRS = uploadManager;
	}

	@Override
	public ForwardDicomNode getForwardDicomNode() {
		return callingNode;
	}

	public String getRequestURL() {
		return stowRS.getRequestURL();
	}

	public DicomStowRS getStowrsSingleFile() {
		return stowRS;
	}

	@Override
	public void stop() {
		StreamUtil.safeClose(stowRS);
	}

	@Override
	public String toString() {
		return stowRS.getRequestURL();
	}

}
