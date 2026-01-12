/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.destination.component;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.karnak.backend.constant.Notification;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.frontend.component.converter.HStringToIntegerConverter;
import org.karnak.frontend.util.UIS;

/**
 * Create a notification component
 */
public class NotificationComponent extends VerticalLayout {

	// Components
	@Setter
	@Getter
	private TextField notify;

	@Setter
	@Getter
	private TextField notifyObjectErrorPrefix;

	@Setter
	@Getter
	private TextField notifyObjectRejectionPrefix;

	@Setter
	@Getter
	private TextField notifyObjectPattern;

	@Setter
	@Getter
	private TextField notifyObjectValues;

	@Setter
	@Getter
	private TextField notifyInterval;

	@Setter
	@Getter
	private Checkbox activateNotification;

	private Div notificationInputsDiv;

	private Div notificationObjectsDiv;

	/**
	 * Constructor
	 */
	public NotificationComponent() {
		// Size
		setWidthFull();

		// In order to not have a padding around the component
		setPadding(true);

		// Build notification components
		buildComponents();

		// Build listeners
		buildListeners();

		// Add components
		addComponents();
	}

	/**
	 * Add components in notification components
	 */
	private void addComponents() {
		// notificationObjectsDiv.add(UIS.setWidthFull(new HorizontalLayout(notify)),
		// UIS.setWidthFull(new HorizontalLayout(notifyObjectErrorPrefix,
		// notifyObjectRejectionPrefix)));
		notificationInputsDiv.add(UIS.setWidthFull(new VerticalLayout(UIS.setWidthFull(new HorizontalLayout(notify)),
				UIS.setWidthFull(new HorizontalLayout(notifyObjectErrorPrefix, notifyObjectRejectionPrefix)),
				UIS.setWidthFull(new HorizontalLayout(notifyObjectPattern, notifyObjectValues, notifyInterval)))));

		add(UIS.setWidthFull(new HorizontalLayout(activateNotification)), notificationInputsDiv);
	}

	/**
	 * Build listeners on components
	 */
	private void buildListeners() {
		buildListenerActivateNotification();
	}

	/**
	 * Listener activate notification
	 */
	private void buildListenerActivateNotification() {
		activateNotification.addValueChangeListener(event -> {
			if (event != null && event.getValue()) {
				notificationInputsDiv.setVisible(true);
				notificationObjectsDiv.setVisible(true);
				// Set default values if null or empty
				updateDefaultValuesNotificationTextFields();
			}
			else {
				notificationInputsDiv.setVisible(false);
				notificationObjectsDiv.setVisible(false);
			}
		});
	}

	/**
	 * Set default values if textfield values are null or empty
	 */
	private void updateDefaultValuesNotificationTextFields() {
		if (notifyObjectErrorPrefix.getValue() == null || notifyObjectErrorPrefix.getValue().trim().isEmpty()) {
			notifyObjectErrorPrefix.setValue(Notification.DEFAULT_SUBJECT_ERROR_PREFIX);
		}
		if (notifyObjectRejectionPrefix.getValue() == null || notifyObjectRejectionPrefix.getValue().trim().isEmpty()) {
			notifyObjectRejectionPrefix.setValue(Notification.DEFAULT_SUBJECT_REJECTION_PREFIX);
		}
		if (notifyObjectPattern.getValue() == null || notifyObjectPattern.getValue().trim().isEmpty()) {
			notifyObjectPattern.setValue(Notification.DEFAULT_SUBJECT_PATTERN);
		}
		if (notifyObjectValues.getValue() == null || notifyObjectValues.getValue().trim().isEmpty()) {
			notifyObjectValues.setValue(Notification.DEFAULT_SUBJECT_VALUES);
		}
		if (notifyInterval.getValue() == null || notifyInterval.getValue().trim().isEmpty()) {
			notifyInterval.setValue(Notification.DEFAULT_INTERVAL);
		}
	}

	/**
	 * Set default values if notification values are null or empty
	 * @param destinationEntity Destination to update
	 */
	public void updateDefaultValuesNotification(DestinationEntity destinationEntity) {
		if (destinationEntity.getNotifyObjectErrorPrefix() == null
				|| destinationEntity.getNotifyObjectErrorPrefix().trim().isEmpty()) {
			destinationEntity.setNotifyObjectErrorPrefix(Notification.DEFAULT_SUBJECT_ERROR_PREFIX);
		}
		if (destinationEntity.getNotifyObjectRejectionPrefix() == null
				|| destinationEntity.getNotifyObjectRejectionPrefix().trim().isEmpty()) {
			destinationEntity.setNotifyObjectRejectionPrefix(Notification.DEFAULT_SUBJECT_REJECTION_PREFIX);
		}
		if (destinationEntity.getNotifyObjectPattern() == null
				|| destinationEntity.getNotifyObjectPattern().trim().isEmpty()) {
			destinationEntity.setNotifyObjectPattern(Notification.DEFAULT_SUBJECT_PATTERN);
		}
		if (destinationEntity.getNotifyObjectValues() == null
				|| destinationEntity.getNotifyObjectValues().trim().isEmpty()) {
			destinationEntity.setNotifyObjectValues(Notification.DEFAULT_SUBJECT_VALUES);
		}
		if (destinationEntity.getNotifyInterval() == null || destinationEntity.getNotifyInterval() == 0) {
			destinationEntity.setNotifyInterval(Integer.parseInt(Notification.DEFAULT_INTERVAL));
		}
	}

	/**
	 * Build components used in Notification component
	 */
	private void buildComponents() {
		buildNotificationInputsDiv();
		buildNotificationObjectsDiv();
		buildActivateNotification();
		buildNotify();
		buildNotifyObjectErrorPrefix();
		buildNotifyObjectRejectionPrefix();
		buildNotifyObjectPattern();
		buildNotifyObjectValues();
		buildNotifyInterval();
	}

