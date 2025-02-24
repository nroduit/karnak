/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.action;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.karnak.backend.config.AppConfig;
import org.karnak.backend.exception.StandardDICOMException;
import org.karnak.backend.model.profilepipe.HMAC;
import org.karnak.backend.model.standard.Module;
import org.karnak.backend.model.standard.ModuleAttribute;
import org.karnak.backend.model.standard.StandardDICOM;
import org.karnak.backend.util.MetadataDICOMObject;

@Slf4j
public class MultipleActions extends AbstractAction {

	final StandardDICOM standardDICOM;

	final ActionItem defaultDummyValue;

	final ActionItem actionUID;

	final ActionItem actionReplaceNull;

	final ActionItem actionRemove;

	final ActionItem actionKeep;

	public MultipleActions(String symbol) {
		super(symbol);
		standardDICOM = AppConfig.getInstance().getStandardDICOM();
		defaultDummyValue = new DefaultDummy(symbol);
		actionUID = new UID("U");
		actionReplaceNull = new ReplaceNull("Z");
		actionRemove = new Remove("X");
		actionKeep = new Keep("K");
	}

	@Override
	public void execute(Attributes dcm, int tag, HMAC hmac) {
		String sopUID = MetadataDICOMObject.getValue(dcm, Tag.SOPClassUID);
		String tagPath = MetadataDICOMObject.getTagPath(dcm, tag);
		try {
			List<ModuleAttribute> moduleAttributes = standardDICOM.getAttributesBySOP(sopUID, tagPath);
			if (moduleAttributes.size() == 1) {
				String currentType = moduleAttributes.get(0).getType();
				ActionItem actionItem = chooseAction(sopUID, currentType);
				actionItem.execute(dcm, tag, hmac);
			}
			else if (moduleAttributes.size() > 1) {
				ActionItem action = multipleAttributes(sopUID, moduleAttributes);
				action.execute(dcm, tag, hmac);
			}
			else {
				ActionItem action = defaultAction();
				action.execute(dcm, tag, hmac);
				log.warn("Cannot find the attribute {} in the SOP {}.  The strictest action will be chosen ({}).",
						tagPath, sopUID, symbol);
			}
		}
		catch (StandardDICOMException standardDICOMException) {
			ActionItem action = defaultAction();
			action.execute(dcm, tag, hmac);
			log.warn(
					"Cannot execute the action {} with the SOP {} and the attribute {}. The strictest action will be chosen.",
					symbol, sopUID, tagPath, standardDICOMException);
		}
	}

	private ActionItem multipleAttributes(String sopUID, List<ModuleAttribute> moduleAttributes) {
		List<ModuleAttribute> mandatoryModuleAttributes = getMandatoryAttributes(sopUID, moduleAttributes);

		if (mandatoryModuleAttributes.isEmpty()) {
			String currentType = ModuleAttribute.getStrictedType(moduleAttributes);
			return chooseAction(sopUID, currentType);
		}

		if (mandatoryModuleAttributes.size() == 1) {
			String currentType = mandatoryModuleAttributes.get(0).getType();
			return chooseAction(sopUID, currentType);
		}

		String currentType = ModuleAttribute.getStrictedType(mandatoryModuleAttributes);
		return chooseAction(sopUID, currentType);
	}

	private List<ModuleAttribute> getMandatoryAttributes(String sopUID, List<ModuleAttribute> moduleAttributes) {
		List<ModuleAttribute> mandatoryModuleAttributes = new ArrayList<>();
		moduleAttributes.forEach(attribute -> {
			Module module = standardDICOM.getModuleByModuleID(sopUID, attribute.getModuleId()).orElse(null);
			if (module != null && Module.moduleIsMandatory(module)) {
				mandatoryModuleAttributes.add(attribute);
			}
		});
		return mandatoryModuleAttributes;
	}

	private ActionItem chooseAction(String sopUID, String currentType) {
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
		if (currentType.equals("1") || currentType.equals("1C")) {
			return defaultDummyValue;
		}
		return actionReplaceNull;
	}

	private ActionItem dummyOrRemove(String currentType) {
		if (currentType.equals("3")) {
			return actionRemove;
		}
		return defaultDummyValue;
	}

	private ActionItem dummyOrReplaceNullOrRemove(String currentType) {
		if (currentType.equals("1") || currentType.equals("1C")) {
			return defaultDummyValue;
		}
		if (currentType.equals("2") || currentType.equals("2C")) {
			return actionReplaceNull;
		}
		return actionRemove;
	}

	private ActionItem replaceNullOrRemove(String currentType) {
		/*
		 * TODO: throw exception ? if (currentType.equals("1") ||
		 * currentType.equals("1C")) { throw new Exception(For the current SOP, the tag
		 * must type 1. Impossible to execute and respect the standard); }
		 */
		if (currentType.equals("2") || currentType.equals("2C")) {
			return actionReplaceNull;
		}
		return actionRemove;
	}

	private ActionItem uidReplaceNullOrRemove(String currentType) {
		if (currentType.equals("1") || currentType.equals("1C")) {
			return actionUID;
		}
		if (currentType.equals("2") || currentType.equals("2C")) {
			return actionReplaceNull;
		}
		return actionRemove;
	}

}
