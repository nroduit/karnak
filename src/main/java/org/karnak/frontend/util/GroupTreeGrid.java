/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.util;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.function.SerializablePredicate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.NamedGroupEntity;
import org.karnak.frontend.component.ConfirmDialog;
import org.karnak.frontend.util.GroupTreeNode.GroupNode;
import org.karnak.frontend.util.GroupTreeNode.ItemNode;

/**
 * A {@link TreeGrid} that shows feature items with an optional one-level grouping: group
 * header rows can be expanded to reveal their items, while ungrouped items stay at the
 * root exactly as in a flat list.
 *
 * <p>
 * The grid is feature-agnostic: subclasses define their own columns via the
 * {@code addPrimaryColumn}/{@code addItemTextColumn} helpers, and the view wires the data
 * and persistence through {@link #init(GroupTreeController, SerializableConsumer)}.
 * Assignment to a group is done by drag &amp; drop (drop an item onto a group, or use the
 * row context menu to remove it from its group). Groups can be created, renamed and
 * deleted from the UI; deleting a group returns its members to the root.
 *
 * @param <T> the feature item type
 */
@NullUnmarked
public abstract class GroupTreeGrid<T> extends TreeGrid<GroupTreeNode<T>> {

	private transient GroupTreeController<T> controller;

	private transient SerializableConsumer<T> navigationListener;

	private transient SerializablePredicate<T> itemFilter = item -> true;

	private final transient Map<String, GroupTreeNode<T>> nodesByKey = new HashMap<>();

	private transient Map<Long, List<GroupTreeNode<T>>> childrenByGroupId = new HashMap<>();

	private transient ItemNode<T> draggedItem;

	private boolean suppressSelection;

	protected GroupTreeGrid() {
		setSelectionMode(SelectionMode.SINGLE);
		setupDragAndDrop();
		setupContextMenu();
		asSingleSelect().addValueChangeListener(event -> {
			if (suppressSelection || navigationListener == null) {
				return;
			}
			GroupTreeNode<T> value = event.getValue();
			if (value == null) {
				navigationListener.accept(null);
			}
			else if (value instanceof ItemNode<T> itemNode) {
				navigationListener.accept(itemNode.item());
			}
			// Selecting a group header row only expands/collapses it: ignore.
		});
	}

	/**
	 * Wire the data source and the item-selection navigation callback, then load the
	 * data.
	 * @param controller bridge to the backend for this feature
	 * @param navigationListener invoked with the selected item (or {@code null} when the
	 * selection is cleared)
	 */
	public void init(GroupTreeController<T> controller, SerializableConsumer<T> navigationListener) {
		this.controller = controller;
		this.navigationListener = navigationListener;
		reload();
	}

	/** Rebuild the whole tree from the controller. Selection events are suppressed. */
	public void reload() {
		if (controller == null) {
			return;
		}
		suppressSelection = true;
		try {
			nodesByKey.clear();
			childrenByGroupId = new HashMap<>();
			List<GroupTreeNode<T>> roots = new ArrayList<>();

			for (NamedGroupEntity group : controller.listGroups()) {
				GroupNode<T> groupNode = new GroupNode<>(group);
				childrenByGroupId.put(group.getId(), new ArrayList<>());
				nodesByKey.put(groupNode.key(), groupNode);
				roots.add(groupNode);
			}

			for (T item : controller.listItems()) {
				if (!itemFilter.test(item)) {
					continue;
				}
				ItemNode<T> itemNode = new ItemNode<>(item, controller.itemId(item));
				nodesByKey.put(itemNode.key(), itemNode);
				NamedGroupEntity group = controller.groupOf(item);
				if (group != null && group.getId() != null && childrenByGroupId.containsKey(group.getId())) {
					childrenByGroupId.get(group.getId()).add(itemNode);
				}
				else {
					roots.add(itemNode);
				}
			}

			setItems(roots, node -> node instanceof GroupNode<?> groupNode
					? childrenByGroupId.getOrDefault(groupNode.group().getId(), List.of()) : List.of());
		}
		finally {
			suppressSelection = false;
		}
	}

	/**
	 * Restrict the items shown by the grid (e.g. a text filter). Rebuilds the tree.
	 * @param filter predicate kept when it returns {@code true}; {@code null} shows all
	 */
	public void setItemFilter(SerializablePredicate<T> filter) {
		this.itemFilter = filter != null ? filter : item -> true;
		reload();
	}

	/** Select the row of the given item (expanding its group), without navigating. */
	public void selectItem(T item) {
		suppressSelection = true;
		try {
			if (item == null || controller == null) {
				deselectAll();
				return;
			}
			GroupTreeNode<T> node = nodesByKey.get("i:" + controller.itemId(item));
			if (node == null) {
				deselectAll();
				return;
			}
			NamedGroupEntity group = controller.groupOf(item);
			if (group != null) {
				GroupTreeNode<T> groupNode = nodesByKey.get("g:" + group.getId());
				if (groupNode != null) {
					expand(groupNode);
				}
			}
			select(node);
		}
		finally {
			suppressSelection = false;
		}
	}

	/** A ready-made "Add group" button that views can place in their toolbar. */
	public Button createAddGroupButton() {
		Button button = new Button("Add group", VaadinIcon.FOLDER_ADD.create());
		button.addThemeVariants(ButtonVariant.TERTIARY);
		button.addClickListener(
				event -> promptName("New group", "", name -> runAndReload(() -> controller.createGroup(name))));
		return button;
	}

	// ---------------------------------------------------------------------------------
	// Column helpers for subclasses
	// ---------------------------------------------------------------------------------

