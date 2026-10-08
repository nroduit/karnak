/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.weasis.core.util.annotations.Generated;

@Service
@Slf4j
@Generated()
public class KheopsApi {

	private final HttpClient httpClient;

	private static final String X_AUTHORIZATION_SOURCE = "X-Authorization-Source";

	public KheopsApi() {
		httpClient = HttpClient.newBuilder()
			.version(HttpClient.Version.HTTP_1_1) // use HTTP 1.1 because an issue with
													// HTTP 2 in Kheops (not image sent
													// after 1000)
			.build();
	}

	// https://github.com/OsiriX-Foundation/KheopsAuthorization/wiki/Add-a-series
	// /studies/{StudyInstanceIUD}/series/{SeriesInstanceIUD}/albums/{album_id}
	// album_id: album id destination
	// x-authorization-source: main album token
	// Authorization: album destination token
	public int shareSerie(String studyInstanceUID, String seriesInstanceUID, String apiUrl, String authorizationSource,
			String authorizationDestination) throws IOException, InterruptedException {
		final String stringURI = String.format("%s/studies/%s/series/%s", apiUrl, studyInstanceUID, seriesInstanceUID);
		final URI uri = URI.create(stringURI);
		HttpRequest request = HttpRequest.newBuilder()
			.PUT(HttpRequest.BodyPublishers.noBody())
			.uri(uri)
			.setHeader(HttpHeaders.ACCEPT, "application/json")
			.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
			.setHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", authorizationDestination))
			.setHeader(X_AUTHORIZATION_SOURCE, String.format("Bearer %s", authorizationSource))
			.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		return response.statusCode();
	}

	public JSONObject tokenIntrospect(String apiUrl, String authorizationToken, String introspectToken)
			throws IOException, InterruptedException {
		final String stringURI = String.format("%s/token/introspect", apiUrl);
		final URI uri = URI.create(stringURI);

		Map<Object, Object> data = new HashMap<>();
		data.put("token", introspectToken);
		HttpRequest request = HttpRequest.newBuilder()
			.POST(Utils.buildDataFromMap(data))
			.uri(uri)
			.setHeader(HttpHeaders.ACCEPT, "application/json")
			.setHeader(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
			.setHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", authorizationToken))
			.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		int status = response.statusCode();
		if (status >= 200 && status <= 300) {
			try {
				return new JSONObject(response.body());
			}
			catch (Exception e) {
				log.error("Cannot parse the token introspection response", e);
			}
		}
		return new JSONObject();
	}

}
