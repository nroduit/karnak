/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom;

import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.function.Consumer;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.model.dicom.result.DicomEchoResult;
import org.karnak.backend.model.dicom.result.DicomNodeCheckResult;
import org.karnak.backend.model.dicom.result.NetworkCheckResult;
import org.weasis.core.util.StringUtil;

/**
 * Grid presenting one {@link DicomNodeCheckResult} per row: the node identity, success /
 * error badges for the DICOM echo and the network check, the measured connection and
 * execution durations, and an expandable details row with the DICOM and network messages.
 */
@NullUnmarked
public class DicomNodeCheckResultGrid extends Grid<DicomNodeCheckResult> {

	private Consumer<DicomNodeCheckResult> capabilityProbeAction;

	public DicomNodeCheckResultGrid() {
		super(DicomNodeCheckResult.class, false);

		init();
	}

	/**
	 * Enables an on-demand "Capabilities" action column. The given callback is invoked
	 * with the row's result when its button is pressed; without it the column is not
	 * shown.
	 */
	public void setCapabilityProbeAction(Consumer<DicomNodeCheckResult> action) {
		this.capabilityProbeAction = action;
		addColumn(new ComponentRenderer<>(this::createCapabilityButton)).setHeader("Capabilities")
			.setAutoWidth(true)
			.setFlexGrow(0);
	}

	private Button createCapabilityButton(DicomNodeCheckResult result) {
		Button button = new Button("Probe", (event) -> capabilityProbeAction.accept(result));
		button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
		return button;
	}

	private void init() {
		// Display details on row click
		setDetailsVisibleOnClick(true);
		setItemDetailsRenderer(createItemDetailsRenderer());

		// Empty grid case
		setEmptyStateText("No results found");

		// Selection mode
		setSelectionMode(SelectionMode.NONE);

		// Styling grid
		addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.WRAP_CELL_CONTENT);

