/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.destination.component;

import static org.karnak.backend.enums.PseudonymType.CACHE_EXTID;
import static org.karnak.backend.enums.PseudonymType.EXTID_API;
import static org.karnak.backend.enums.PseudonymType.EXTID_IN_TAG;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.frontend.component.ProjectDropDown;
import org.karnak.frontend.util.UIS;

@Getter
public class DeIdentificationComponent extends VerticalLayout {

	// Labels
	private static final String LABEL_CHECKBOX_DEIDENTIFICATION = "Activate de-identification";

	private static final String LABEL_DISCLAIMER_DEIDENTIFICATION = "In order to ensure complete de-identification, visual verification of metadata and images is necessary.";

	private static final String LABEL_DEFAULT_ISSUER = "If this field is filled, it will be used with the Patient ID to ensure unique patient identification across different healthcare systems.";

	// Components
	private Checkbox deIdentificationCheckbox;

	private NativeLabel disclaimerLabel;

	private ProjectDropDown projectDropDown;

	private PseudonymInDicomTagComponent pseudonymInDicomTagComponent;

	private PseudonymFromApi pseudonymFromApiComponent;

	@Setter
	private Binder<DestinationEntity> destinationBinder;

	private Div pseudonymDicomTagDiv;

	private Div pseudonymApi;

	private Div deIdentificationDiv;

	private ProfileLabel profileLabel;

	private WarningNoProjectsDefined warningNoProjectsDefined;

	private Select<String> pseudonymTypeSelect;

	private TextField issuerOfPatientIDByDefault;

	private final DestinationComponentUtil destinationComponentUtil;

	/**
	 * Constructor
	 */
	public DeIdentificationComponent() {
		this.destinationComponentUtil = new DestinationComponentUtil();
	}

	/**
	 * Init deidentification component
	 * @param binder Binder for checks
	 */
	public void init(final Binder<DestinationEntity> binder) {
		// Init destination binder
		setDestinationBinder(binder);

		// Build deidentification components
		buildComponents();

		// Init destination binder
		initDestinationBinder();

		// Build Listeners
		buildListeners();

		// Add components
		addComponents();
	}

	/**
	 * Add components
	 */
	private void addComponents() {
		// Padding
		setPadding(true);

		// Add components in deidentification div
		deIdentificationDiv.add(disclaimerLabel, projectDropDown, profileLabel, pseudonymTypeSelect,
				pseudonymDicomTagDiv, pseudonymApi, issuerOfPatientIDByDefault);

		// If checkbox is checked set div visible, invisible otherwise
		deIdentificationDiv.setVisible(deIdentificationCheckbox.getValue());

		// Add components in view
		add(UIS.setWidthFull(new HorizontalLayout(deIdentificationCheckbox, deIdentificationDiv)));
	}

	/**
	 * Build listeners
	 */
	private void buildListeners() {
		buildPseudonymTypeListener();
		destinationComponentUtil.buildWarningNoProjectDefinedListener(warningNoProjectsDefined,
				deIdentificationCheckbox);
		destinationComponentUtil.buildProjectDropDownListener(projectDropDown, profileLabel);
	}

	/**
	 * Build deidentification components
	 */
	private void buildComponents() {
		buildIssuerOfPatientID();
		projectDropDown = destinationComponentUtil.buildProjectDropDown();
		profileLabel = new ProfileLabel();
		warningNoProjectsDefined = destinationComponentUtil.buildWarningNoProjectDefined();
		deIdentificationCheckbox = destinationComponentUtil.buildActivateCheckbox(LABEL_CHECKBOX_DEIDENTIFICATION);
		buildDisclaimerLabel();
		buildPseudonymTypeSelect();
		buildPseudonymInDicomTagComponent();
		buildPseudonymFromApiComponent();
		deIdentificationDiv = destinationComponentUtil.buildActivateDiv();
		buildPseudonymDicomTagDiv();
		buildPseudonymApi();
	}

	/**
	 * Build Pseudonym In Dicom Tag Component
	 */
	private void buildPseudonymInDicomTagComponent() {
		pseudonymInDicomTagComponent = new PseudonymInDicomTagComponent(destinationBinder);
	}

	private void buildPseudonymFromApiComponent() {
		pseudonymFromApiComponent = new PseudonymFromApi(destinationBinder);
	}

	/**
	 * Build Pseudonym Dicom Tag Div which is visible if "Pseudonym is in a dicom tag" is
	 * selected
	 */
	private void buildPseudonymDicomTagDiv() {
		pseudonymDicomTagDiv = new Div();
		pseudonymDicomTagDiv.add(pseudonymInDicomTagComponent);
	}

