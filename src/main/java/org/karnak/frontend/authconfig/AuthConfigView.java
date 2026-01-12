/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.authconfig;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.karnak.backend.data.entity.AuthConfigEntity;
import org.karnak.backend.enums.AuthConfigType;
import org.karnak.frontend.MainLayout;
import org.karnak.frontend.authconfig.component.AuthConfigComponent;
import org.karnak.frontend.authconfig.component.NewAuthConfigComponent;
import org.karnak.frontend.component.WarningConfirmDialog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;

@Route(value = AuthConfigView.ROUTE, layout = MainLayout.class)
@PageTitle("Karnak - Authentication Config")
@Tag("auth-config-view")
@Secured({ "ROLE_admin" })
public class AuthConfigView extends HorizontalLayout {

	public static final String VIEW_NAME = "Authentication Config";

	public static final String ROUTE = "auth-config";

	private final AuthConfigLogic authConfigLogic;

	private final AuthConfigComponent authConfigComponent;

	private final Grid<AuthConfigEntity> authConfigGrid;

	private VerticalLayout barAndGridLayout;

	private NewAuthConfigComponent newAuthConfigComponent;

	@Autowired
	public AuthConfigView(AuthConfigLogic authConfigLogic) {

		this.authConfigLogic = authConfigLogic;

		authConfigGrid = new Grid<>();
		authConfigComponent = new AuthConfigComponent();
		authConfigComponent.setWidth("72%");

		initComponents();
		buildLayout();

		addEventGridSelection();
		addEventDeleteAuthConfig();
		addEventSaveAuthConfig();
		addEventCreateAuthConfig();
		addEventCancelAuthConfig();

		setAlignItems(Alignment.STRETCH);
		this.getStyle().setDisplay(Style.Display.FLEX);
		add(barAndGridLayout, authConfigComponent);
	}

	private void initComponents() {
		authConfigGrid.setItems(authConfigLogic.getItems());
		authConfigGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
		authConfigGrid.addColumn(a -> a.getAuthConfigType().getCode()).setHeader("Type");
		authConfigGrid.addColumn(AuthConfigEntity::getCode).setHeader("Identifier");

		newAuthConfigComponent = new NewAuthConfigComponent();
	}

	private void buildLayout() {
		setSizeFull();

		barAndGridLayout = new VerticalLayout();
		barAndGridLayout.add(newAuthConfigComponent);
		barAndGridLayout.add(authConfigGrid);
		barAndGridLayout.setWidth("25%");

	}

	private void addEventGridSelection() {
		authConfigGrid.asSingleSelect().addValueChangeListener(event -> {
			if (event.getValue() != null) {
				authConfigComponent.displayData(authConfigLogic.retrieveAuthConfig(event.getValue().getCode()));
			}
		});
	}

	private void addEventDeleteAuthConfig() {
		authConfigComponent.getDeleteBtn().addClickListener(buttonClickEvent -> {
			Div dialogContent = new Div();
			dialogContent
				.add(new Text("Are you sure you want to delete the entry " + authConfigComponent.getAuthConfigCode()
						+ "? Please make sure it is not used anywhere since it can result in errors."));
			WarningConfirmDialog dialog = new WarningConfirmDialog(dialogContent);
			dialog.addConfirmationListener(componentEvent -> {
				authConfigLogic.deleteAuthConfig(authConfigComponent.getAuthConfigCode());
				authConfigLogic.refreshAll();
				authConfigGrid.setItems(authConfigLogic.getItems());
				authConfigComponent.cancel();
			});
			dialog.open();
		});
	}

	private void addEventCreateAuthConfig() {
		newAuthConfigComponent.getSaveAuthConfig().addClickListener(buttonClickEvent -> validateIdentifier());
		newAuthConfigComponent.getNewNameField().addKeyDownListener(Key.ENTER, keyDownEvent -> validateIdentifier());
	}

	private void validateIdentifier() {
		String name = newAuthConfigComponent.getNewNameField().getValue();
		if (name == null || name.isEmpty()) {
			newAuthConfigComponent.getNewNameField().setInvalid(true);
			newAuthConfigComponent.getNewNameField().setErrorMessage("Identifier is required");
		}
		else if (authConfigLogic.contains(name)) {
			newAuthConfigComponent.getNewNameField().setInvalid(true);
			newAuthConfigComponent.getNewNameField().setErrorMessage("This identifier already exists");
		}
		else {
			newAuthConfigComponent.getNewNameField().setInvalid(false);
			authConfigComponent.displayEmptyForm(name);
			newAuthConfigComponent.resetNewAuthConfigComponent();
		}
	}

	private void addEventSaveAuthConfig() {
		authConfigComponent.getSaveBtn().addClickListener(buttonClickEvent -> {
			if (authConfigComponent.isValid()) {
				AuthConfigEntity newEntity = authConfigComponent.getData();
				authConfigLogic.createAuthConfig(authConfigComponent.getAuthConfigCode(), AuthConfigType.OAUTH2,
						newEntity);
				authConfigLogic.refreshAll();
				authConfigGrid.setItems(authConfigLogic.getItems());
				authConfigGrid.select(newEntity);
			}
		});
	}

	private void addEventCancelAuthConfig() {
		authConfigComponent.getCancelBtn().addClickListener(buttonClickEvent -> {
			authConfigComponent.cancel();
			authConfigGrid.deselectAll();
		});
	}

}
