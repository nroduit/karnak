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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.UploadHandler;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullUnmarked;
import org.karnak.backend.data.entity.DicomNodeConfigEntity;
import org.karnak.backend.enums.MessageFormat;
import org.karnak.backend.enums.MessageLevel;
import org.karnak.backend.model.dicom.ConfigNode;
import org.karnak.backend.model.dicom.DicomNodeList;
import org.karnak.backend.model.dicom.Message;
import org.karnak.backend.service.DicomNodeConfigService;
import org.karnak.backend.util.DicomNodeUtil;
import org.weasis.core.util.annotations.Generated;

/**
 * Management section for the configured DICOM nodes: a grid with add/edit/delete and CSV
 * import/export. The dynamic Gateway destinations are listed read-only and cannot be
 * edited, deleted or used as an import target.
 */
@Generated()
@NullUnmarked
public class ManageDicomNodesView extends AbstractView {

	private static final char CSV_SEPARATOR = ',';

	private final transient DicomNodeConfigService service;

	private final transient DicomNodeUtil dicomNodeUtil;

	private final DicomNodeManagementGrid grid;

	private final transient UI ui;

	public ManageDicomNodesView(DicomNodeConfigService service, DicomNodeUtil dicomNodeUtil) {
		this.service = service;
		this.dicomNodeUtil = dicomNodeUtil;
		this.ui = UI.getCurrent();
		this.grid = createGrid();

		createMainLayout();
		refresh();
	}

	private void createMainLayout() {
		// Fill the page so the grid can flex-grow and scroll internally rather than
		// rendering at its intrinsic (header-only) height.
		setSizeFull();
		getStyle().set("min-height", "0");

		mainLayout = new VerticalLayout();
		mainLayout.setSizeFull();
		mainLayout.setPadding(true);
		mainLayout.setSpacing(true);
		mainLayout.getStyle().set("min-height", "0");

		H6 title = new H6("DICOM Node Management");
		title.getStyle().set("margin-top", "0px");

		Button addBtn = new Button("Add Node", VaadinIcon.PLUS.create(), event -> openEditor(null));
		addBtn.addThemeVariants(ButtonVariant.PRIMARY);

		Button importExportBtn = new Button("Import / Export", VaadinIcon.EXCHANGE.create(),
				event -> openImportExportDialog());

		HorizontalLayout toolbar = new HorizontalLayout(addBtn, importExportBtn);
		toolbar.setSpacing(true);

		mainLayout.add(title, toolbar, grid);
		mainLayout.setFlexGrow(1, grid);
		add(mainLayout);
	}

	private DicomNodeManagementGrid createGrid() {
		DicomNodeManagementGrid managementGrid = new DicomNodeManagementGrid();
		managementGrid.setSizeFull();
		// Floor the grid at a few rows: it flex-shrinks with the window down to this
		// height, and only then does the page scroll.
		managementGrid.setMinHeight("200px");
		managementGrid.setEditHandler(this::openEditor);
		managementGrid.setDeleteHandler(this::deleteNode);
		return managementGrid;
	}

	private void refresh() {
		List<DicomNodeConfigEntity> rows = new ArrayList<>(service.findAll());
		for (DicomNodeList group : dicomNodeUtil.getDynamicNodeGroups()) {
			for (ConfigNode node : group) {
				rows.add(dynamicRow(node));
			}
		}
		grid.setItems(rows);
	}

	/**
	 * A read-only grid row (no id) for a node coming from a dynamic source (e.g. the
	 * Gateway destinations); its group is the source's group name carried on the node.
	 */
	private static DicomNodeConfigEntity dynamicRow(ConfigNode node) {
		var entity = new DicomNodeConfigEntity(node.getName(), node.getAet(), node.getHostname(), node.getPort(), "",
				node.getNodeType());
		entity.setId(null);
		return entity;
	}

	private void openEditor(DicomNodeConfigEntity node) {
		DicomNodeEditorDialog dialog = new DicomNodeEditorDialog(node, service.getNodeTypes(), service.getKnownGroups(),
				DicomNodeConfigService.NODE_TYPE_WORKSTATION);
		dialog.addSaveNodeListener(this::saveNode);
		dialog.open();
	}

	private void saveNode(DicomNodeEditorDialog.SaveNodeEvent event) {
		try {
			if (event.getNodeId() != null) {
				service.updateNode(event.getNodeId(), event.getDescription(), event.getAeTitle(), event.getHostname(),
						event.getPort(), event.getNodeType(), event.getNodeGroup());
			}
			else {
				service.saveNode(event.getDescription(), event.getAeTitle(), event.getHostname(), event.getPort(),
						event.getNodeType(), event.getNodeGroup());
			}
			refresh();
			displayMessage(new Message(MessageLevel.INFO, MessageFormat.TEXT, "DICOM node saved"));
		}
		catch (Exception ex) {
			displayMessage(new Message(MessageLevel.ERROR, MessageFormat.TEXT,
					"Cannot save the DICOM node: " + ex.getMessage()));
		}
	}

	private void deleteNode(DicomNodeConfigEntity node) {
		if (node.getId() == null) {
			return;
		}
		try {
			service.deleteNode(node.getId());
			refresh();
			displayMessage(new Message(MessageLevel.INFO, MessageFormat.TEXT, "DICOM node deleted"));
		}
		catch (Exception ex) {
			displayMessage(new Message(MessageLevel.ERROR, MessageFormat.TEXT,
					"Cannot delete the DICOM node: " + ex.getMessage()));
		}
	}