	/**
	 * Add the main hierarchy column (with the expand toggle). Group rows render a folder
	 * header; item rows render the supplied component.
	 */
	protected Grid.Column<GroupTreeNode<T>> addPrimaryColumn(String header,
			SerializableFunction<T, Component> itemRenderer, Comparator<T> itemComparator) {
		return addComponentHierarchyColumn(node -> renderPrimary(node, itemRenderer)).setHeader(header)
			.setSortable(true)
			.setComparator(nodeComparator(itemComparator));
	}

	/**
	 * Add a text column that is blank for group rows and shows {@code value} for items.
	 */
	protected Grid.Column<GroupTreeNode<T>> addItemTextColumn(String header, SerializableFunction<T, String> value,
			Comparator<T> itemComparator) {
		return addColumn(node -> node instanceof ItemNode<T> itemNode ? value.apply(itemNode.item()) : "")
			.setHeader(header)
			.setSortable(true)
			.setComparator(nodeComparator(itemComparator));
	}

	/**
	 * Build a node comparator: groups first (by name), then items by
	 * {@code itemComparator}.
	 */
	protected Comparator<GroupTreeNode<T>> nodeComparator(Comparator<T> itemComparator) {
		return (a, b) -> {
			boolean aGroup = a.isGroup();
			boolean bGroup = b.isGroup();
			if (aGroup && bGroup) {
				return CollatorUtils.compare(groupName(a), groupName(b));
			}
			if (aGroup) {
				return -1;
			}
			if (bGroup) {
				return 1;
			}
			return itemComparator.compare(((ItemNode<T>) a).item(), ((ItemNode<T>) b).item());
		};
	}

	// ---------------------------------------------------------------------------------
	// Internals
	// ---------------------------------------------------------------------------------

	private Component renderPrimary(GroupTreeNode<T> node, SerializableFunction<T, Component> itemRenderer) {
		if (node instanceof GroupNode<?> groupNode) {
			return groupHeader(groupNode.group());
		}
		return itemRenderer.apply(((ItemNode<T>) node).item());
	}

	private Component groupHeader(NamedGroupEntity group) {
		Icon icon = VaadinIcon.FOLDER_O.create();
		icon.setSize("var(--aura-font-size-l)");
		Span name = new Span(group.getName());
		name.getStyle().set("font-weight", "600");
		HorizontalLayout layout = new HorizontalLayout(icon, name);
		layout.setAlignItems(Alignment.CENTER);
		layout.setSpacing(true);
		return layout;
	}

	private String groupName(GroupTreeNode<T> node) {
		return ((GroupNode<?>) node).group().getName();
	}

	private void setupDragAndDrop() {
		setRowsDraggable(true);
		setDropMode(GridDropMode.ON_TOP);
		addDragStartListener(event -> {
			GroupTreeNode<T> dragged = event.getDraggedItems().isEmpty() ? null : event.getDraggedItems().get(0);
			// Only items are movable; groups cannot be nested.
			draggedItem = dragged instanceof ItemNode<T> itemNode ? itemNode : null;
		});
		addDragEndListener(event -> draggedItem = null);
		addDropListener(event -> {
			if (draggedItem == null) {
				return;
			}
			NamedGroupEntity targetGroup = resolveGroup(event.getDropTargetItem().orElse(null));
			T item = draggedItem.item();
			runAndReload(() -> controller.assign(item, targetGroup));
		});
	}

	private NamedGroupEntity resolveGroup(GroupTreeNode<T> target) {
		if (target instanceof GroupNode<?> groupNode) {
			return groupNode.group();
		}
		if (target instanceof ItemNode<T> itemNode) {
			// Dropping onto another item joins that item's group (or the root).
			return controller.groupOf(itemNode.item());
		}
		return null;
	}

	private void setupContextMenu() {
		GridContextMenu<GroupTreeNode<T>> menu = addContextMenu();
		menu.setDynamicContentHandler(node -> {
			menu.removeAll();
			if (node instanceof GroupNode<?> groupNode) {
				NamedGroupEntity group = groupNode.group();
				menu.addItem("Rename group", event -> promptName("Rename group", group.getName(),
						name -> runAndReload(() -> controller.renameGroup(group, name))));
				menu.addItem("Delete group", event -> confirmDeleteGroup(group));
				return true;
			}
			if (node instanceof ItemNode<T> itemNode && controller.groupOf(itemNode.item()) != null) {
				T item = itemNode.item();
				menu.addItem("Remove from group", event -> runAndReload(() -> controller.assign(item, null)));
				return true;
			}
			return false;
		});
	}

	private void confirmDeleteGroup(NamedGroupEntity group) {
		ConfirmDialog dialog = new ConfirmDialog("Delete group \"" + group.getName()
				+ "\"?<br>The items inside will move back to the root of the list.");
		dialog.addConfirmationListener(event -> runAndReload(() -> controller.deleteGroup(group)));
		dialog.open();
	}

	private void promptName(String title, String initial, SerializableConsumer<String> onSave) {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle(title);
		TextField field = new TextField("Group name");
		field.setValue(initial == null ? "" : initial);
		field.setWidthFull();
		Button save = new Button("Save", event -> {
			String value = field.getValue() == null ? "" : field.getValue().trim();
			if (!value.isEmpty()) {
				onSave.accept(value);
				dialog.close();
			}
		});
		save.addThemeVariants(ButtonVariant.PRIMARY);
		Button cancel = new Button("Cancel", event -> dialog.close());
		dialog.add(field);
		dialog.getFooter().add(cancel, save);
		dialog.open();
		field.focus();
	}

	private void runAndReload(Runnable action) {
		if (controller == null) {
			return;
		}
		action.run();
		reload();
	}

}
