/*
 * Copyright (c) 2021 Karnak Team and other contributors.
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
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.ExcludedTagEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.exception.AbortException;
import org.karnak.backend.exception.EndpointException;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.model.action.ActionItem;
import org.karnak.backend.model.action.Replace;
import org.karnak.backend.model.expression.ExprCondition;
import org.karnak.backend.model.expression.ExpressionError;
import org.karnak.backend.model.expression.ExpressionResult;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.profilepipe.TagActionMap;
import org.karnak.backend.service.ApplicationContextProvider;
import org.karnak.backend.service.EndpointService;
import org.springframework.web.client.HttpClientErrorException;
import org.weasis.dicom.param.AttributeEditorContext;

@Slf4j
public class ReplaceApi extends AbstractProfileItem {

	private final TagActionMap tagsAction;

	private final ActionItem actionByDefault;

	private final TagActionMap exceptedTagsAction;

	private EndpointService endpointService;

	public ReplaceApi(ProfileElementEntity profileElementEntity) throws ProfileException {
		super(profileElementEntity);
		tagsAction = new TagActionMap();
		exceptedTagsAction = new TagActionMap();
		actionByDefault = new Replace("R");
		profileValidation();
		setActionHashMap();
	}

	private void setActionHashMap() {

		if (tagEntities != null && !tagEntities.isEmpty()) {
			for (IncludedTagEntity tag : tagEntities) {
				tagsAction.put(tag.getTagValue(), actionByDefault);
			}
		}
		if (excludedTagEntities != null) {
			for (ExcludedTagEntity tag : excludedTagEntities) {
				exceptedTagsAction.put(tag.getTagValue(), actionByDefault);
			}
		}
	}

	@Override
	public ActionItem getAction(Attributes dcm, Attributes dcmCopy, int tag, HMAC hmac) {
		if (!tagsAction.isEmpty() && tagsAction.get(tag) == null) {
			return null;
		}

		String value = null;
		if (exceptedTagsAction.get(tag) == null) {
			String url = null;
			String responsePath = null;
			String method = "get"; // defaults to get if not specified
			String body = null;
			String authConfig = null;
			String defaultValue = null;

			for (ArgumentEntity ae : argumentEntities) {
				if ("url".equals(ae.getArgumentKey())) {
					url = evaluateStringWithExpression(ae.getArgumentValue(), dcm);
				}
				else if ("responsePath".equals(ae.getArgumentKey())) {
					responsePath = ae.getArgumentValue();
					if (!responsePath.startsWith("/")) {
						responsePath = "/" + responsePath;
					}
				}
				else if ("method".equals(ae.getArgumentKey())) {
					method = ae.getArgumentValue();
				}
				else if ("body".equals(ae.getArgumentKey())) {
					body = evaluateStringWithExpression(ae.getArgumentValue(), dcm);
				}
				else if ("authConfig".equals(ae.getArgumentKey())) {
					authConfig = ae.getArgumentValue();
				}
				else if ("defaultValue".equals(ae.getArgumentKey())) {
					defaultValue = ae.getArgumentValue();
				}
			}

			String response = null;
			if (endpointService == null) {
				endpointService = ApplicationContextProvider.bean(EndpointService.class);
			}

			try {
				if (method.equalsIgnoreCase("post")) {
					response = endpointService.post(authConfig, url, body);
				}
				else if (method.equalsIgnoreCase("get")) {
					response = endpointService.get(authConfig, url);
				}
				else {
					throw new EndpointException("Unsupported HTTP Method : " + method);
				}

				ObjectMapper objectMapper = new ObjectMapper();
				value = objectMapper.readTree(response).at(responsePath).textValue();
				// Node that matches given JSON Pointer: if no match exists, will return a
				// node for which TreeNode.isMissingNode() returns true.
			}
			catch (JsonProcessingException e) {
				if (defaultValue == null) {
					throw new EndpointException("An error occurred while parsing the JSON response ", e);
				}
			}
			catch (IllegalArgumentException e) {
				if (defaultValue == null) {
					// Abort current transfer, authConfig not defined
					throw new EndpointException(e.getMessage());
				}
			}
			catch (HttpClientErrorException e) {
				if (defaultValue == null) {
					// Abort current transfer
					throw new EndpointException("HTTP Client Error : " + e.getStatusText() + " - " + url);
				}
			}

			ActionItem replace = new Replace("D");

			if (value != null && !value.isEmpty()) {
				replace.setDummyValue(value);
			}
			else if (defaultValue != null) {
				replace.setDummyValue(defaultValue);
			}
			else {
				throw new AbortException(AttributeEditorContext.Abort.CONNECTION_EXCEPTION,
						"Transfer aborted, replace value not found in response - " + responsePath);
			}
			return replace;
		}
		return null;
	}

	@Override
	public void profileValidation() throws ProfileException {
		String errorMessage = "Cannot build the profile ";

		if (tagEntities == null || tagEntities.isEmpty()) {
			throw new ProfileException(errorMessage + codeName + ": No tags defined");
		}

		if (argumentEntities == null || argumentEntities.size() < 2) {
			throw new ProfileException(
					"Cannot build the profile " + codeName + ": Need to specify url and responsePath argument");
		}
		else {

			boolean urlProvided = false;
			boolean responsePathProvided = false;
			boolean isPost = false;
			boolean bodyProvided = false;
			for (ArgumentEntity ae : argumentEntities) {
				if (ae.getArgumentKey().equals("url")) {
					urlProvided = true;
					String error = validateStringWithExpression(ae.getArgumentValue());
					if (error != null) {
						throw new ProfileException(String.format("Expression is not valid: \n\r%s", error));
					}
				}
				else if (ae.getArgumentKey().equals("responsePath")) {
					responsePathProvided = true;
				}
				else if (ae.getArgumentKey().equals("method")) {
					if (!ae.getArgumentValue().equalsIgnoreCase("post")
							&& !ae.getArgumentValue().equalsIgnoreCase("get")) {
						throw new ProfileException(
								"Cannot build the profile " + codeName + ": method must be get or post");
					}
					else if (ae.getArgumentValue().equalsIgnoreCase("post")) {
						isPost = true;
					}
				}
				else if (ae.getArgumentKey().equals("body")) {
					bodyProvided = true;
					String error = validateStringWithExpression(ae.getArgumentValue());
					if (error != null) {
						throw new ProfileException(String.format("Expression is not valid: \n\r%s", error));
					}
				}
			}
			if (!urlProvided) {
				throw new ProfileException("Cannot build the profile " + codeName + ": url argument is mandatory");
			}
			if (!responsePathProvided) {
				throw new ProfileException(
						"Cannot build the profile " + codeName + ": responsePath argument is mandatory");
			}
			if (isPost && !bodyProvided) {
				throw new ProfileException(
						"Cannot build the profile " + codeName + ": body argument is mandatory for a POST request");
			}
		}

		final ExpressionError expressionError = ExpressionResult.isValid(condition, new ExprCondition(new Attributes()),
				Boolean.class);
		if (condition != null && !expressionError.isValid()) {
			throw new ProfileException(expressionError.getMsg());
		}
	}

}
