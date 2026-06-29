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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.AbstractBackEndHierarchicalDataProvider;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalQuery;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.frontend.monitoring.MonitoringLogic;
import org.karnak.frontend.monitoring.component.MonitoringNode.DestinationNode;
import org.karnak.frontend.monitoring.component.MonitoringNode.ErrorNode;
import org.karnak.frontend.monitoring.component.MonitoringNode.SeriesNode;
import org.karnak.frontend.monitoring.component.MonitoringNode.StudyNode;
import org.weasis.core.util.annotations.Generated;

/**
 * Hierarchical monitoring view: Destination (prefixed by the forward node AE Title) /
 * Study / Series, with one error-reason line per failing series showing the number of
 * instances concerned. Each level is fetched lazily from {@link MonitoringLogic} when
 * expanded.
 */
@Generated()
@NullUnmarked
public class MonitoringTreeGrid extends TreeGrid<MonitoringNode> {

	private final transient MonitoringTreeDataProvider dataProvider;

	private transient Consumer<MonitoringNode> selectionListener;

	public MonitoringTreeGrid(MonitoringLogic monitoringLogic, Supplier<TransferStatusFilter> filterSupplier) {
		this.dataProvider = new MonitoringTreeDataProvider(monitoringLogic, filterSupplier);

		// The first column absorbs all extra width; the rest only take what they need.
		addComponentHierarchyColumn(this::nameComponent).setHeader("Destination / Study / Series").setFlexGrow(1);
		addColumn(node -> node instanceof DestinationNode d ? Long.toString(d.studies()) : "").setHeader("Studies")
			.setFlexGrow(0)
			.setAutoWidth(true);
		addColumn(this::seriesText).setHeader("Series").setFlexGrow(0).setAutoWidth(true);
		addColumn(this::instancesText).setHeader("Instances").setFlexGrow(0).setAutoWidth(true);
		addColumn(this::errorsText).setHeader("Errors").setFlexGrow(0).setAutoWidth(true);
		addComponentColumn(this::statusBadge).setHeader("Status").setFlexGrow(0).setAutoWidth(true);

		setSelectionMode(SelectionMode.SINGLE);
		asSingleSelect().addValueChangeListener(event -> {
			if (selectionListener != null) {
				selectionListener.accept(event.getValue());
			}
		});

		setDataProvider(dataProvider);
		setSizeFull();
	}

	/**
	 * Notified with the selected node (or {@code null} when the selection is cleared).
	 */
	public void setSelectionListener(Consumer<MonitoringNode> selectionListener) {
		this.selectionListener = selectionListener;
	}

	/** Reload the whole tree (after a filter change or a manual refresh). */
	public void refresh() {
		dataProvider.refreshAll();
	}

	/** Expand every destination/study/series branch that contains errors. */
	public void expandErrors() {
		List<MonitoringNode> toExpand = new ArrayList<>();
		collectErrorBranches(null, toExpand);
		expand(toExpand);
	}

	private void collectErrorBranches(MonitoringNode parent, List<MonitoringNode> accumulator) {
		for (MonitoringNode child : dataProvider.childrenOf(parent)) {
			if (child.hasErrors() && dataProvider.hasChildren(child)) {
				accumulator.add(child);
				collectErrorBranches(child, accumulator);
			}
		}
	}

	// --- rendering -------------------------------------------------------------------

	private Component nameComponent(MonitoringNode node) {
		VaadinIcon iconType = switch (node) {
			case DestinationNode ignored -> VaadinIcon.SERVER;
			case StudyNode ignored -> VaadinIcon.FOLDER_O;
			case SeriesNode ignored -> VaadinIcon.RECORDS;
			case ErrorNode ignored -> VaadinIcon.WARNING;
		};
		String text = switch (node) {
			case DestinationNode d -> d.displayName();
			case StudyNode s -> s.studyUid() + (isBlank(s.description()) ? "" : " — " + s.description());
			case SeriesNode se -> se.serieUid() + (isBlank(se.modality()) ? "" : " [" + se.modality() + "]");
			case ErrorNode e -> isBlank(e.reason()) ? "(no reason)" : e.reason();
		};
		Icon icon = iconType.create();
		icon.setSize("var(--lumo-icon-size-s)");
		Span label = new Span(text);
		if (node.hasErrors()) {
			label.getStyle().set("color", "var(--lumo-error-text-color)");
		}
		HorizontalLayout layout = new HorizontalLayout(icon, label);
		layout.setAlignItems(Alignment.CENTER);
		layout.setSpacing(true);
		return layout;
	}

	private String seriesText(MonitoringNode node) {
		return switch (node) {
			case DestinationNode d -> Long.toString(d.series());
			case StudyNode s -> Long.toString(s.series());
			default -> "";
		};
	}

	private String instancesText(MonitoringNode node) {
		return switch (node) {
			case DestinationNode d -> Long.toString(d.instances());
			case StudyNode s -> Long.toString(s.instances());
			case SeriesNode se -> Long.toString(se.instances());
			case ErrorNode e -> Long.toString(e.instances());
		};
	}

