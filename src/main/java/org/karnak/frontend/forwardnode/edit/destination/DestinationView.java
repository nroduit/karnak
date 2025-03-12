/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.forwardnode.edit.destination;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.karnak.backend.data.entity.ForwardNodeEntity;
import org.karnak.frontend.forwardnode.ForwardNodeLogic;
import org.karnak.frontend.forwardnode.edit.component.ButtonSaveDeleteCancel;
import org.karnak.frontend.forwardnode.edit.destination.component.GridDestination;
import org.karnak.frontend.forwardnode.edit.destination.component.NewUpdateDestination;
import org.karnak.frontend.util.UIS;

/**
 * Destination View
 */
@SuppressWarnings("serial")
public class DestinationView extends VerticalLayout {

	// Destination Logic
	private final DestinationLogic destinationLogic;

	private final ForwardNodeLogic forwardNodeLogic;

	// UI components
	private UI ui;

	private TextField filter;

	private Button newDestinationDICOM;

	private Button newDestinationSTOW;

	private GridDestination gridDestination;

	private HorizontalLayout layoutFilterButton;

	private ButtonSaveDeleteCancel buttonForwardNodeSaveDeleteCancel;

	private NewUpdateDestination newUpdateDestination;

	private static final String LABEL_NEW_DESTINATION_DICOM = "DICOM";

	private static final String LABEL_NEW_DESTINATION_STOW = "STOW";

	private static final String PLACEHOLDER_FILTER = "Filter properties of destination";

	/**
	 * Destination view constructor
	 * @param forwardNodeLogic Logic service of the view
	 */
	public DestinationView(final ForwardNodeLogic forwardNodeLogic) {

		// Bind the autowired service
		this.destinationLogic = forwardNodeLogic.getDestinationLogic();
		this.forwardNodeLogic = forwardNodeLogic;

		// Set the view in the service
		this.destinationLogic.setDestinationsView(this);

		// Keep the UI in order to be processed in the check of activity
		ui = UI.getCurrent();

		// Create components and layout
		buildComponentsLayout();
	}

	/**
	 * Create components, layout and add the layout of the view
	 */
	private void buildComponentsLayout() {
		setSizeFull();
		filter = new TextField();
		newDestinationDICOM = new Button(LABEL_NEW_DESTINATION_DICOM);
		newDestinationSTOW = new Button(LABEL_NEW_DESTINATION_STOW);
		gridDestination = new GridDestination();

		setTextFieldFilter();
		setButtonNewDestinationDICOM();
		setButtonNewDestinationSTOW();
		loadForwardNode(null);

		layoutFilterButton = new HorizontalLayout(filter, newDestinationDICOM, newDestinationSTOW);
		layoutFilterButton.setVerticalComponentAlignment(Alignment.START, filter);
		layoutFilterButton.expand(filter);

		add(UIS.setWidthFull(layoutFilterButton), UIS.setWidthFull(gridDestination));
	}

	private void setTextFieldFilter() {
		filter.setPlaceholder(PLACEHOLDER_FILTER);
		// Apply the filter to grid's data provider. TextField value is never null
		filter.addValueChangeListener(event -> destinationLogic.setFilter(event.getValue()));
	}

	private void setButtonNewDestinationDICOM() {
		newDestinationDICOM.getElement().setAttribute("title", "New destination of type dicom");
		newDestinationDICOM.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		newDestinationDICOM.setIcon(VaadinIcon.PLUS_CIRCLE.create());
		// newDestinationDICOM.addClickListener(click ->
		// destinationLogic.newDestinationDicom());
	}

	private void setButtonNewDestinationSTOW() {
		newDestinationSTOW.getElement().setAttribute("title", "New destination of type stow");
		newDestinationSTOW.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		newDestinationSTOW.setIcon(VaadinIcon.PLUS_CIRCLE.create());
		// newDestinationStow.addClickListener(click ->
		// destinationLogic.newDestinationStow());
	}

	@Override
	public void setEnabled(boolean enabled) {
		filter.setEnabled(enabled);
		newDestinationDICOM.setEnabled(enabled);
		newDestinationSTOW.setEnabled(enabled);
		gridDestination.setEnabled(enabled);
	}

	public void loadForwardNode(ForwardNodeEntity forwardNodeEntity) {
		ForwardNodeEntity forwardNodeEntityReload = null;
		if (forwardNodeEntity != null) {
			forwardNodeEntityReload = forwardNodeLogic.retrieveForwardNodeById(forwardNodeEntity.getId());
			setEnabled(forwardNodeEntityReload != null);
		}
		destinationLogic.loadForwardNode(forwardNodeEntityReload);
		gridDestination.setItems(destinationLogic);
		setEnabled(forwardNodeEntity != null);
	}

	public Button getNewDestinationDICOM() {
		return newDestinationDICOM;
	}

	public Button getNewDestinationSTOW() {
		return newDestinationSTOW;
	}

	public GridDestination getGridDestination() {
		return gridDestination;
	}

	public DestinationLogic getDestinationLogic() {
		return destinationLogic;
	}

	public UI getUi() {
		return ui;
	}

	public void setUi(UI ui) {
		this.ui = ui;
	}

	public ButtonSaveDeleteCancel getButtonForwardNodeSaveDeleteCancel() {
		return buttonForwardNodeSaveDeleteCancel;
	}

	public void setButtonForwardNodeSaveDeleteCancel(ButtonSaveDeleteCancel buttonForwardNodeSaveDeleteCancel) {
		this.buttonForwardNodeSaveDeleteCancel = buttonForwardNodeSaveDeleteCancel;
	}

	public NewUpdateDestination getNewUpdateDestination() {
		return newUpdateDestination;
	}

	public void setNewUpdateDestination(NewUpdateDestination newUpdateDestination) {
		this.newUpdateDestination = newUpdateDestination;
	}

}
