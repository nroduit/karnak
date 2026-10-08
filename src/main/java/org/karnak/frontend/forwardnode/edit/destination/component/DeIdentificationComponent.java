/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.frontend.component.ProjectDropDown;
import org.weasis.core.util.annotations.Generated;

@Getter
@Generated()
@NullUnmarked
public class DeIdentificationComponent extends VerticalLayout {

	// Labels
	private static final String LABEL_CHECKBOX_DEIDENTIFICATION = "Activate de-identification";

	private static final String LABEL_DISCLAIMER_DEIDENTIFICATION = "In order to ensure complete de-identification, visual verification of metadata and images is necessary.";

	private static final String LABEL_DEFAULT_ISSUER = "If filled, it is combined with the Patient ID to ensure unique patient identification across different healthcare systems.";

	private static final String LABEL_CHECKBOX_SKIP_ISSUER = "Ignore Issuer of Patient ID";

	private static final String HELPER_SKIP_ISSUER = "When checked, the Issuer of Patient ID is not used to build the key that retrieves the pseudonym from the cache. Only applies to \"Pseudonym is already stored in KARNAK\".";

	// Components
	private Checkbox deIdentificationCheckbox;

	private Checkbox skipIssuerOfPatientIdCheckbox;

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

		// Group the skip checkbox + issuer text field in a dedicated block, with enough
		// spacing that each control reads together with its own helper text. The
		// "Ignore Issuer of Patient ID" checkbox comes first because it governs the
		// issuer field below it (checking it disables and clears that field).
		VerticalLayout issuerLayout = new VerticalLayout();
		issuerLayout.setPadding(false);
		issuerLayout.setSpacing(false);
		issuerLayout.setWidthFull();
		issuerLayout.getStyle().set("gap", "1.25rem");
		issuerLayout.add(skipIssuerOfPatientIdCheckbox, issuerOfPatientIDByDefault);

		// Keep the resolved-profile label tight under the project field, so it reads as
		// that field's helper rather than a separate control
		VerticalLayout projectGroup = new VerticalLayout();
		projectGroup.setPadding(false);
		projectGroup.setSpacing(false);
		projectGroup.setWidthFull();
		projectGroup.getStyle().set("gap", "0.25rem");
		projectGroup.add(projectDropDown, profileLabel);

		// Stack the controls in two readable sections: which project/profile to apply,
		// then how the pseudonym is generated. A generous gap keeps each field visually
		// bound to its own helper text instead of the next component's label.
		VerticalLayout content = new VerticalLayout();
		content.setPadding(false);
		content.setSpacing(false);
		content.setWidthFull();
		content.getStyle().set("gap", "1.25rem");
		content.add(disclaimerLabel, sectionTitle("Project & profile"), projectGroup,
				sectionTitle("Pseudonym generation"), pseudonymTypeSelect, pseudonymDicomTagDiv, pseudonymApi,
				issuerLayout);
		deIdentificationDiv.add(content);

		// If checkbox is checked set div visible, invisible otherwise
		deIdentificationDiv.setVisible(deIdentificationCheckbox.getValue());