	private void buildPseudonymApi() {
		pseudonymApi = new Div();
		pseudonymApi.add(pseudonymFromApiComponent);
	}

	/**
	 * Build pseudonym type
	 */
	private void buildPseudonymTypeSelect() {
		pseudonymTypeSelect = new Select<>();
		pseudonymTypeSelect.setLabel("Pseudonym type");
		pseudonymTypeSelect.setWidth("100%");
		pseudonymTypeSelect.getStyle().set("right", "0px");
		pseudonymTypeSelect.setItems(CACHE_EXTID.getValue(), EXTID_IN_TAG.getValue(), EXTID_API.getValue());
	}

	/**
	 * Build disclaimer
	 */
	private void buildDisclaimerLabel() {
		disclaimerLabel = new NativeLabel(LABEL_DISCLAIMER_DEIDENTIFICATION);
		disclaimerLabel.getStyle().set("color", "red");
		disclaimerLabel.setMinWidth("75%");
		disclaimerLabel.getStyle().set("right", "0px");
	}

	/**
	 * Build issuer of patient ID
	 */
	private void buildIssuerOfPatientID() {
		issuerOfPatientIDByDefault = new TextField();
		issuerOfPatientIDByDefault.setLabel("Issuer of Patient ID by default");
		issuerOfPatientIDByDefault.setWidth("100%");
		issuerOfPatientIDByDefault.setPlaceholder(LABEL_DEFAULT_ISSUER);
		UIS.setTooltip(issuerOfPatientIDByDefault, LABEL_DEFAULT_ISSUER);
	}

	/**
	 * Listener on pseudonym type
	 */
	private void buildPseudonymTypeListener() {
		pseudonymTypeSelect.addValueChangeListener(event -> {
			if (event.getValue() != null) {
				pseudonymDicomTagDiv.setVisible(Objects.equals(event.getValue(), EXTID_IN_TAG.getValue()));
				pseudonymApi.setVisible(Objects.equals(event.getValue(), EXTID_API.getValue()));
			}
		});
	}

	private void initDestinationBinder() {
		destinationBinder.forField(issuerOfPatientIDByDefault)
			.bind(DestinationEntity::getIssuerByDefault, (destinationEntity, s) -> {
				if (deIdentificationCheckbox.getValue()) {
					destinationEntity.setIssuerByDefault(s);
				}
				else {
					destinationEntity.setIssuerByDefault("");
				}
			});
		destinationBinder.forField(deIdentificationCheckbox)
			.bind(DestinationEntity::isDesidentification, DestinationEntity::setDesidentification);
		destinationBinder.forField(projectDropDown)
			.withValidator(project -> project != null || !deIdentificationCheckbox.getValue(), "Choose a project")
			.bind(DestinationEntity::getDeIdentificationProjectEntity,
					DestinationEntity::setDeIdentificationProjectEntity);

		destinationBinder.forField(pseudonymTypeSelect)
			.withValidator(Objects::nonNull, "Choose pseudonym type\n")
			.bind(destination -> destination.getPseudonymType().getValue(), (destination, s) -> {
				if (s.equals(EXTID_IN_TAG.getValue())) {
					destination.setPseudonymType(EXTID_IN_TAG);
				}
				else if (s.equals(EXTID_API.getValue())) {
					destination.setPseudonymType(EXTID_API);
				}
				else if (s.equals(CACHE_EXTID.getValue())) {
					destination.setPseudonymType(CACHE_EXTID);
				}
			});
	}

	/**
	 * Clean fields of destination which are not saved because not selected by user
	 * @param destinationEntity Destination to clean
	 */
	public void cleanUnSavedData(DestinationEntity destinationEntity) {
		// Reset the destination for the part tag is in dicom tag in case the pseudonym
		// type selected is not pseudonym in dicom tag or deidentification not active
		if (!destinationEntity.isDesidentification()
				|| !Objects.equals(destinationEntity.getPseudonymType(), EXTID_IN_TAG)) {
			destinationEntity.setTag(null);
			destinationEntity.setDelimiter(null);
			destinationEntity.setPosition(null);
			destinationEntity.setSavePseudonym(null);
		}

		if (!destinationEntity.isDesidentification()) {
			// Reset the destination for pseudonym type, project, issuer of patient id
			destinationEntity.setDeIdentificationProjectEntity(null);
			destinationEntity.setPseudonymType(CACHE_EXTID);
			destinationEntity.setIssuerByDefault(null);
		}
	}

}
