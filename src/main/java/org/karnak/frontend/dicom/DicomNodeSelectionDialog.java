/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.frontend.dicom;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.util.List;
import lombok.Getter;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.DicomNodeList;
import org.weasis.core.util.annotations.Generated;

/**
 * Lightweight, read-only picker for a configured DICOM node: pick a group, then a node,
 * then confirm. It only fills a form - creating, editing, grouping and importing nodes
 * live in the dedicated management sections. Used by the DICOM Echo and Worklist tools.
 */
@Generated()
@NullUnmarked
public class DicomNodeSelectionDialog extends Dialog {

	private final List<DicomNodeList> groups;

	private Select<DicomNodeList> groupSelector;

	private ComboBox<ConfigNode> nodeSelector;

	private Button selectBtn;

	public DicomNodeSelectionDialog(List<DicomNodeList> groups, String title, String nodeLabel) {
		this.groups = (groups != null) ? groups : List.of();
		setHeaderTitle(title);
		setWidth("420px");

		if (this.groups.isEmpty()) {
			add(new Div("No node is configured yet."));
		}
		else {
			add(buildForm(nodeLabel));
		}
		addButtons();
	}

	public void addSelectDicomNodeListener(ComponentEventListener<SelectDicomNodeEvent> listener) {
		addListener(SelectDicomNodeEvent.class, listener);
	}

	private FormLayout buildForm(String nodeLabel) {
		nodeSelector = new ComboBox<>(nodeLabel);
		nodeSelector.setClearButtonVisible(true);
		nodeSelector.setPlaceholder("Select a node");
		nodeSelector.setItemLabelGenerator(item -> item.getName() + " [" + item.getAet() + " | " + item.getHostname()
				+ " | " + item.getPort() + "]");
		nodeSelector.setRenderer(getDivConfigNodeComponentRenderer());
		nodeSelector.addValueChangeListener(event -> {
			if (selectBtn != null) {
				selectBtn.setEnabled(event.getValue() != null);
			}
		});

		FormLayout form = new FormLayout();

		// Only show the Group filter when there is more than one group to choose from;
		// with a single group it would be a redundant control (e.g. the worklist nodes).
		if (groups.size() > 1) {
			groupSelector = new Select<>();
			groupSelector.setLabel("Group");
			groupSelector.setPlaceholder("Select a group");
			groupSelector.setItems(groups);
			groupSelector.addValueChangeListener(event -> populateNodes(event.getValue()));
			form.add(groupSelector, nodeSelector);
		}
		else {
			populateNodes(groups.getFirst());
			form.add(nodeSelector);
		}
		return form;
	}

	private void populateNodes(DicomNodeList group) {
		List<ConfigNode> items = (group != null) ? group : List.of();
		nodeSelector.setItems(items);
		// No default selection: the user must pick a node explicitly.
		nodeSelector.clear();
	}

	private void addButtons() {
		Button cancelBtn = new Button("Cancel", event -> close());

		selectBtn = new Button("Select", event -> {
			if (nodeSelector != null && nodeSelector.getValue() != null) {
				fireEvent(new SelectDicomNodeEvent(this, false, nodeSelector.getValue()));
			}
			close();
		});
		selectBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		// No node is selected by default, so the confirm button starts disabled and is
		// enabled by the node selector's value-change listener once the user picks one.
		selectBtn.setEnabled(nodeSelector != null && nodeSelector.getValue() != null);

		getFooter().add(cancelBtn, selectBtn);
	}

	/**
	 * Renders a node as its description over its {@code AET | host | port} network
	 * details.
	 */
	public static ComponentRenderer<Div, ConfigNode> getDivConfigNodeComponentRenderer() {
		return new ComponentRenderer<>(item -> {
			Div div = new Div();
			div.getStyle().set("line-height", "92%");

			Span spanDescription = new Span(item.getName());
			spanDescription.getStyle().set("font-weight", "500");

			HtmlComponent htmlLineBreak = new HtmlComponent("BR");

			Span spanOtherAttributes = new Span(item.getAet() + " | " + item.getHostname() + " | " + item.getPort());
			spanOtherAttributes.getStyle().set("font-size", "75%");

			div.add(spanDescription, htmlLineBreak, spanOtherAttributes);
			return div;
		});
	}

	@Getter
	public static class SelectDicomNodeEvent extends ComponentEvent<DicomNodeSelectionDialog> {

		private final ConfigNode selectedDicomNode;

		public SelectDicomNodeEvent(DicomNodeSelectionDialog source, boolean fromClient, ConfigNode selectedDicomNode) {
			super(source, fromClient);
			this.selectedDicomNode = selectedDicomNode;
		}

	}

}