	private String errorsText(MonitoringNode node) {
		long errors = switch (node) {
			case DestinationNode d -> d.errors();
			case StudyNode s -> s.errors();
			case SeriesNode se -> se.errors();
			case ErrorNode ignored -> 0;
		};
		return errors > 0 ? Long.toString(errors) : "";
	}

	private Span statusBadge(MonitoringNode node) {
		return switch (node) {
			case ErrorNode ignored -> badge("error", true);
			case DestinationNode d -> statusBadge(d.errors());
			case StudyNode s -> statusBadge(s.errors());
			case SeriesNode se -> statusBadge(se.errors());
		};
	}

	private Span statusBadge(long errors) {
		return errors > 0 ? badge(errors + " error(s)", true) : badge("OK", false);
	}

	private Span badge(String text, boolean error) {
		Span span = new Span(text);
		span.getElement().getThemeList().add(error ? "badge error" : "badge success");
		return span;
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	/**
	 * Lazy hierarchical data provider: each level is one grouped aggregation query,
	 * cached per parent until the next {@link #refreshAll()} (so the count + fetch passes
	 * of a single expansion do not query twice).
	 */
	private static final class MonitoringTreeDataProvider
			extends AbstractBackEndHierarchicalDataProvider<MonitoringNode, Void> {

		private final transient MonitoringLogic logic;

		private final transient Supplier<TransferStatusFilter> filterSupplier;

		private final transient Map<String, List<MonitoringNode>> cache = new HashMap<>();

		MonitoringTreeDataProvider(MonitoringLogic logic, Supplier<TransferStatusFilter> filterSupplier) {
			this.logic = logic;
			this.filterSupplier = filterSupplier;
		}

		@Override
		public Object getId(MonitoringNode item) {
			return item.key();
		}

		@Override
		protected Stream<MonitoringNode> fetchChildrenFromBackEnd(HierarchicalQuery<MonitoringNode, Void> query) {
			return childrenOf(query.getParent()).stream().skip(query.getOffset()).limit(query.getLimit());
		}

		@Override
		public int getChildCount(HierarchicalQuery<MonitoringNode, Void> query) {
			return childrenOf(query.getParent()).size();
		}

		@Override
		public boolean hasChildren(MonitoringNode item) {
			return switch (item) {
				case DestinationNode ignored -> true;
				case StudyNode ignored -> true;
				case SeriesNode se -> se.errors() > 0;
				case ErrorNode ignored -> false;
			};
		}

		@Override
		public void refreshAll() {
			cache.clear();
			super.refreshAll();
		}

		List<MonitoringNode> childrenOf(MonitoringNode parent) {
			String key = parent == null ? "ROOT" : parent.key();
			return cache.computeIfAbsent(key, ignored -> loadChildren(parent));
		}

		private List<MonitoringNode> loadChildren(MonitoringNode parent) {
			TransferStatusFilter filter = filterSupplier.get();
			if (parent == null) {
				return logic.listDestinations(filter)
					.stream()
					.<MonitoringNode>map(d -> new DestinationNode(d.destinationId(), d.forwardAet(),
							d.destinationLabel(), d.studies(), d.series(), d.instances(), d.sent(), d.errors()))
					.toList();
			}
			return switch (parent) {
				case DestinationNode d -> logic.listStudies(filter, d.destinationId())
					.stream()
					.<MonitoringNode>map(s -> new StudyNode(d.destinationId(), s.studyUid(), s.studyUidToSend(),
							s.description(), s.descriptionToSend(), s.patientIdOriginal(), s.patientIdToSend(),
							s.accessionNumberOriginal(), s.accessionNumberToSend(), s.studyDateOriginal(),
							s.studyDateToSend(), s.series(), s.instances(), s.sent(), s.errors(), s.firstSeen(),
							s.lastSeen()))
					.toList();
				case StudyNode s -> logic.listSeries(filter, s.destinationId(), s.studyUid())
					.stream()
					.<MonitoringNode>map(se -> new SeriesNode(s.destinationId(), s.studyUid(), s.studyUidToSend(),
							s.patientIdOriginal(), s.patientIdToSend(), s.accessionNumberOriginal(),
							s.accessionNumberToSend(), s.description(), s.descriptionToSend(), s.studyDateOriginal(),
							s.studyDateToSend(), se.serieUid(), se.serieUidToSend(), se.serieDescription(),
							se.serieDescriptionToSend(), se.modality(), se.sopClassUids(), se.serieDateOriginal(),
							se.serieDateToSend(), se.instances(), se.sent(), se.errors(), se.firstSeen(),
							se.lastSeen()))
					.toList();
				case SeriesNode se -> logic.listErrors(filter, se.destinationId(), se.serieUid())
					.stream()
					.<MonitoringNode>map(er -> new ErrorNode(se.key(), er.reason(), er.instances()))
					.toList();
				case ErrorNode ignored -> List.of();
			};
		}

	}

}
