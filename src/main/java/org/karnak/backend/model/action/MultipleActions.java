/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.action;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.config.AppConfig;
import org.karnak.backend.exception.StandardDICOMException;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.standard.Module;
import org.karnak.backend.model.standard.ModuleAttribute;
import org.karnak.backend.model.standard.StandardDICOM;
import org.karnak.backend.util.MetadataDICOMObject;

@Slf4j
public class MultipleActions extends AbstractAction {

	private final StandardDICOM standardDICOM;

	private final ActionItem defaultDummyValue;

	private final ActionItem actionUID;

	private final ActionItem actionReplaceNull;

	private final ActionItem actionRemove;

	public MultipleActions(String symbol) {
		super(symbol);
		standardDICOM = AppConfig.getInstance().getStandardDICOM();
		defaultDummyValue = new DefaultDummy(symbol);
		actionUID = new UID("U");
		actionReplaceNull = new ReplaceNull("Z");
		actionRemove = new Remove("X");
	}

	@Override
	public void execute(Attributes dcm, int tag, HMAC hmac) {
		String sopUID = MetadataDICOMObject.getValue(dcm, Tag.SOPClassUID);
		String tagPath = MetadataDICOMObject.getTagPath(dcm, tag);
		ActionItem action;
		try {
			List<ModuleAttribute> moduleAttributes = standardDICOM.getAttributesBySOP(sopUID, tagPath);
			action = switch (moduleAttributes.size()) {
				case 0 -> {
					log.warn("Cannot find the attribute {} in the SOP {}. The strictest action will be chosen ({}).",
							tagPath, sopUID, symbol);
					yield defaultAction();
				}
				case 1 -> chooseAction(sopUID, moduleAttributes.getFirst().getType());
				default -> multipleAttributes(sopUID, moduleAttributes);
			};
		}
		catch (StandardDICOMException e) {
			log.warn(
					"Cannot execute the action {} with the SOP {} and the attribute {}. The strictest action will be chosen.",
					symbol, sopUID, tagPath, e);
			action = defaultAction();
		}
		action.execute(dcm, tag, hmac);
	}

	private ActionItem multipleAttributes(String sopUID, List<ModuleAttribute> moduleAttributes) {
		List<ModuleAttribute> mandatory = getMandatoryAttributes(sopUID, moduleAttributes);
		String currentType = mandatory.size() == 1 ? mandatory.getFirst().getType()
				: ModuleAttribute.getStricterType(mandatory.isEmpty() ? moduleAttributes : mandatory);
		return chooseAction(sopUID, currentType);
	}

	private List<ModuleAttribute> getMandatoryAttributes(String sopUID, List<ModuleAttribute> moduleAttributes) {
		return moduleAttributes.stream()
			.filter(attribute -> standardDICOM.getModuleByModuleID(sopUID, attribute.getModuleId())
				.filter(Module::moduleIsMandatory)
				.isPresent())
			.toList();
	}

	private ActionItem chooseAction(String sopUID, @Nullable String currentType) {
		if (currentType == null) {
			return defaultAction();
		}
		return switch (symbol) {
			case "Z/D" -> dummyOrReplaceNull(currentType);
			case "X/D" -> dummyOrRemove(currentType);
			case "X/Z/D" -> dummyOrReplaceNullOrRemove(currentType);
			case "X/Z" -> replaceNullOrRemove(currentType);
			case "X/Z/U", "X/Z/U*" -> uidReplaceNullOrRemove(currentType);
			default -> defaultDummyValue;
		};
	}

	private ActionItem defaultAction() {
		return switch (symbol) {
			case "X/Z" -> actionReplaceNull;
			case "X/Z/U", "X/Z/U*" -> actionUID;
			default -> defaultDummyValue;
		};
	}

	private ActionItem dummyOrReplaceNull(String currentType) {
		return isType1(currentType) ? defaultDummyValue : actionReplaceNull;
	}

	private ActionItem dummyOrRemove(String currentType) {
		return currentType.equals("3") ? actionRemove : defaultDummyValue;
	}

	private ActionItem dummyOrReplaceNullOrRemove(String currentType) {
		if (isType1(currentType)) {
			return defaultDummyValue;
		}
		return isType2(currentType) ? actionReplaceNull : actionRemove;
	}

	private ActionItem replaceNullOrRemove(String currentType) {
		// A type 1/1C tag should not reach X/Z; both branches null it out or drop it.
		return isType2(currentType) ? actionReplaceNull : actionRemove;
	}

	private ActionItem uidReplaceNullOrRemove(String currentType) {
		if (isType1(currentType)) {
			return actionUID;
		}
		return isType2(currentType) ? actionReplaceNull : actionRemove;
	}

	// Type 1/1C: required, non-empty. Type 2/2C: required, may be empty.
	private static boolean isType1(String type) {
		return type.equals("1") || type.equals("1C");
	}

	private static boolean isType2(String type) {
		return type.equals("2") || type.equals("2C");
	}

}
