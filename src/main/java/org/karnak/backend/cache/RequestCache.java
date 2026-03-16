/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.cache;

import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.karnak.backend.util.SecurityUtil;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

public class RequestCache extends HttpSessionRequestCache {

	/**
	 * Saves unauthenticated requests so we can redirect the user to the page they were
	 * trying to access once they’re logged in
	 * @param request Request
	 * @param response Response
	 */
	@Override
	public void saveRequest(HttpServletRequest request, HttpServletResponse response) {
		if (!SecurityUtil.isFrameworkInternalRequest(request)) {
			super.saveRequest(request, response);
		}
	}

	/**
	 * Determine the path to navigate to: Retrieve if existing the saved request or
	 * redirect to the root path
	 * @return the path resolved
	 */
	public String resolveRedirectUrl() {
		SavedRequest savedRequest = getRequest(VaadinServletRequest.getCurrent().getHttpServletRequest(),
				VaadinServletResponse.getCurrent().getHttpServletResponse());
		if (savedRequest instanceof DefaultSavedRequest) {
			final String requestURI = ((DefaultSavedRequest) savedRequest).getRequestURI();
			// check for valid URI
			if (requestURI != null && !requestURI.isEmpty()) {
				return requestURI.startsWith("/") ? requestURI.substring(1) : requestURI;
			}
		}

		// if everything fails, redirect to the main view
		return "";
	}

}
