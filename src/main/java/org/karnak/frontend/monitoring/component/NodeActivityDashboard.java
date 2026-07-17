/*
 * Copyright (c) 2022-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.monitoring.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.List;
import java.util.function.Supplier;
import org.karnak.backend.model.monitoring.NodeActivity;
import org.karnak.frontend.monitoring.MonitoringLogic;
import org.weasis.core.util.annotations.Generated;

/**
 * Forward-node activity dashboard: KPI cards with the totals over the selected period
 * plus a per-forward-node table (studies, series, instances, retries, sent, errors,
 * excluded, and de-identification / tag-morphing volume). Dependency-free — cards are
 * styled {@code Div}s.
 */
@Generated()
public class NodeActivityDashboard extends VerticalLayout {

	private final transient MonitoringLogic monitoringLogic;

	private final transient Supplier<TransferStatusFilter> filterSupplier;

	private final HorizontalLayout cards = new HorizontalLayout();

	private final Grid<NodeActivity> grid = new Grid<>(NodeActivity.class, false);

	public NodeActivityDashboard(MonitoringLogic monitoringLogic, Supplier<TransferStatusFilter> filterSupplier) {
		this.monitoringLogic = monitoringLogic;
		this.filterSupplier = filterSupplier;

		cards.setWidthFull();
		cards.getStyle().set("flex-wrap", "wrap");

		grid.addColumn(NodeActivity::forwardAet).setHeader("Forward AETitle").setSortable(true).setFlexGrow(20);
		grid.addColumn(NodeActivity::studies).setHeader("Studies").setSortable(true);
		grid.addColumn(NodeActivity::series).setHeader("Series").setSortable(true);
		grid.addColumn(NodeActivity::instances).setHeader("Instances").setSortable(true);
		grid.addColumn(NodeActivity::retries).setHeader("Retries").setSortable(true);
		grid.addColumn(NodeActivity::sent).setHeader("Sent").setSortable(true);
		grid.addColumn(NodeActivity::errors).setHeader("Errors").setSortable(true);
		grid.addColumn(NodeActivity::excluded).setHeader("Excluded").setSortable(true);
		grid.addColumn(NodeActivity::deidentified).setHeader("De-identified").setSortable(true);
		grid.addColumn(NodeActivity::tagMorphed).setHeader("Tag-morphed").setSortable(true);
		grid.setWidthFull();

		add(cards, grid);
		setSizeFull();
	}

	/** Recompute the dashboard for the current filter range. */
	public void refresh() {
		List<NodeActivity> nodes = monitoringLogic.listNodeActivity(filterSupplier.get());
		grid.setItems(nodes);

		cards.removeAll();
		cards.add(card("Studies", sum(nodes, NodeActivity::studies), false),
				card("Series", sum(nodes, NodeActivity::series), false),
				card("Instances", sum(nodes, NodeActivity::instances), false),
				card("Retries", sum(nodes, NodeActivity::retries), false),
				card("Sent", sum(nodes, NodeActivity::sent), false),
				card("Errors", sum(nodes, NodeActivity::errors), true),
				card("Excluded", sum(nodes, NodeActivity::excluded), false),
				card("De-identified", sum(nodes, NodeActivity::deidentified), false),
				card("Tag-morphed", sum(nodes, NodeActivity::tagMorphed), false));
	}

	private long sum(List<NodeActivity> nodes, java.util.function.ToLongFunction<NodeActivity> extractor) {
		return nodes.stream().mapToLong(extractor).sum();
	}

	private Component card(String label, long value, boolean errorEmphasis) {
		Span number = new Span(Long.toString(value));
		number.getStyle().set("font-size", "var(--aura-font-size-xl)").set("font-weight", "700");
		if (errorEmphasis && value > 0) {
			number.addClassName("karnak-error-text");
		}
		Span caption = new Span(label);
		caption.getStyle()
			.set("color", "var(--vaadin-text-color-secondary)")
			.set("font-size", "var(--aura-font-size-s)");

		Div card = new Div(number, caption);
		card.getStyle()
			.set("display", "flex")
			.set("flex-direction", "column")
			.set("min-width", "120px")
			.set("padding", "var(--vaadin-gap-m)")
			.set("margin", "var(--vaadin-gap-xs)")
			.set("border", "1px solid color-mix(in srgb, var(--vaadin-text-color) 10%, transparent)")
			.set("border-radius", "var(--vaadin-radius-l)");
		return card;
	}

}
