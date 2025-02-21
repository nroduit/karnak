/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.pseudonym.mapping.component;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.karnak.backend.cache.Patient;
import org.karnak.backend.util.DateFormat;
import org.karnak.frontend.component.BoxShadowComponent;

/**
 * Result component
 */
public class MappingResultComponent extends VerticalLayout {

	// Components
	private Details patientFoundDetails;

	/**
	 * Constructor
	 */
	public MappingResultComponent() {
		setWidth(98, Unit.PERCENTAGE);

		// Result
		patientFoundDetails = new Details();
		BoxShadowComponent patientFoundBoxShadowComponent = new BoxShadowComponent(patientFoundDetails);
		patientFoundBoxShadowComponent.getElement().getStyle().set("padding", "1%");
		patientFoundBoxShadowComponent.setWidthFull();
		add(patientFoundBoxShadowComponent);
	}

	/**
	 * Handle result when Find is clicked
	 * @param patientFound patient found/not found by the backend
	 * @param inputValue Input value entered by the user
	 * @param location from where the mapping has been taken from
	 */
	public void handleResultFindPatient(Patient patientFound, String inputValue, String location) {
		// Reset details
		getPatientFoundDetails().removeAll();

		// If found set patient data
		if (patientFound != null) {
			handleResultFindPatientPatientFound(patientFound, location);
		}
		else {
			// Else notification message
			handleResultFindPatientPatientNotFound(inputValue, location);
		}
	}

	/**
	 * Handle Result of Find Patient when Patient Not Found
	 * @param inputValue Input value entered by the user
	 * @param location from where the mapping has been taken from
	 */
	public static void handleResultFindPatientPatientNotFound(String inputValue, String location) {
		Span content = new Span(String.format("%s Pseudonym %s not found", location, inputValue));
		content.getStyle().set("color", "var(--lumo-error-text-color)");
		Notification notification = new Notification(content);
		notification.setDuration(3000);
		notification.setPosition(Position.TOP_CENTER);
		notification.open();
	}

	/**
	 * Handle Result of Find Patient when Patient Found
	 * @param patientFound Patient found
	 * @param location Where the patient has been stored
	 */
	private void handleResultFindPatientPatientFound(Patient patientFound, String location) {
		getPatientFoundDetails().setOpened(true);

		// Summary
		NativeLabel summaryDetails = new NativeLabel(location);
		summaryDetails.getStyle().set("font-size", "large").set("font-weight", "bolder");
		getPatientFoundDetails().setSummary(summaryDetails);

		// Add result in details
		HorizontalLayout patientFoundDetailLayout = new HorizontalLayout();
		patientFoundDetailLayout.setAlignItems(Alignment.START);
		patientFoundDetailLayout.setWidthFull();
		addDetailPatientFound(patientFoundDetailLayout, "Patient Id", patientFound.getPatientId());
		addDetailPatientFound(patientFoundDetailLayout, "Last Name", patientFound.getPatientLastName());
		addDetailPatientFound(patientFoundDetailLayout, "First Name", patientFound.getPatientFirstName());
		addDetailPatientFound(patientFoundDetailLayout, "Birth Date",
				DateFormat.format(patientFound.getPatientBirthDate(), DateFormat.FORMAT_DDMMYYYY_SLASH));
		addDetailPatientFound(patientFoundDetailLayout, "Issuer Of Patient Id", patientFound.getIssuerOfPatientId());
		addDetailPatientFound(patientFoundDetailLayout, "Sex", patientFound.getPatientSex());

		// Add layout
		getPatientFoundDetails().add(patientFoundDetailLayout);
	}

	/**
	 * Add information of the patient found in badge title components
	 * @param patientFoundDetailLayout Layout
	 * @param title Title
	 * @param value Value of the patient found
	 */
	private void addDetailPatientFound(HorizontalLayout patientFoundDetailLayout, String title, String value) {
		if (value != null) {
			patientFoundDetailLayout.add(buildBadgeTitle(String.format("%s: %s", title, value)));
		}
	}

	/**
	 * Build badge title
	 * @param title Title
	 * @return BoxShadowComponent designed with the title
	 */
	private BoxShadowComponent buildBadgeTitle(final String title) {
		// Title
		NativeLabel label = new NativeLabel(title);
		label.getElement().getStyle().set("padding", "10px");

		// Badge
		BoxShadowComponent badgeTitle = new BoxShadowComponent(label);
		badgeTitle.getStyle().set("font-size", "revert").set("font-weight", "bolder");
		badgeTitle.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
		badgeTitle.getStyle().set("color", "var(--lumo-primary-text-color)");

		return badgeTitle;
	}

	public Details getPatientFoundDetails() {
		return patientFoundDetails;
	}

	public void setPatientFoundDetails(Details patientFoundDetails) {
		this.patientFoundDetails = patientFoundDetails;
	}

}
