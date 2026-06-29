/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profiles;

import static org.karnak.backend.service.EndpointService.evaluateStringWithExpression;
import static org.karnak.backend.service.EndpointService.validateStringWithExpression;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.exception.AbortException;
import org.karnak.backend.exception.EndpointException;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.action.Replace;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.TagActionMap;
import org.karnak.backend.service.ApplicationContextProvider;
import org.karnak.backend.service.EndpointService;
import org.springframework.web.client.HttpClientErrorException;
import org.weasis.dicom.param.AttributeEditorContext;

@Slf4j
public class ReplaceApi extends AbstractProfileItem {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final TagActionMap tagsAction;

	private final TagActionMap exceptedTagsAction;

	private @Nullable EndpointService endpointService;

	/**
	 * Endpoint call configuration extracted from the profile {@code argumentEntities}.
	 */
	private record ApiArguments(@Nullable String url, @Nullable String responsePath, String method,
			@Nullable String body, @Nullable String authConfig, @Nullable String defaultValue) {
	}

	public ReplaceApi(ProfileElementEntity profileElementEntity) throws ProfileException {
		super(profileElementEntity);
		tagsAction = new TagActionMap();
		exceptedTagsAction = new TagActionMap();
		ActionItem actionByDefault = new Replace("R");
		profileValidation();
		mapTagsToAction(tagsAction, exceptedTagsAction, actionByDefault);
	}

	@Override
	public @Nullable ActionItem getAction(Attributes dcm, Attributes dcmCopy, int tag, HMAC hmac) {
		if (!tagsAction.isEmpty() && tagsAction.get(tag) == null) {
			return null;
		}
		if (exceptedTagsAction.get(tag) != null) {
			return null;
		}

		ApiArguments args = parseArguments(dcm);
		String value = fetchValue(args);

		ActionItem replace = new Replace("D");
		if (value != null && !value.isEmpty()) {
			replace.setDummyValue(value);
		}
		else if (args.defaultValue() != null) {
			replace.setDummyValue(args.defaultValue());
		}
		else {
			throw new AbortException(AttributeEditorContext.Abort.CONNECTION_EXCEPTION,
					"Transfer aborted, replace value not found in response - " + args.responsePath());
		}
		return replace;
	}

	private ApiArguments parseArguments(Attributes dcm) {
		String url = null;
		String responsePath = null;
		String method = "get"; // defaults to get if not specified
		String body = null;
		String authConfig = null;
		String defaultValue = null;
		for (ArgumentEntity ae : argumentEntities) {
			switch (ae.getArgumentKey()) {
				case "url" -> url = evaluateStringWithExpression(ae.getArgumentValue(), dcm);
				case "responsePath" -> {
					responsePath = ae.getArgumentValue();
					if (!responsePath.startsWith("/")) {
						responsePath = "/" + responsePath;
					}
				}
				case "method" -> method = ae.getArgumentValue();
				case "body" -> body = evaluateStringWithExpression(ae.getArgumentValue(), dcm);
				case "authConfig" -> authConfig = ae.getArgumentValue();
				case "defaultValue" -> defaultValue = ae.getArgumentValue();
				default -> {
				}
			}
		}
		return new ApiArguments(url, responsePath, method, body, authConfig, defaultValue);
	}

	// Returns the resolved value, or null to fall back to the configured default value.
	private @Nullable String fetchValue(ApiArguments args) {
		if (endpointService == null) {
			endpointService = ApplicationContextProvider.bean(EndpointService.class);
		}
		try {
			String response = switch (args.method().toLowerCase()) {
				case "post" -> endpointService.post(args.authConfig(), args.url(), args.body());
				case "get" -> endpointService.get(args.authConfig(), args.url());
				default -> throw new EndpointException("Unsupported HTTP Method : " + args.method());
			};
			return OBJECT_MAPPER.readTree(response).at(args.responsePath()).textValue();
		}
		catch (JsonProcessingException e) {
			if (args.defaultValue() == null) {
				throw new EndpointException("An error occurred while parsing the JSON response ", e);
			}
		}
		catch (IllegalArgumentException e) {
			if (args.defaultValue() == null) {
				// Abort current transfer, authConfig not defined
				throw new EndpointException(e.getMessage());
			}
		}
		catch (HttpClientErrorException e) {
			if (args.defaultValue() == null) {
				// Abort current transfer
				throw new EndpointException("HTTP Client Error : " + e.getStatusText() + " - " + args.url());
			}
		}
		return null;
	}

	@Override
	public void profileValidation() throws ProfileException {
		String errorMessage = "Cannot build the profile " + codeName + ": ";

		if (tagEntities == null || tagEntities.isEmpty()) {
			throw new ProfileException(errorMessage + "No tags defined");
		}
		if (argumentEntities == null || argumentEntities.size() < 2) {
			throw new ProfileException(errorMessage + "Need to specify url and responsePath argument");
		}

		boolean urlProvided = false;
		boolean responsePathProvided = false;
		boolean isPost = false;
		boolean bodyProvided = false;
		for (ArgumentEntity ae : argumentEntities) {
			switch (ae.getArgumentKey()) {
				case "url" -> {
					urlProvided = true;
					validateExpression(ae.getArgumentValue());
				}
				case "responsePath" -> responsePathProvided = true;
				case "method" -> {
					String method = ae.getArgumentValue();
					if (!method.equalsIgnoreCase("post") && !method.equalsIgnoreCase("get")) {
						throw new ProfileException(errorMessage + "method must be get or post");
					}
					isPost = method.equalsIgnoreCase("post");
				}
				case "body" -> {
					bodyProvided = true;
					validateExpression(ae.getArgumentValue());
				}
				default -> {
				}
			}
		}
		if (!urlProvided) {
			throw new ProfileException(errorMessage + "url argument is mandatory");
		}
		if (!responsePathProvided) {
			throw new ProfileException(errorMessage + "responsePath argument is mandatory");
		}
		if (isPost && !bodyProvided) {
			throw new ProfileException(errorMessage + "body argument is mandatory for a POST request");
		}

		validateCondition();
	}

	private static void validateExpression(String expression) throws ProfileException {
		String error = validateStringWithExpression(expression);
		if (error != null) {
			throw new ProfileException(String.format("Expression is not valid: \n\r%s", error));
		}
	}

}
