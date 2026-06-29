/*
 * Copyright (c) 2021-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.pseudonym.mapping;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.cache.Patient;
import org.karnak.frontend.MainLayout;
import org.karnak.frontend.pseudonym.mapping.component.MappingInputComponent;
import org.karnak.frontend.pseudonym.mapping.component.MappingResultComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.weasis.core.util.annotations.Generated;

@Route(value = PseudonymMappingView.ROUTE, layout = MainLayout.class)
@PageTitle("Karnak - Mapping Pseudonym")
@Tag("mapping-pseudonym-view")
@RolesAllowed("investigator")
@Generated()
@NullUnmarked
public class PseudonymMappingView extends HorizontalLayout {

	public static final String VIEW_NAME = "Pseudonym mapping ";

	public static final String ROUTE = "mapping";

	// Layout
	private VerticalLayout mappingLayout;

	// Mapping Logic
	private final PseudonymMappingLogic pseudonymMappingLogic;

	// Components
	private List<MappingResultComponent> mappingResultComponents;

	private MappingInputComponent mappingInputComponent;

	private NativeLabel pseudonymToLookForLabel;

	/**
	 * Autowired constructor.
	 * @param pseudonymMappingLogic Mapping Logic used to call backend services and
	 * implement logic linked to the view
	 */
	@Autowired
	public PseudonymMappingView(final PseudonymMappingLogic pseudonymMappingLogic) {
		// Bind the autowired service
		this.pseudonymMappingLogic = pseudonymMappingLogic;

		// Set the view in the service
		this.pseudonymMappingLogic.setMappingView(this);

		// Build components
		buildComponents();

		// Build listeners
		buildListeners();

		// Add components in the view
		addComponentsView();
	}

	/**
	 * Build listeners
	 */
	private void buildListeners() {

		// Find patient listener
		mappingInputComponent.getFindButton().addClickListener(event -> {
			if (mappingInputComponent.getPseudonymTextField().getValue() != null
					&& !mappingInputComponent.getPseudonymTextField().getValue().isEmpty()) {

				// Reset previous results found
				removePreviousResultsFound();

				// Create title searched pseudonym
				buildTitlePseudonymToLookFor(mappingInputComponent.getPseudonymTextField().getValue());

				// Find mapping pseudonym/patient store in external id cache
				mappingFindPatientInExternalIDCache();

				// reset value pseudonym entered by the user
				mappingInputComponent.getPseudonymTextField().clear();

				// Case no result found: change label title
				if (mappingResultComponents != null && mappingResultComponents.isEmpty()) {
					pseudonymToLookForLabel
						.setText(String.format("%s: no mapping found", pseudonymToLookForLabel.getText()));
				}
			}
		});
	}

	/**
	 * Remove previous results found to clear the view
	 */
	private void removePreviousResultsFound() {
		mappingResultComponents.forEach(c -> mappingLayout.remove(c));
		if (pseudonymToLookForLabel != null) {
			mappingLayout.remove(pseudonymToLookForLabel);
		}
		mappingResultComponents.clear();
	}

	/**
	 * Build title pseudonym to look for
	 * @param pseudonym pseudonym to look for
	 */
	private void buildTitlePseudonymToLookFor(String pseudonym) {
		pseudonymToLookForLabel = new NativeLabel(String.format("Pseudonym %s", pseudonym));
		pseudonymToLookForLabel.getElement().getStyle().set("margin-left", "1em");
		pseudonymToLookForLabel.getStyle().set("font-size", "large").set("font-weight", "bolder");

		// Add in the layout
		mappingLayout.add(pseudonymToLookForLabel);
	}

	/**
	 * Find mapping patient in cache for all projects
	 */
	private void mappingFindPatientInExternalIDCache() {

		// Retrieve pseudonym patient mapping in all projects
		Map<String, Patient> mappingPseudoProjectPatientFound = pseudonymMappingLogic
			.retrieveExternalIDCachePatients(mappingInputComponent.getPseudonymTextField().getValue());

		// Handle result find patient
		if (!mappingPseudoProjectPatientFound.isEmpty()) {
			mappingPseudoProjectPatientFound.forEach((project, patient) -> {
				MappingResultComponent mappingResultComponent = new MappingResultComponent();
				mappingResultComponent.handleResultFindPatient(patient,
						mappingInputComponent.getPseudonymTextField().getValue(),
						String.format("[External][%s]", project));

				// Add in the list of components to reset
				mappingResultComponents.add(mappingResultComponent);

				// Add in the layout
				mappingLayout.add(mappingResultComponent);
			});
		}
		else {
			MappingResultComponent.handleResultFindPatientPatientNotFound(
					mappingInputComponent.getPseudonymTextField().getValue(), "[External]");
		}
	}

	/**
	 * Build components
	 */
	private void buildComponents() {
		// Input pseudonym
		mappingInputComponent = new MappingInputComponent();

		// List of components to remove when new search is done
		mappingResultComponents = new ArrayList<>();
	}

	/**
	 * Add components in the view
	 */
	private void addComponentsView() {
		setSizeFull();

		// Layout
		mappingLayout = new VerticalLayout();

		// Titles
		mappingLayout.add(new H2("Pseudonym Mapping"));

		// Input
		mappingLayout.add(mappingInputComponent);

		// Layout
		add(mappingLayout);
	}

}