		// Checkbox as a full-width header row, the form fields stacked below it
		add(deIdentificationCheckbox, deIdentificationDiv);
	}

	/**
	 * Build a small uppercase heading that groups related fields inside the card
	 * @param text Heading text
	 * @return Styled heading span
	 */
	private static Span sectionTitle(String text) {
		Span title = new Span(text);
		title.addClassName("karnak-section-title");
		return title;
	}

	/**
	 * Build listeners
	 */
	private void buildListeners() {
		buildPseudonymTypeListener();
		buildSkipIssuerListener();
		destinationComponentUtil.buildWarningNoProjectDefinedListener(warningNoProjectsDefined,
				deIdentificationCheckbox);
		destinationComponentUtil.buildProjectDropDownListener(projectDropDown, profileLabel);
	}

	/**
	 * Build deidentification components
	 */
	private void buildComponents() {
		buildIssuerOfPatientID();
		buildSkipIssuerOfPatientIdCheckbox();
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
		// Keep the selector compact rather than spanning the whole panel; the option
		// labels never need the full destination-panel width.
		pseudonymTypeSelect.setWidth("350px");
		pseudonymTypeSelect.setHelperText(
				"How Karnak obtains the pseudonym: from its own cache, from a DICOM tag in the image, or from an external API.");
		pseudonymTypeSelect.setItems(CACHE_EXTID.getValue(), EXTID_IN_TAG.getValue(), EXTID_API.getValue());
	}

	/**
	 * Build disclaimer
	 */
	private void buildDisclaimerLabel() {
		disclaimerLabel = new NativeLabel(LABEL_DISCLAIMER_DEIDENTIFICATION);
		disclaimerLabel.addClassName("karnak-note-text");
		disclaimerLabel.setWidthFull();
	}

	/**
	 * Build issuer of patient ID
	 */
	private void buildIssuerOfPatientID() {
		issuerOfPatientIDByDefault = new TextField();
		issuerOfPatientIDByDefault.setLabel("Issuer of Patient ID by default");
		// Half-width is plenty for an issuer identifier; no need to span the whole panel.
		issuerOfPatientIDByDefault.setWidth("50%");
		issuerOfPatientIDByDefault.setPlaceholder("e.g. hospital identifier");
		issuerOfPatientIDByDefault.setHelperText(LABEL_DEFAULT_ISSUER);
		// Only relevant to CACHE_EXTID ("Pseudonym is already stored in KARNAK"), where
		// it
		// is the fallback issuer used to build the cache key; hidden for the other types.
		issuerOfPatientIDByDefault.setVisible(false);
	}

	/**
	 * Build skip issuer of patient ID checkbox — hidden by default until CACHE_EXTID is
	 * selected
	 */
	private void buildSkipIssuerOfPatientIdCheckbox() {
		skipIssuerOfPatientIdCheckbox = new Checkbox(LABEL_CHECKBOX_SKIP_ISSUER);
		skipIssuerOfPatientIdCheckbox.setVisible(false);
		skipIssuerOfPatientIdCheckbox.setHelperText(HELPER_SKIP_ISSUER);
	}

	/**
	 * Listener on skip issuer checkbox: disables the issuer text field when checked
	 */
	private void buildSkipIssuerListener() {
		skipIssuerOfPatientIdCheckbox.addValueChangeListener(event -> {
			boolean skip = Boolean.TRUE.equals(event.getValue());
			issuerOfPatientIDByDefault.setEnabled(!skip);
			if (skip) {
				issuerOfPatientIDByDefault.clear();
			}
		});
	}

	/**
	 * Listener on pseudonym type
	 */
	private void buildPseudonymTypeListener() {
		pseudonymTypeSelect.addValueChangeListener(event -> {
			if (event.getValue() != null) {
				pseudonymDicomTagDiv.setVisible(Objects.equals(event.getValue(), EXTID_IN_TAG.getValue()));
				pseudonymApi.setVisible(Objects.equals(event.getValue(), EXTID_API.getValue()));
				updateSkipIssuerCheckboxState(event.getValue());
			}
		});
	}

	/**
	 * Show the skip issuer checkbox only for CACHE_EXTID, hide and reset it otherwise
	 * @param pseudonymTypeValue Currently selected pseudonym type value
	 */
	private void updateSkipIssuerCheckboxState(String pseudonymTypeValue) {
		boolean isCacheExtid = Objects.equals(pseudonymTypeValue, CACHE_EXTID.getValue());
		issuerOfPatientIDByDefault.setVisible(isCacheExtid);
		skipIssuerOfPatientIdCheckbox.setVisible(isCacheExtid);
		if (!isCacheExtid) {
			skipIssuerOfPatientIdCheckbox.setValue(false);
			issuerOfPatientIDByDefault.setEnabled(true);
		}
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
		destinationBinder.forField(skipIssuerOfPatientIdCheckbox)
			.bind(DestinationEntity::isSkipIssuerOfPatientId, DestinationEntity::setSkipIssuerOfPatientId);
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
			// Reset the destination for pseudonym type, project, issuer of patient id,
			// skip issuer
			destinationEntity.setDeIdentificationProjectEntity(null);
			destinationEntity.setPseudonymType(CACHE_EXTID);
			destinationEntity.setIssuerByDefault(null);
			destinationEntity.setSkipIssuerOfPatientId(false);
		}
	}

}
