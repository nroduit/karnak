/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.RolesAllowed;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.frontend.MainLayout;
import org.karnak.frontend.component.ConfirmDialog;
import org.karnak.frontend.forwardnode.component.GridForwardNode;
import org.karnak.frontend.forwardnode.component.LayoutNewGridForwardNode;
import org.karnak.frontend.forwardnode.edit.LayoutEditForwardNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.weasis.core.util.annotations.Generated;

/**
 * Forward Node View
 */
@Route(value = ForwardNodeView.ROUTE, layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
@PageTitle("Karnak - Forward node")
@RolesAllowed("admin")
@Generated()
public class ForwardNodeView extends HorizontalLayout implements HasUrlParameter<String> {

	public static final String VIEW_NAME = "Gateway";

	public static final String ROUTE = "forwardnode";

	public static final String SAVE = "Save";

	public static final String DELETE = "Delete";

	// Forward Node Logic
	private final ForwardNodeLogic forwardNodeLogic;

	// UI components
	private final LayoutNewGridForwardNode layoutNewGridForwardNode;

	private final LayoutEditForwardNode layoutEditForwardNode;

	/**
	 * Autowired constructor.
	 * @param forwardNodeLogic Forward Node Logic used to call backend services and
	 * implement logic linked to the view
	 */
	@Autowired
	public ForwardNodeView(ForwardNodeLogic forwardNodeLogic, Environment environment) {

		// Bind the autowired service
		this.forwardNodeLogic = forwardNodeLogic;

		// Set the view in the service
		this.forwardNodeLogic.setForwardNodeView(this);

		// Build components
		this.layoutNewGridForwardNode = new LayoutNewGridForwardNode();
		this.layoutEditForwardNode = new LayoutEditForwardNode(forwardNodeLogic, environment);

		// Init components
		initComponents();

		// Create layout
		buildLayout();

		// Events
		// LayoutNewGridForwardNode
		addEventNewForwardNodeLayoutNewGrid();
		// LayoutEditForwardNode
		addEventCancelButtonLayoutEdit();
		addEventDeleteButtonLayoutEdit();
		addEventSaveButtonLayoutEdit();
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		ForwardNodeEntity currentForwardNodeEntity = null;
		forwardNodeLogic.refreshAll();
		if (parameter != null) {
			Long idForwardNode = forwardNodeLogic.enter(parameter);
			if (idForwardNode != null) {
				currentForwardNodeEntity = forwardNodeLogic.retrieveForwardNodeById(idForwardNode);
			}
		}
		layoutNewGridForwardNode.load(currentForwardNodeEntity);
		layoutEditForwardNode.load(currentForwardNodeEntity);
	}

	/**
	 * Init components
	 */
	private void initComponents() {
		GridForwardNode grid = layoutNewGridForwardNode.getGridForwardNode();
		grid.init(forwardNodeLogic, this::onForwardNodeSelected);
		// Insert the "Add group" button between the new-node form and the grid
		layoutNewGridForwardNode.addComponentAtIndex(1, grid.createAddGroupButton());
	}

	public GridForwardNode getGridForwardNode() {
		return layoutNewGridForwardNode.getGridForwardNode();
	}

	/**
	 * Selection navigation for the forward node tree. Replicates the previous grid
	 * selection listener: clearing the selection resets the Save/Delete button labels.
	 * @param value the selected forward node, or {@code null} when cleared
	 */
	private void onForwardNodeSelected(ForwardNodeEntity value) {
		if (value == null) {
			layoutEditForwardNode.getButtonForwardNodeSaveDeleteCancel().getSave().setText(SAVE);
			layoutEditForwardNode.getButtonForwardNodeSaveDeleteCancel().getDelete().setText(DELETE);
		}
		forwardNodeLogic.editForwardNode(value);
	}

	/**
	 * Create and add the layout of the view
	 */
	public void buildLayout() {
		setSizeFull();
		layoutNewGridForwardNode.setWidth("100%");
		layoutEditForwardNode.setWidth("100%");
		SplitLayout splitLayout = new SplitLayout(layoutNewGridForwardNode, layoutEditForwardNode);
		splitLayout.setSizeFull();
		// Draggable splitter between the node list (left) and the edit panel (right).
		splitLayout.setSplitterPosition(30);
		add(splitLayout);
	}

	/**
	 * Event when adding a forward node from the new-forward-node popup
	 */
	private void addEventNewForwardNodeLayoutNewGrid() {
		layoutNewGridForwardNode.getNewForwardNodeDialog().setOnConfirm(() -> {
			eventAddForwardNodeLayoutNewGrid(
					new ForwardNodeEntity(layoutNewGridForwardNode.getTextFieldNewAETitleForwardNode().getValue()));
			return true;
		});
	}

	/**
	 * Actions done when adding a forward node for the LayoutNewGridForwardNode
	 * @param forwardNodeEntity Forward node to add
	 */
	private void eventAddForwardNodeLayoutNewGrid(ForwardNodeEntity forwardNodeEntity) {
		forwardNodeLogic.addForwardNode(forwardNodeEntity);
		layoutNewGridForwardNode.getGridForwardNode().selectItem(forwardNodeEntity);
		forwardNodeLogic.editForwardNode(forwardNodeEntity);
	}

	/**
	 * Add event when click on cancel button in LayoutEditForwardNode
	 */
	private void addEventCancelButtonLayoutEdit() {
		layoutEditForwardNode.getButtonForwardNodeSaveDeleteCancel().getCancel().addClickListener(event -> {
			forwardNodeLogic.cancelForwardNode();
			// Case transfer is in progress reset labels
			layoutEditForwardNode.getButtonForwardNodeSaveDeleteCancel().getSave().setText(SAVE);
			layoutEditForwardNode.getButtonForwardNodeSaveDeleteCancel().getDelete().setText(DELETE);
		});
	}

	/**
	 * Add event when click on delete button in LayoutEditForwardNode
	 */
	private void addEventDeleteButtonLayoutEdit() {
		layoutEditForwardNode.getButtonForwardNodeSaveDeleteCancel().getDelete().addClickListener(event -> {
			if (layoutEditForwardNode.getCurrentForwardNodeEntity() != null) {
				ConfirmDialog dialog = new ConfirmDialog("Are you sure to delete the forward node "
						+ layoutEditForwardNode.getCurrentForwardNodeEntity().getFwdAeTitle() + " ?"
						+ "<br>It will also delete the related entries from the monitoring view.");
				dialog.addConfirmationListener(componentEvent -> {
					forwardNodeLogic.deleteForwardNode(layoutEditForwardNode.getCurrentForwardNodeEntity());
					forwardNodeLogic.refreshAll();
					forwardNodeLogic.cancelForwardNode();
				});
				dialog.open();
			}
		});
	}

	/**
	 * Add event when click on save button in LayoutEditForwardNode
	 */
	private void addEventSaveButtonLayoutEdit() {
		layoutEditForwardNode.getButtonForwardNodeSaveDeleteCancel().getSave().addClickListener(event -> {
			if (layoutEditForwardNode.getBinderForwardNode()
				.writeBeanIfValid(layoutEditForwardNode.getCurrentForwardNodeEntity())) {
				forwardNodeLogic.updateForwardNode(layoutEditForwardNode.getCurrentForwardNodeEntity());
				forwardNodeLogic.refreshItem(layoutEditForwardNode.getCurrentForwardNodeEntity());
				forwardNodeLogic.refreshAll();
				forwardNodeLogic.cancelForwardNode();
			}
		});
	}

}
