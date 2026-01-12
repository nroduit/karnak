/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.project.component;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.binder.Validator;
import java.util.Objects;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.karnak.backend.data.entity.ProjectEntity;
import org.karnak.backend.data.entity.SecretEntity;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.frontend.component.ProfileDropDown;

@Getter
public class TextFieldsBindProject {

	private final Binder<ProjectEntity> binder;

	private final TextField textResearchName;

	private final ComboBox<SecretEntity> secretComboBox;

	private final ProfileDropDown profileDropDown;

	public TextFieldsBindProject() {
		this.textResearchName = new TextField();
		this.secretComboBox = new ComboBox<>("Project Secret");
		this.profileDropDown = new ProfileDropDown();
		this.binder = setBinder();
	}

	private Binder<ProjectEntity> setBinder() {
		Binder<ProjectEntity> binderBean = new BeanValidationBinder<>(ProjectEntity.class);
		binderBean.forField(textResearchName)
			.withValidator(StringUtils::isNotBlank, "Name is mandatory")
			.bind(ProjectEntity::getName, ProjectEntity::setName);

		binderBean.forField(secretComboBox)
			.withValidator(secretMandatoryValidator())
			.withValidator(secretValidValidator())
			.bind(ProjectEntity::retrieveActiveSecret, ProjectEntity::applyActiveSecret);

		binderBean.forField(profileDropDown)
			.withValidator(Objects::nonNull, "Choose the de-identification profile\n")
			.bind(ProjectEntity::getProfileEntity, ProjectEntity::setProfileEntity);
		return binderBean;
	}

	/**
	 * Validate secretEntity key
	 * @return validation ok if key is valid, validation error otherwise
	 */
	private Validator<SecretEntity> secretValidValidator() {
		return (secretEntity, valueContext) -> {
			if (HMAC.validateKey(HMAC.byteToHex(secretEntity.getSecretKey()))) {
				return ValidationResult.ok();
			}
			else {
				return ValidationResult.error("Secret is not valid");
			}
		};
	}

	/**
	 * Validate key is present.
	 * @return validation ok if key is not null or empty, validation error otherwise
	 */
	private Validator<SecretEntity> secretMandatoryValidator() {
		return (secretEntity, valueContext) -> {
			if (secretComboBox.getValue() != null && secretEntity.getSecretKey() != null
					&& secretEntity.getSecretKey().length > 0) {
				return ValidationResult.ok();
			}
			else {
				return ValidationResult.error("Secret is mandatory");
			}
		};
	}

}
