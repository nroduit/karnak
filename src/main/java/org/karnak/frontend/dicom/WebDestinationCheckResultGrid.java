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
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.model.dicom.result.TlsCertificateInfo;
import org.karnak.backend.model.dicom.result.WebDestinationCheckResult;
import org.karnak.backend.model.dicom.result.WebNodeCheckResult;

/**
 * Grid presenting one {@link WebNodeCheckResult} per row: the DICOMweb destination, a
 * reachability badge, the HTTP status of the probe and the TLS certificate state, with an
 * expandable details row.
 */
@NullUnmarked
public class WebDestinationCheckResultGrid extends Grid<WebNodeCheckResult> {

	public WebDestinationCheckResultGrid() {
		super(WebNodeCheckResult.class, false);

		setDetailsVisibleOnClick(true);
		setItemDetailsRenderer(createDetailsRenderer());
		setEmptyStateText("No DICOMweb destinations found");
		setSelectionMode(SelectionMode.NONE);
		addThemeVariants(GridVariant.WRAP_CELL_CONTENT);

		addColumn(createDestinationRenderer()).setHeader("Destination");
		addColumn(createStatusRenderer()).setHeader("Reachable");
		addColumn(createHttpRenderer()).setHeader("HTTP Status").setAutoWidth(true).setFlexGrow(0);
		addColumn(createTlsRenderer()).setHeader("TLS");
	}

	private static ComponentRenderer<Div, WebNodeCheckResult> createDestinationRenderer() {
		return new ComponentRenderer<>((row) -> {
			Div div = new Div();
			if (row != null) {
				div.add(new Div(row.node().description()));
				Div url = new Div(row.node().url());
				url.getStyle().set("font-size", "var(--aura-font-size-xs)");
				div.add(url);
			}
			return div;
		});
	}

	private static ComponentRenderer<Badge, WebNodeCheckResult> createStatusRenderer() {
		return new ComponentRenderer<>((row) -> {
			Badge badge = new Badge();
			if (row != null) {
				boolean successful = row.result().isSuccessful();
				badge.addThemeVariants(successful ? BadgeVariant.SUCCESS : BadgeVariant.ERROR);
				badge.setText(successful ? "Reachable" : "Unreachable");
			}
			return badge;
		});
	}

	private static ComponentRenderer<Div, WebNodeCheckResult> createHttpRenderer() {
		return new ComponentRenderer<>((row) -> {
			Div div = new Div();
			if (row != null && row.result().isHttpResponded()) {
				div.setText(String.valueOf(row.result().getHttpStatus()));
			}
			return div;
		});
	}

	private static ComponentRenderer<Div, WebNodeCheckResult> createTlsRenderer() {
		return new ComponentRenderer<>((row) -> {
			Div div = new Div();
			if (row != null) {
				WebDestinationCheckResult result = row.result();
				if (!result.isSecure()) {
					div.setText("plain HTTP");
				}
				else {
					TlsCertificateInfo tls = result.getTls();
					div.setText(tls != null
							? tls.protocol() + " — " + (tls.expired() ? "EXPIRED" : tls.daysUntilExpiry() + "d left")
							: "handshake failed");
				}
			}
			return div;
		});
	}

	private static ComponentRenderer<UnorderedList, WebNodeCheckResult> createDetailsRenderer() {
		return new ComponentRenderer<>((row) -> {
			UnorderedList list = new UnorderedList();
			list.getStyle().set("font-size", "var(--aura-font-size-s)");

			if (row != null) {
				WebDestinationCheckResult result = row.result();

				if (result.isUnexpectedError()) {
					list.add(new ListItem("Error: " + result.getUnexpectedErrorMessage()));
					return list;
				}

				String endpoint = result.getHost() + ":" + result.getPort();
				list.add(new ListItem(result.isTcpReachable() ? "TCP connection to " + endpoint + " succeeded"
						: "TCP connection to " + endpoint + " failed"));

				if (result.isHttpResponded()) {
					list.add(new ListItem("HTTP OPTIONS returned status " + result.getHttpStatus()));
				}
				else if (result.isTcpReachable()) {
					list.add(new ListItem("No HTTP response from the endpoint"));
				}

				if (result.isSecure()) {
					TlsCertificateInfo tls = result.getTls();
					list.add(new ListItem(tls != null ? "TLS: " + tls.getSummary() : "TLS handshake failed"));
				}

				if (result.getAuth() != null) {
					list.add(new ListItem(result.getAuth().getSummary()));
				}

				result.getServiceProbes().forEach((probe) -> list.add(new ListItem(probe.getSummary())));
			}

			return list;
		});
	}

}
