/*
 * Copyright (c) 2026 Karnak Team and other contributors.
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
import com.vaadin.flow.data.binder.Binder;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.DestinationEntity;
import org.karnak.frontend.util.UIS;
import org.weasis.core.util.annotations.Generated;

/**
 * DICOM conformance report configuration, kept separate from the notification settings:
 * it has its own activation and its own recipient list (with a fallback to the
 * notification emails when left blank). Custom report options will be added here.
 */
@Generated()
@NullUnmarked
public class ConformanceReportComponent extends VerticalLayout {

	@Setter
	@Getter
	private Checkbox virtualDestination;

	@Setter
	@Getter
	private Checkbox buildConformanceReport;

	@Setter
	@Getter
	private TextField conformanceReportNotify;

	@Setter
	@Getter
	private Checkbox checkValueConformity;

	@Setter
	@Getter
	private Checkbox deepSequenceValidation;

	private Div optionsDiv;

	public ConformanceReportComponent() {
		setWidthFull();
		setPadding(true);

		buildComponents();
		buildListeners();
		addComponents();
	}

	private void buildComponents() {
		buildOptionsDiv();
		buildVirtualDestination();
		buildBuildConformanceReport();
		buildConformanceReportNotify();
		buildCheckValueConformity();
		buildDeepSequenceValidation();
	}

	private void buildVirtualDestination() {
		virtualDestination = new Checkbox("Virtual destination (report only, discard DICOM)");
		// By default deactivate
		virtualDestination.setValue(false);
		UIS.setTooltip(virtualDestination,
				"Do not forward anything to the final node: validate each study and email the conformance report only. "
						+ "The DICOM is routed to devnull and the delivery options (host/port or URL, transfer syntax, "
						+ "notification, …) are disabled.");
	}

	private void buildBuildConformanceReport() {
		buildConformanceReport = new Checkbox("Build DICOM conformance report");
		// By default deactivate
		buildConformanceReport.setValue(false);
		UIS.setTooltip(buildConformanceReport,
				"Validate each study sent to this destination against the DICOM standard and email a conformance report");
	}

	private void buildConformanceReportNotify() {
		conformanceReportNotify = new TextField("Conformance report: list of emails");
		conformanceReportNotify.setWidth("100%");
		conformanceReportNotify.setHelperText("Leave empty to reuse the notification emails");
		UIS.setTooltip(conformanceReportNotify,
				"Comma separated list of emails the conformance report is sent to. When empty, the notification email list is used.");
	}

	private void buildCheckValueConformity() {
		checkValueConformity = new Checkbox("Check value content conformity (VR rules)");
		// By default deactivate: real-world data often deviates from VR length/format
		// rules
		checkValueConformity.setValue(false);
		UIS.setTooltip(checkValueConformity,
				"Also report values that violate their VR length or format rules (PS3.5), e.g. an over-long string or a malformed date");
	}

	private void buildDeepSequenceValidation() {
		deepSequenceValidation = new Checkbox("Deep sequence validation (SR, functional groups)");
		// By default deactivate: deeper recursion enlarges the in-memory snapshot
		deepSequenceValidation.setValue(false);
		UIS.setTooltip(deepSequenceValidation,
				"Recurse the conformance checks through every sequence level (e.g. the SR content tree or enhanced multiframe functional groups) instead of only the first one");
	}

	private void buildOptionsDiv() {
		optionsDiv = new Div();
		// By default hide
		optionsDiv.setVisible(false);
		optionsDiv.setWidthFull();
	}

	private void buildListeners() {
		buildConformanceReport.addValueChangeListener(
				event -> optionsDiv.setVisible(isVirtual() || Boolean.TRUE.equals(buildConformanceReport.getValue())));
		virtualDestination.addValueChangeListener(event -> applyVirtualReportOnly());
	}

	/**
	 * A virtual destination is report-only, so the conformance report is mandatory: force
	 * it on, show its options, and prevent toggling it off while virtual is selected.
	 */
	private void applyVirtualReportOnly() {
		boolean virtual = isVirtual();
		if (virtual) {
			buildConformanceReport.setValue(true);
		}
		buildConformanceReport.setReadOnly(virtual);
		optionsDiv.setVisible(virtual || Boolean.TRUE.equals(buildConformanceReport.getValue()));
	}

	private boolean isVirtual() {
		return Boolean.TRUE.equals(virtualDestination.getValue());
	}

	private void addComponents() {
		optionsDiv
			.add(UIS.setWidthFull(new VerticalLayout(UIS.setWidthFull(new HorizontalLayout(conformanceReportNotify)),
					UIS.setWidthFull(new HorizontalLayout(checkValueConformity)),
					UIS.setWidthFull(new HorizontalLayout(deepSequenceValidation)))));
		add(UIS.setWidthFull(new HorizontalLayout(virtualDestination)),
				UIS.setWidthFull(new HorizontalLayout(buildConformanceReport)), optionsDiv);
	}

	/**
	 * Init binder for the component
	 * @param binder Binder
	 */
	public void init(Binder<DestinationEntity> binder) {
		binder.forField(getVirtualDestination())
			.bind(DestinationEntity::isVirtualDestination, DestinationEntity::setVirtualDestination);

		binder.forField(getBuildConformanceReport())
			.bind(DestinationEntity::isBuildConformanceReport, DestinationEntity::setBuildConformanceReport);

		binder.forField(getConformanceReportNotify())
			.bind(DestinationEntity::getConformanceReportNotify, DestinationEntity::setConformanceReportNotify);

		binder.forField(getCheckValueConformity())
			.bind(DestinationEntity::isCheckValueConformity, DestinationEntity::setCheckValueConformity);

		binder.forField(getDeepSequenceValidation())
			.bind(DestinationEntity::isDeepSequenceValidation, DestinationEntity::setDeepSequenceValidation);
	}

}
