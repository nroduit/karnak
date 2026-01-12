/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.kheops;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.ListDataProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.WeakHashMap;
import org.karnak.backend.data.entity.KheopsAlbumsEntity;

public class GridSwitchingAlbums extends Grid<KheopsAlbumsEntity> {

	private final Binder<KheopsAlbumsEntity> binder;

	private final ListDataProvider<KheopsAlbumsEntity> dataProvider;

	private final Collection<Button> editButtons;

	private Editor<KheopsAlbumsEntity> editor;

	public GridSwitchingAlbums() {
		setWidthFull();

		setItems(new ArrayList<>());
		dataProvider = (ListDataProvider<KheopsAlbumsEntity>) getDataProvider();

		TextFieldsBindSwitchingAlbum textFieldsBindSwitchingAlbum = new TextFieldsBindSwitchingAlbum();
		binder = textFieldsBindSwitchingAlbum.getBinder();
		TextField textUrlAPI = textFieldsBindSwitchingAlbum.getTextUrlAPI();
		TextField textAuthorizationDestination = textFieldsBindSwitchingAlbum.getTextAuthorizationDestination();
		TextField textAuthorizationSource = textFieldsBindSwitchingAlbum.getTextAuthorizationSource();
		TextField textCondition = textFieldsBindSwitchingAlbum.getTextCondition();
		editButtons = Collections.newSetFromMap(new WeakHashMap<>());

		addColumn(KheopsAlbumsEntity::getUrlAPI).setHeader("URL API")
			.setFlexGrow(15)
			.setSortable(true)
			.setEditorComponent(textUrlAPI);

		addColumn(KheopsAlbumsEntity::getAuthorizationDestination).setHeader("Token destination")
			.setFlexGrow(15)
			.setSortable(true)
			.setEditorComponent(textAuthorizationDestination);

		addColumn(KheopsAlbumsEntity::getAuthorizationSource).setHeader("Token source")
			.setFlexGrow(15)
			.setSortable(true)
			.setEditorComponent(textAuthorizationSource);

		addColumn(KheopsAlbumsEntity::getCondition).setHeader("Condition")
			.setFlexGrow(15)
			.setSortable(true)
			.setEditorComponent(textCondition);

		setEditorColumn();
	}

	private void setEditorColumn() {
		editor = getEditor();
		editor.setBinder(binder);
		editor.setBuffered(true);

		Column<KheopsAlbumsEntity> editorColumn = addComponentColumn(kheopsAlbums -> {
			Button edit = new Button("Edit");
			edit.addClickListener(e -> editor.editItem(kheopsAlbums));
			edit.setEnabled(!editor.isOpen());

			Button remove = new Button("Remove");
			remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
			remove.addClickListener(e -> {
				dataProvider.getItems().remove(kheopsAlbums);
				dataProvider.refreshAll();
			});
			remove.setEnabled(!editor.isOpen());

			editButtons.add(edit);
			editButtons.add(remove);
			return new Div(edit, remove);
		}).setFlexGrow(15);

		editor.addOpenListener(e -> editButtons.forEach(button -> button.setEnabled(!editor.isOpen())));
		editor.addCloseListener(e -> editButtons.forEach(button -> button.setEnabled(!editor.isOpen())));

		Button save = new Button("Validate");
		save.addClickListener(event -> {
			// Get the current edited Kheops album
			KheopsAlbumsEntity currentEditedKheopsAlbumsEntity = new KheopsAlbumsEntity();
			if (binder.writeBeanIfValid(currentEditedKheopsAlbumsEntity)) {
				// Save only if not already existing in table
				if (!dataProvider.getItems().contains(currentEditedKheopsAlbumsEntity)) {
					editor.save();
					setItems(dataProvider);
				}
				else {
					// Show a notification
					Span content = new Span("Already existing");
					content.getStyle().set("color", "var(--lumo-error-text-color)");
					Notification notification = new Notification(content);
					notification.setDuration(3000);
					notification.setPosition(Position.BOTTOM_END);
					notification.open();
				}
			}
		});
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		Button cancel = new Button("Cancel", e -> editor.cancel());

		Div buttons = new Div(save, cancel);
		editorColumn.setEditorComponent(buttons);
	}

	public void clearEditorEditButtons() {
		editButtons.clear();
		editor.cancel();
	}

	public KheopsAlbumsEntity getSelectedRow() {
		return asSingleSelect().getValue();
	}

	public void refresh(KheopsAlbumsEntity data) {
		getDataCommunicator().refresh(data);
	}

}
