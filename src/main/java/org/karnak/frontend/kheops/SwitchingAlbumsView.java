/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.kheops;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.ListDataProvider;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.KheopsAlbumsEntity;
import org.weasis.core.util.annotations.Generated;

@Generated()
@NullUnmarked
public class SwitchingAlbumsView extends CustomField<List<KheopsAlbumsEntity>> {

	private final NewSwitchingAlbum newSwitchingAlbum;

	private final GridSwitchingAlbums gridSwitchingAlbums;

	private final Binder<KheopsAlbumsEntity> newSwitchingAlbumBinder;

	private final ListDataProvider<KheopsAlbumsEntity> dataProviderSwitchingAlbums;

	private final List<KheopsAlbumsEntity> kheopsAlbumsEntityList;

	private final Checkbox checkboxSwitchingAlbums;

	private final VerticalLayout layout;

	public SwitchingAlbumsView() {
		setWidthFull();
		gridSwitchingAlbums = new GridSwitchingAlbums();
		dataProviderSwitchingAlbums = (ListDataProvider<KheopsAlbumsEntity>) gridSwitchingAlbums.getDataProvider();
		newSwitchingAlbum = new NewSwitchingAlbum();
		newSwitchingAlbumBinder = newSwitchingAlbum.getBinder();
		kheopsAlbumsEntityList = new ArrayList<>();
		layout = new VerticalLayout();
		checkboxSwitchingAlbums = new Checkbox("Switching in different KHEOPS albums");
		add(checkboxSwitchingAlbums, layout);
		setEventCheckBox();
		setEventButtonAdd();
	}

	private void setEventCheckBox() {
		checkboxSwitchingAlbums.addValueChangeListener(event -> {
			if (event.getValue() != null) {
				addComponent(event.getValue());
			}
		});
	}

	private void setCheckboxSwitchingAlbumsValue() {
		checkboxSwitchingAlbums.setValue(!dataProviderSwitchingAlbums.getItems().isEmpty());
	}

	public void addComponent(Boolean value) {
		if (value == true) {
			layout.add(newSwitchingAlbum, gridSwitchingAlbums);
		}
		else {
			newSwitchingAlbum.clear();
			layout.removeAll();
		}
	}

	public VerticalLayout getComponent() {
		return layout;
	}

	private void setEventButtonAdd() {
		newSwitchingAlbum.getButtonAdd().addClickListener(event -> {
			newSwitchingAlbum.getTextErrorConditionMsg().setText("");
			KheopsAlbumsEntity newKheopsAlbumsEntity = new KheopsAlbumsEntity();
			if (newSwitchingAlbumBinder.writeBeanIfValid(newKheopsAlbumsEntity)) {
				// Add only if not already existing in table
				if (!dataProviderSwitchingAlbums.getItems().contains(newKheopsAlbumsEntity)) {
					dataProviderSwitchingAlbums.getItems().add(newKheopsAlbumsEntity);
					dataProviderSwitchingAlbums.refreshAll();
					newSwitchingAlbum.clear();
				}
				else {
					// Otherwise error message already existing
					newSwitchingAlbum.getTextErrorConditionMsg().setText("Already existing");
				}
			}
		});
	}

	@Override
	protected List<KheopsAlbumsEntity> generateModelValue() {
		return kheopsAlbumsEntityList;
	}

	@Override
	public List<KheopsAlbumsEntity> getValue() {
		return checkboxSwitchingAlbums.getValue() ? new ArrayList<>(dataProviderSwitchingAlbums.getItems()) : null;
	}

	@Override
	public void setValue(List<KheopsAlbumsEntity> kheopsAlbumEntities) {
		dataProviderSwitchingAlbums.getItems().removeAll(dataProviderSwitchingAlbums.getItems());
		gridSwitchingAlbums.clearEditorEditButtons();
		dataProviderSwitchingAlbums.getItems()
			.addAll(kheopsAlbumEntities != null ? kheopsAlbumEntities : new ArrayList<>());
		dataProviderSwitchingAlbums.refreshAll();
		setCheckboxSwitchingAlbumsValue();
	}

	@Override
	protected void setPresentationValue(List<KheopsAlbumsEntity> kheopsAlbumEntities) {
	}

}
