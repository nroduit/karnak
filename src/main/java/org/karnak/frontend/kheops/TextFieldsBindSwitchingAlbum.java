/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.kheops;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import java.util.List;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.karnak.backend.api.KheopsApi;
import org.karnak.backend.data.entity.KheopsAlbumsEntity;
import org.karnak.backend.model.expression.ExprCondition;
import org.karnak.backend.model.expression.ExpressionError;
import org.karnak.backend.model.expression.ExpressionResult;
import org.karnak.backend.service.kheops.SwitchingAlbum;

public class TextFieldsBindSwitchingAlbum {

	private final KheopsApi kheopsApi;

	@Getter
	private final Binder<KheopsAlbumsEntity> binder;

	@Getter
	private final TextField textUrlAPI;

	@Getter
	private final TextField textAuthorizationDestination;

	@Getter
	private final TextField textAuthorizationSource;

	@Getter
	private final TextField textCondition;

	private ExpressionError expressionError;

	@Getter
	private final Span textErrorConditionMsg;

	public TextFieldsBindSwitchingAlbum() {
		kheopsApi = new KheopsApi();
		textUrlAPI = new TextField();
		textAuthorizationDestination = new TextField();
		textAuthorizationSource = new TextField();
		textCondition = new TextField();
		textErrorConditionMsg = new Span();
		expressionError = new ExpressionError(true, "");
		binder = buildBinder();
	}

	private Binder<KheopsAlbumsEntity> buildBinder() {
		Binder<KheopsAlbumsEntity> b = new BeanValidationBinder<>(KheopsAlbumsEntity.class);
		b.forField(textAuthorizationDestination)
			.withValidator(StringUtils::isNotBlank, "Token destination is mandatory")
			.withValidator(value -> {
				if (!textUrlAPI.getValue().isBlank()) {
					return validateToken(value, textUrlAPI.getValue(), SwitchingAlbum.MIN_SCOPE_DESTINATION);
				}
				return true;
			}, "Token can't be validate, minimum permissions: [write]")
			.bind(KheopsAlbumsEntity::getAuthorizationDestination, KheopsAlbumsEntity::setAuthorizationDestination);
		b.forField(textAuthorizationSource)
			.withValidator(StringUtils::isNotBlank, "Token source is mandatory")
			.withValidator(value -> {
				if (!textUrlAPI.getValue().isBlank()) {
					return validateToken(value, textUrlAPI.getValue(), SwitchingAlbum.MIN_SCOPE_SOURCE);
				}
				return true;
			}, "Token can't be validate, minimum permissions: [read, send]")
			.bind(KheopsAlbumsEntity::getAuthorizationSource, KheopsAlbumsEntity::setAuthorizationSource);
		b.forField(textUrlAPI)
			.withValidator(StringUtils::isNotBlank, "Url API is mandatory")
			.bind(KheopsAlbumsEntity::getUrlAPI, KheopsAlbumsEntity::setUrlAPI);
		b.forField(textCondition).withValidator(value -> {
			if (!textCondition.getValue().isEmpty()) {
				expressionError = ExpressionResult.isValid(textCondition.getValue(), new ExprCondition(),
						Boolean.class);
				textErrorConditionMsg.setText(expressionError.getMsg());
				return expressionError.isValid();
			}
			textErrorConditionMsg.setText("");
			return true;
		}, "Condition is not valid").bind(KheopsAlbumsEntity::getCondition, KheopsAlbumsEntity::setCondition);
		return b;
	}

	private boolean validateToken(String token, String urlAPI, List<String> validMinScope) {
		try {
			JSONObject responseIntrospect = kheopsApi.tokenIntrospect(urlAPI, token, token);
			return SwitchingAlbum.validateIntrospectedToken(responseIntrospect, validMinScope);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		catch (Exception e) {
			// Do nothing
		}
		return false;
	}

}