		addColumns();
	}

	private void addColumns() {
		addColumn(createDicomNodeRenderer()).setHeader("Dicom Node");
		addColumn(createDicomStatusRenderer()).setHeader("Dicom Echo");
		addColumn(createConnectionRenderer()).setHeader("Connection Time (ms)");
		addColumn(createExecutionRenderer()).setHeader("Execution Time (ms)");
		addColumn(createNetworkStatusRenderer()).setHeader("Check Network");
	}

	private static ComponentRenderer<Div, DicomNodeCheckResult> createDicomNodeRenderer() {
		return new ComponentRenderer<>((dicomNodeCheckResult) -> {
			Div div = new Div();

			if (dicomNodeCheckResult != null) {
				div.add(new Div(dicomNodeCheckResult.getCalledNodeDescription()));
				div.add(new Div(dicomNodeCheckResult.getCalledNodeNetworkDetails()));
			}

			return div;
		});
	}

	private static ComponentRenderer<Badge, DicomNodeCheckResult> createDicomStatusRenderer() {
		return new ComponentRenderer<>((dicomNodeCheckResult) -> {
			Badge badge = new Badge();

			if (dicomNodeCheckResult != null) {
				DicomEchoResult dicomEchoResult = dicomNodeCheckResult.getDicomEchoResult();
				if (dicomEchoResult != null) {
					initBadge(badge, dicomEchoResult.isSuccessful());
				}
			}

			return badge;
		});
	}

	private static ComponentRenderer<Badge, DicomNodeCheckResult> createNetworkStatusRenderer() {
		return new ComponentRenderer<>((dicomNodeCheckResult) -> {
			Badge badge = new Badge();

			if (dicomNodeCheckResult != null) {
				NetworkCheckResult networkCheckResult = dicomNodeCheckResult.getNetworkCheckResult();
				if (networkCheckResult != null) {
					initBadge(badge, networkCheckResult.isSuccessful());
				}
			}

			return badge;
		});
	}

	private static void initBadge(Badge badge, boolean isSuccessful) {
		badge.addThemeVariants(isSuccessful ? BadgeVariant.SUCCESS : BadgeVariant.ERROR);
		badge.setText(isSuccessful ? "Success" : "Error");
	}

	private static ComponentRenderer<Div, DicomNodeCheckResult> createConnectionRenderer() {
		return new ComponentRenderer<>((dicomNodeCheckResult) -> {
			Div div = new Div();

			if (dicomNodeCheckResult != null) {
				DicomEchoResult dicomEchoResult = dicomNodeCheckResult.getDicomEchoResult();
				if (dicomEchoResult != null) {
					Long connectionDurationInMs = dicomEchoResult.getConnectionDurationInMs();
					if (connectionDurationInMs != null) {
						div.setText(String.valueOf(connectionDurationInMs));
					}
				}
			}

			return div;
		});
	}

	private static ComponentRenderer<Div, DicomNodeCheckResult> createExecutionRenderer() {
		return new ComponentRenderer<>((dicomNodeCheckResult) -> {
			Div div = new Div();

			if (dicomNodeCheckResult != null) {
				DicomEchoResult dicomEchoResult = dicomNodeCheckResult.getDicomEchoResult();
				if (dicomEchoResult != null) {
					Long executionDurationInMs = dicomEchoResult.getExecutionDurationInMs();
					if (executionDurationInMs != null) {
						div.setText(String.valueOf(executionDurationInMs));
					}
				}
			}

			return div;
		});
	}

	private static ComponentRenderer<HorizontalLayout, DicomNodeCheckResult> createItemDetailsRenderer() {
		return new ComponentRenderer<>((dicomNodeCheckResult) -> {
			HorizontalLayout layout = new HorizontalLayout();
			layout.setWidthFull();
			layout.setMargin(false);
			layout.setPadding(false);
			layout.setSpacing(true);
			layout.getStyle().set("font-size", "var(--lumo-font-size-s)");

			if (dicomNodeCheckResult != null) {
				layout.add(createDicomStatusLayout(dicomNodeCheckResult.getDicomEchoResult()));
				layout.add(createNetworkStatusLayout(dicomNodeCheckResult.getNetworkCheckResult()));
			}

			return layout;
		});
	}

	private static VerticalLayout createDicomStatusLayout(DicomEchoResult dicomEchoResult) {
		VerticalLayout layout = detailsSection("DICOM Status");

		if (dicomEchoResult != null) {
			UnorderedList unorderedList = new UnorderedList();

			if (dicomEchoResult.isUnexpectedError()) {
				unorderedList.add(new ListItem("Unexpected error: " + dicomEchoResult.getUnexpectedErrorMessage()));
			}
			else if (dicomEchoResult.isRejected()) {
				unorderedList.add(new ListItem("Association rejected: " + dicomEchoResult.getRejectionReason()));
			}
			else if (dicomEchoResult.isVerificationUnsupported()) {
				unorderedList.add(new ListItem(dicomEchoResult.getVerificationUnsupportedMessage()));
				addIfPresent(unorderedList, "Peer implementation: ",
						dicomEchoResult.getRemoteImplementationVersionName());
				addIfPresent(unorderedList, "Peer class UID: ", dicomEchoResult.getRemoteImplementationClassUid());
			}
			else {
				unorderedList.add(new ListItem("Status code: " + dicomEchoResult.getDicomStatusInHex()));
				addIfPresent(unorderedList, "Status message: ", dicomEchoResult.getDicomStatusMessage());
				addIfPresent(unorderedList, "Peer implementation: ",
						dicomEchoResult.getRemoteImplementationVersionName());
				addIfPresent(unorderedList, "Peer class UID: ", dicomEchoResult.getRemoteImplementationClassUid());
			}

			layout.add(unorderedList);
		}

		return layout;
	}

	private static void addIfPresent(UnorderedList list, String label, String value) {
		if (StringUtil.hasText(value)) {
			list.add(new ListItem(label + value));
		}
	}

	private static VerticalLayout createNetworkStatusLayout(NetworkCheckResult networkCheckResult) {
		VerticalLayout layout = detailsSection("Network Status");

		if (networkCheckResult != null) {
			UnorderedList unorderedList = new UnorderedList();
			unorderedList.add(new ListItem(networkCheckResult.getCheckHostnameMessage()));
			unorderedList.add(new ListItem(networkCheckResult.getCheckPortMessage()));
			addIfPresent(unorderedList, "", networkCheckResult.getCheckQualityMessage());

			layout.add(unorderedList);
		}

		return layout;
	}

	private static VerticalLayout detailsSection(String title) {
		VerticalLayout layout = new VerticalLayout();
		layout.setWidthFull();
		layout.setMargin(false);
		layout.setPadding(false);
		layout.setSpacing(false);

		H6 header = new H6(title);
		header.getStyle().set("margin-top", "0px");
		layout.add(header);

		return layout;
	}

}