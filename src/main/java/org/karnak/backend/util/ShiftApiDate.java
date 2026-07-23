/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.util;

import static org.karnak.backend.service.EndpointService.evaluateStringWithExpression;
import static org.karnak.backend.service.EndpointService.validateStringWithExpression;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DateTimeException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.exception.AbortException;
import org.karnak.backend.exception.EndpointException;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.service.ApplicationContextProvider;
import org.karnak.backend.service.EndpointService;
import org.springframework.web.client.HttpClientErrorException;
import org.weasis.dicom.param.AttributeEditorContext;

@Slf4j
public class ShiftApiDate {

	private static EndpointService endpointService;

	private ShiftApiDate() {
	}

	public static void verifyShiftArguments(List<ArgumentEntity> argumentEntities) throws IllegalArgumentException {
		if (argumentEntities == null || argumentEntities.isEmpty()) {
			throw new IllegalArgumentException(
					"Cannot build the option ShiftApiDate: Missing argument, url and days_path are required");
		}

		boolean urlProvided = false;
		boolean daysPathProvided = false;
		boolean isPost = false;
		boolean bodyProvided = false;

		for (ArgumentEntity ae : argumentEntities) {
			final String key = ae.getArgumentKey();
			if ("url".equals(key)) {
				urlProvided = true;
				String error = validateStringWithExpression(ae.getArgumentValue());
				if (error != null) {
					throw new IllegalArgumentException(String.format("Expression is not valid: \n\r%s", error));
				}
			}
			else if ("days_path".equals(key)) {
				daysPathProvided = true;
			}
			else if ("method".equals(key)) {
				if (!ae.getArgumentValue().equalsIgnoreCase("post") && !ae.getArgumentValue().equalsIgnoreCase("get")) {
					throw new IllegalArgumentException(
							"Cannot build the option ShiftApiDate: method must be get or post");
				}
				if (ae.getArgumentValue().equalsIgnoreCase("post")) {
					isPost = true;
				}
			}
			else if ("body".equals(key)) {
				bodyProvided = true;
				String error = validateStringWithExpression(ae.getArgumentValue());
				if (error != null) {
					throw new IllegalArgumentException(String.format("Expression is not valid: \n\r%s", error));
				}
			}
		}

		if (!urlProvided) {
			throw new IllegalArgumentException("Cannot build the option ShiftApiDate: url argument is mandatory");
		}
		if (!daysPathProvided) {
			throw new IllegalArgumentException("Cannot build the option ShiftApiDate: days_path argument is mandatory");
		}
		if (isPost && !bodyProvided) {
			throw new IllegalArgumentException(
					"Cannot build the option ShiftApiDate: body argument is mandatory for a POST request");
		}
	}

	public static String shift(Attributes dcmCopy, int tag, List<ArgumentEntity> argumentEntities, HMAC hmac)
			throws DateTimeException {
		verifyShiftArguments(argumentEntities);

		String url = null;
		String daysPath = null;
		String secondsPath = null;
		String method = "get";
		String body = null;
		String authConfig = null;

		for (ArgumentEntity ae : argumentEntities) {
			switch (ae.getArgumentKey()) {
				case "url" -> url = ae.getArgumentValue();
				case "days_path" -> daysPath = normalizeJsonPath(ae.getArgumentValue());
				case "seconds_path" -> secondsPath = normalizeJsonPath(ae.getArgumentValue());
				case "method" -> method = ae.getArgumentValue();
				case "body" -> body = ae.getArgumentValue();
				case "authConfig" -> authConfig = ae.getArgumentValue();
				default -> {
				}
			}
		}

		url = evaluateStringWithExpression(url, dcmCopy);
		if (body != null) {
			body = evaluateStringWithExpression(body, dcmCopy);
		}

		String response = fetchResponse(authConfig, url, method, body);
		int shiftDays = parseShiftValue(response, daysPath, "days_path");
		int shiftSeconds = 0;
		if (secondsPath != null) {
			shiftSeconds = parseShiftValue(response, secondsPath, "seconds_path");
		}

		String dcmElValue = dcmCopy.getString(tag);
		return ShiftDate.shiftValue(dcmCopy, tag, dcmElValue, shiftDays, shiftSeconds);
	}

	private static String normalizeJsonPath(String path) {
		if (path != null && !path.startsWith("/")) {
			return "/" + path;
		}
		return path;
	}

	private static String fetchResponse(String authConfig, String url, String method, String body) {
		if (endpointService == null) {
			endpointService = ApplicationContextProvider.bean(EndpointService.class);
		}

		try {
			if (method.equalsIgnoreCase("post")) {
				return endpointService.post(authConfig, url, body);
			}
			if (method.equalsIgnoreCase("get")) {
				return endpointService.get(authConfig, url);
			}
			throw new EndpointException("Unsupported HTTP Method : " + method);
		}
		catch (IllegalArgumentException e) {
			throw new EndpointException(e.getMessage());
		}
		catch (HttpClientErrorException e) {
			throw new EndpointException("HTTP Client Error : " + e.getStatusText() + " - " + url);
		}
	}

	private static int parseShiftValue(String response, String jsonPath, String argumentName) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode node = objectMapper.readTree(response).at(jsonPath);
			if (node == null || node.isMissingNode() || node.isNull()) {
				throw new AbortException(AttributeEditorContext.Abort.CONNECTION_EXCEPTION,
						"Transfer aborted, shift value not found in response - " + argumentName + " (" + jsonPath
								+ ")");
			}
			String textValue = node.isNumber() ? String.valueOf(node.intValue()) : node.asText();
			if (textValue == null || textValue.isEmpty()) {
				throw new AbortException(AttributeEditorContext.Abort.CONNECTION_EXCEPTION,
						"Transfer aborted, shift value not found in response - " + argumentName + " (" + jsonPath
								+ ")");
			}
			return Integer.parseInt(textValue);
		}
		catch (JsonProcessingException e) {
			throw new EndpointException("An error occurred while parsing the JSON response ", e);
		}
		catch (NumberFormatException e) {
			throw new AbortException(AttributeEditorContext.Abort.CONNECTION_EXCEPTION,
					"Transfer aborted, shift value is not a valid integer - " + argumentName + " (" + jsonPath + ")");
		}
	}

}
