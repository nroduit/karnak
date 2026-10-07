/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Test;
import org.karnak.backend.model.dicominnolitics.JsonCIODtoFuncGroupMacro;
import org.karnak.backend.model.dicominnolitics.StandardCIODtoFuncGroupMacros;

/**
 * Guards the shipped {@code curated-validation-rules.json}: every conditional-requirement
 * predicate must be well formed so a curation typo fails the build instead of silently
 * disabling a check.
 */
@DisplayNameGeneration(ReplaceUnderscores.class)
class CuratedConditionalRequirementsTest {

	private static Map<String, ConditionalRequirement> conditions;

	@BeforeAll
	static void load() {
		conditions = CuratedValidationRules.load().getConditionalRequirements();
	}

	@Test
	void curated_file_provides_conditional_requirements() {
		assertFalse(conditions.isEmpty());
	}

	@Test
	void every_key_is_module_id_slash_tag_path() {
		conditions.keySet().forEach(key -> {
			String[] parts = key.split("/", 2);
			assertEquals(2, parts.length, "key must be moduleId/tagPath: " + key);
			assertFalse(parts[0].isBlank(), "blank moduleId in " + key);
			for (String segment : parts[1].split(":")) {
				assertTrue(segment.matches("[0-9a-fA-F]{8}"), "tag path segment must be 8 hex digits: " + key);
			}
		});
	}

	@Test
	void every_predicate_is_well_formed() {
		conditions.forEach((key, requirement) -> assertWellFormed(key, requirement.getRequiredWhen()));
	}

	@Test
	void every_functional_group_macro_of_the_standard_has_its_sequence() {
		Map<String, Map<String, String>> macros = CuratedValidationRules.load().getFunctionalGroupMacros();
		Arrays.stream(StandardCIODtoFuncGroupMacros.readJsonCIODToFuncGroupMacros())
			.map(JsonCIODtoFuncGroupMacro::getMacroId)
			.distinct()
			.forEach(macroId -> {
				Map<String, String> sequences = macros.get(macroId);
				assertTrue(sequences != null && !sequences.isEmpty(), "no curated sequence for macro " + macroId);
				sequences.keySet()
					.forEach(tag -> assertTrue(tag.matches("[0-9a-f]{8}"), "bad tag " + tag + " in " + macroId));
			});
	}

	@Test
	void every_functional_group_condition_targets_a_conditional_macro() {
		Set<String> conditionalMacros = Arrays.stream(StandardCIODtoFuncGroupMacros.readJsonCIODToFuncGroupMacros())
			.filter(macro -> "C".equals(macro.getUsage()))
			.map(macro -> macro.getCiodId() + "/" + macro.getMacroId())
			.collect(Collectors.toSet());
		Map<String, ConditionalRequirement> fgConditions = CuratedValidationRules.load().getFunctionalGroupConditions();
		assertFalse(fgConditions.isEmpty());
		fgConditions.forEach((key, requirement) -> {
			assertTrue(conditionalMacros.contains(key), "not a C macro of the standard: " + key);
			assertWellFormed(key, requirement.getRequiredWhen());
		});
	}

	private static void assertWellFormed(String key, Condition condition) {
		assertTrue(condition != null, "missing requiredWhen in " + key);
		List<Condition> allOf = condition.getAllOf();
		List<Condition> anyOf = condition.getAnyOf();
		if (allOf != null || anyOf != null) {
			List<Condition> children = allOf != null ? allOf : anyOf;
			assertFalse(children.isEmpty(), "empty composite in " + key);
			children.forEach(child -> assertWellFormed(key, child));
			return;
		}
		// Leaf: a tag plus exactly one operator
		assertTrue(condition.getTag() != null && condition.getTag().matches("[0-9a-fA-F]{8}"),
				"leaf tag must be 8 hex digits in " + key);
		int operators = 0;
		if (condition.getPresent() != null) {
			operators++;
		}
		if (condition.getEquals() != null) {
			operators++;
		}
		if (condition.getIn() != null) {
			operators++;
		}
		if (condition.getNotIn() != null) {
			operators++;
		}
		assertEquals(1, operators, "leaf must have exactly one operator in " + key);
	}

}