/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.karnak.KarnakApplication;

/**
 * Builds the links to the online user documentation for the running Karnak version.
 *
 * <p>
 * The documentation site serves several Karnak releases from the same pages and selects
 * one from a {@code v} query parameter, so the running version is appended to every link.
 * The parameter is inserted before any fragment, because topics may carry one (for
 * instance {@code userguide/projects/index.html#5-project-secret}).
 */
@NullMarked
public final class DocumentationLinks {

	public static final String BASE_URL = "https://karnak.weasis.org/";

	/** Version taken from the jar manifest; absent when running from the IDE. */
	private static final @Nullable String VERSION = KarnakApplication.class.getPackage().getImplementationVersion();

	private DocumentationLinks() {
	}

	/**
	 * Returns the URL of a documentation topic for the running version.
	 * @param topic the topic relative to {@link #BASE_URL}, or {@code null} for the
	 * documentation root
	 * @return the URL to open in a browser
	 */
	public static String url(@Nullable String topic) {
		return withVersion(BASE_URL + (topic == null ? "" : topic), VERSION);
	}

	static String withVersion(String url, @Nullable String version) {
		if (version == null || version.isBlank()) {
			return url;
		}
		int fragment = url.indexOf('#');
		String path = fragment < 0 ? url : url.substring(0, fragment);
		String anchor = fragment < 0 ? "" : url.substring(fragment);
		return path + (path.indexOf('?') < 0 ? '?' : '&') + "v=" + URLEncoder.encode(version, StandardCharsets.UTF_8)
				+ anchor;
	}

}