	private void openImportExportDialog() {
		List<String> knownGroups = service.getKnownGroups();

		ComboBox<String> importGroup = groupCombo("Import into group", "blank = use the file's own group column",
				knownGroups);
		importGroup.setAllowCustomValue(true);
		importGroup.addCustomValueSetListener(event -> importGroup.setValue(event.getDetail()));
		Checkbox replace = new Checkbox("Replace existing in scope");

		ComboBox<String> exportGroup = groupCombo("Export group", "blank = every group", knownGroups);

		Upload importUpload = new Upload((UploadHandler) event -> {
			byte[] bytes;
			try (InputStream in = event.getInputStream()) {
				bytes = in.readAllBytes();
			}
			String targetGroup = emptyToNull(importGroup.getValue());
			boolean replaceExisting = replace.getValue();
			// Replacing without a target group wipes every node (worklist included), so
			// confirm first.
			if (replaceExisting && targetGroup == null) {
				runOnUi(() -> confirmReplaceAll(() -> doImport(bytes, null, true)));
			}
			else {
				doImport(bytes, targetGroup, replaceExisting);
			}
		});
		importUpload.setDropLabel(new Span("Drop a CSV file here"));
		importUpload.setUploadButton(new Button("Import CSV", VaadinIcon.UPLOAD.create()));
		importUpload.setAcceptedFileTypes(".csv", "text/csv");
		importUpload.setMaxFiles(1);

		Anchor exportAnchor = new Anchor();
		exportAnchor.setHref(DownloadHandler.fromInputStream(event -> new DownloadResponse(
				new ByteArrayInputStream(service.exportCsv(emptyToNull(exportGroup.getValue()))), "dicom-nodes.csv",
				"text/csv", -1)));
		exportAnchor.getElement().setAttribute("download", true);
		exportAnchor.add(new Button("Export CSV", VaadinIcon.DOWNLOAD.create()));

		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("Import / Export DICOM Nodes");
		dialog.add(importExportContent(importGroup, replace, importUpload, exportGroup, exportAnchor));
		dialog.getFooter().add(new Button("Close", event -> dialog.close()));
		dialog.open();
	}

	private static VerticalLayout importExportContent(ComboBox<String> importGroup, Checkbox replace,
			Upload importUpload, ComboBox<String> exportGroup, Anchor exportAnchor) {
		HorizontalLayout importOptions = new HorizontalLayout(importGroup, replace);
		importOptions.setAlignItems(Alignment.BASELINE);
		HorizontalLayout exportRow = new HorizontalLayout(exportGroup, exportAnchor);
		exportRow.setAlignItems(Alignment.BASELINE);

		VerticalLayout content = new VerticalLayout(sectionTitle("Import"), importOptions, importUpload,
				sectionTitle("Export"), exportRow);
		content.setPadding(false);
		content.setSpacing(true);
		content.setWidth("30em");
		return content;
	}

	private static H6 sectionTitle(String text) {
		H6 title = new H6(text);
		title.getStyle().set("margin-top", "0");
		return title;
	}

	private static ComboBox<String> groupCombo(String label, String helper, List<String> groups) {
		ComboBox<String> field = new ComboBox<>(label);
		field.setItems(groups);
		field.setPlaceholder("All groups");
		field.setClearButtonVisible(true);
		field.setHelperText(helper);
		return field;
	}

	private void doImport(byte[] bytes, String targetGroup, boolean replace) {
		try {
			DicomNodeConfigService.ImportReport report = service.importCsv(new ByteArrayInputStream(bytes),
					CSV_SEPARATOR, targetGroup, replace);
			runOnUi(() -> {
				refresh();
				showImportReport(targetGroup, report);
			});
		}
		catch (Exception ex) {
			runOnUi(() -> displayMessage(new Message(MessageLevel.ERROR, MessageFormat.TEXT,
					"Cannot import the CSV file: " + ex.getMessage())));
		}
	}

	private void showImportReport(String targetGroup, DicomNodeConfigService.ImportReport report) {
		String scope = (targetGroup != null) ? "group '" + targetGroup + "'" : "all groups";
		String removed = (report.removed() > 0) ? " (" + report.removed() + " removed)" : "";
		String summary = report.imported() + " DICOM node(s) imported into " + scope + removed;

		if (report.errors().isEmpty()) {
			displayMessage(new Message(MessageLevel.INFO, MessageFormat.TEXT, summary));
			return;
		}

		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("Import report");
		dialog.add(new Div(summary + " - " + report.errors().size() + " row(s) skipped:"));
		UnorderedList list = new UnorderedList();
		report.errors().forEach(message -> list.add(new ListItem(message)));
		dialog.add(list);
		dialog.getFooter().add(new Button("Close", event -> dialog.close()));
		dialog.open();
	}

	private void confirmReplaceAll(Runnable onConfirm) {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle("Replace all DICOM nodes?");
		dialog.add(new Div("This deletes every existing DICOM node (including worklist nodes) before importing "
				+ "the file. This cannot be undone."));

		Button cancel = new Button("Cancel", event -> dialog.close());
		Button confirm = new Button("Delete all & import", event -> {
			dialog.close();
			onConfirm.run();
		});
		confirm.addThemeVariants(ButtonVariant.PRIMARY, ButtonVariant.ERROR);

		dialog.getFooter().add(cancel, confirm);
		dialog.open();
	}

	private void runOnUi(Runnable command) {
		if (ui != null) {
			ui.access(command::run);
		}
		else {
			command.run();
		}
	}

	private static String emptyToNull(String value) {
		return (value == null || value.isBlank()) ? null : value;
	}

}
