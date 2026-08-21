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
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.exception.AbortException;
import org.karnak.backend.exception.EndpointException;
import org.karnak.backend.service.ApplicationContextProvider;
import org.karnak.backend.service.EndpointService;
import org.springframework.web.client.HttpClientErrorException;
import org.weasis.dicom.param.AttributeEditorContext;

/**
 * {@code shift_from_api} option of the {@code action.on.dates} profile item: shifts a
 * date/time value by the amounts returned by an external endpoint, typically queried per
 * patient.
 */
@Slf4j
@NullUnmarked
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
					throw new IllegalArgumentException(error);
				}
			}
			else if ("daysPath".equals(key)) {
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
					throw new IllegalArgumentException(error);
				}
			}
		}

		if (!urlProvided) {
			throw new IllegalArgumentException("Cannot build the option ShiftApiDate: url argument is mandatory");
		}
		if (!daysPathProvided) {
			throw new IllegalArgumentException("Cannot build the option ShiftApiDate: daysPath argument is mandatory");
		}
		if (isPost && !bodyProvided) {
			throw new IllegalArgumentException(
					"Cannot build the option ShiftApiDate: body argument is mandatory for a POST request");
		}
	}

	/**
	 * Shifts the value of {@code tag} by the amounts returned by the configured endpoint.
	 *
	 * <p>
	 * The two datasets play distinct roles and are <b>not</b> interchangeable:
	 * <ul>
	 * <li>{@code dcm} is the dataset currently being de-identified, at the nesting level
	 * of {@code tag}: during sequence recursion in
	 * {@link org.karnak.backend.service.profilepipe.Profile#applyAction} it is the
	 * sequence <i>item</i>. It is the only dataset holding the value to shift and its
	 * VR.</li>
	 * <li>{@code original} is the untouched copy of that same dataset, taken before the
	 * pipeline started, against which the {@code {{...}}} placeholders of the {@code url}
	 * and {@code body} arguments are resolved — from its own level outwards, so the
	 * patient identifier of the enclosing study stays visible for a date nested in a
	 * sequence. Resolving them on {@code dcm} would instead send already pseudonymized
	 * identifiers to the endpoint, since the patient identifier typically sorts before
	 * the shifted date tag, defeating the per-patient lookup.</li>
	 * </ul>
	 * @param dcm dataset being de-identified, at the nesting level of {@code tag}
	 * @param original untouched copy of {@code dcm}, used to resolve the placeholders of
	 * the request
	 * @param tag tag whose value must be shifted
	 * @param argumentEntities arguments of the profile item
	 * @return the shifted value, or {@code null} when the tag holds no value or its VR is
	 * not a date/time
	 * @throws DateTimeException if the value of {@code tag} is not a valid date/time
	 * @throws org.karnak.backend.exception.EndpointException if the endpoint cannot be
	 * called or its response cannot be parsed
	 * @throws AbortException if the response holds no usable shift value
	 */
	public static @Nullable String shift(Attributes dcm, Attributes original, int tag,
			List<ArgumentEntity> argumentEntities) throws DateTimeException {
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
				case "daysPath" -> daysPath = normalizeJsonPath(ae.getArgumentValue());
				case "secondsPath" -> secondsPath = normalizeJsonPath(ae.getArgumentValue());
				case "method" -> method = ae.getArgumentValue();
				case "body" -> body = ae.getArgumentValue();
				case "authConfig" -> authConfig = ae.getArgumentValue();
				default -> {
				}
			}
		}

		url = evaluateStringWithExpression(url, original);
		if (body != null) {
			body = evaluateStringWithExpression(body, original);
		}

		String response = fetchResponse(authConfig, url, method, body);
		int shiftDays = parseShiftValue(response, daysPath, "daysPath");
		int shiftSeconds = 0;
		if (secondsPath != null) {
			shiftSeconds = parseShiftValue(response, secondsPath, "secondsPath");
		}

		String dcmElValue = dcm.getString(tag);
		return ShiftDate.shiftValue(dcm, tag, dcmElValue, shiftDays, shiftSeconds);
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