	/**
	 * Notify interval
	 */
	private void buildNotifyInterval() {
		notifyInterval = new TextField(String.format("Notif.: interval (Default: %s)", Notification.DEFAULT_INTERVAL));
		notifyInterval.setWidth("32%");
		notifyInterval.addThemeVariants(TextFieldVariant.LUMO_ALIGN_RIGHT);
		UIS.setTooltip(notifyInterval, String.format(
				"Interval in seconds for sending a notification (when no new image is arrived in the archive folder). Default value: %s",
				Notification.DEFAULT_INTERVAL));
	}

	/**
	 * Notify Object Values
	 */
	private void buildNotifyObjectValues() {
		notifyObjectValues = new TextField(
				String.format("Notif.: subject values (Default: %s)", Notification.DEFAULT_SUBJECT_VALUES));
		notifyObjectValues.setWidth("32%");
		UIS.setTooltip(notifyObjectValues, String.format(
				"Values injected in the pattern [PatientID StudyDescription StudyDate StudyInstanceUID]. Default value: %s",
				Notification.DEFAULT_SUBJECT_VALUES));
	}

	/**
	 * Notify Object Pattern
	 */
	private void buildNotifyObjectPattern() {
		notifyObjectPattern = new TextField(
				String.format("Notif.: subject pattern (Default: %s)", Notification.DEFAULT_SUBJECT_PATTERN));
		notifyObjectPattern.setWidth("32%");
		UIS.setTooltip(notifyObjectPattern, String.format(
				"Pattern of the email object, see https://dzone.com/articles/java-string-format-examples. Default value: %s",
				Notification.DEFAULT_SUBJECT_PATTERN));
	}

	/**
	 * Notify Object Error Prefix
	 */
	private void buildNotifyObjectErrorPrefix() {
		notifyObjectErrorPrefix = new TextField(
				String.format("Notif.: error subject prefix (Default: %s)", Notification.DEFAULT_SUBJECT_ERROR_PREFIX));
		notifyObjectErrorPrefix.setWidth("49%");
		UIS.setTooltip(notifyObjectErrorPrefix,
				String.format("Prefix of the email object when containing an issue. Default value: %s",
						Notification.DEFAULT_SUBJECT_ERROR_PREFIX));
	}

	/**
	 * Notify Object Rejection Prefix
	 */
	private void buildNotifyObjectRejectionPrefix() {
		notifyObjectRejectionPrefix = new TextField(String.format("Notif.: rejection subject prefix (Default: %s)",
				Notification.DEFAULT_SUBJECT_REJECTION_PREFIX));
		notifyObjectRejectionPrefix.setWidth("49%");
		UIS.setTooltip(notifyObjectRejectionPrefix,
				String.format("Prefix of the email object in case of rejections. Default value: %s",
						Notification.DEFAULT_SUBJECT_REJECTION_PREFIX));
	}

	/**
	 * Notify
	 */
	private void buildNotify() {
		notify = new TextField("Notif.: list of emails");
		notify.setWidth("100%");
		notify.getStyle().set("padding-top", "0");
		notify.getStyle().set("padding", "0");
	}

	/**
	 * Activate Notification
	 */
	private void buildActivateNotification() {
		activateNotification = new Checkbox("Activate notification");
		// By default deactivate
		activateNotification.setValue(false);
	}

	/**
	 * Notification Inputs Div
	 */
	private void buildNotificationInputsDiv() {
		notificationInputsDiv = new Div();
		// By default hide
		notificationInputsDiv.setVisible(false);
		notificationInputsDiv.setWidthFull();
	}

	/**
	 * Notification Inputs Div
	 */
	private void buildNotificationObjectsDiv() {
		notificationObjectsDiv = new Div();
		// By default hide
		notificationObjectsDiv.setVisible(false);
		notificationObjectsDiv.setWidthFull();
	}

	/**
	 * Init binder for the component
	 * @param binder Binder
	 */
	public void init(Binder<DestinationEntity> binder) {

		// Activate notification
		binder.forField(getActivateNotification())
			.bind(DestinationEntity::isActivateNotification, DestinationEntity::setActivateNotification);

		// List of emails
		binder.forField(getNotify()).withValidator((s, valueContext) -> {
			if (StringUtils.isBlank(s) && getActivateNotification().getValue()) {
				return ValidationResult.error("Should have at least one address email");
			}
			return ValidationResult.ok();
		}).bind(DestinationEntity::getNotify, DestinationEntity::setNotify);

		// Interval
		binder.forField(getNotifyInterval()) //
			.withConverter(new HStringToIntegerConverter()) //
			.bind(DestinationEntity::getNotifyInterval, DestinationEntity::setNotifyInterval);

		// Error Prefix
		binder.forField(getNotifyObjectErrorPrefix())
			.bind(DestinationEntity::getNotifyObjectErrorPrefix, DestinationEntity::setNotifyObjectErrorPrefix);

		// Error Prefix
		binder.forField(getNotifyObjectRejectionPrefix())
			.bind(DestinationEntity::getNotifyObjectRejectionPrefix, DestinationEntity::setNotifyObjectRejectionPrefix);

		// Subject Pattern
		binder.forField(getNotifyObjectPattern())
			.bind(DestinationEntity::getNotifyObjectPattern, DestinationEntity::setNotifyObjectPattern);

		// Subject Values
		binder.forField(getNotifyObjectValues())
			.bind(DestinationEntity::getNotifyObjectValues, DestinationEntity::setNotifyObjectValues);
	}

}
