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

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NullUnmarked;

/**
 * Curated validation rules supplementing the innolitics DICOM standard JSON (which has no
 * machine-readable enumerated values or SOP Class / Modality coherence data). Loaded from
 * the bundled {@code curated-validation-rules.json} resource — see
 * {@code doc/dicom-standard-json.md} for the source and update procedure.
 */
@NullUnmarked
public class CuratedValidationRules {

	/**
	 * Fallback source label used when the bundled JSON resource omits the {@code source}
	 * field (e.g. an older rules file).
	 */
	public static final String DEFAULT_DICOM_STANDARD_SOURCE = "innolitics/dicom-standard JSON";

	private static final String RESOURCE = "curated-validation-rules.json";

	/**
	 * Source and vintage of the bundled DICOM standard JSON files, shown in report
	 * headers. Read from the {@code source} field of
	 * {@code curated-validation-rules.json} so it lives next to the data it describes —
	 * update it there when re-vendoring the standard files (see
	 * doc/dicom-standard-json.md).
	 */
	private String source;

	private Map<String, EnumeratedRule> enumeratedValues;

	private Map<String, List<String>> sopClassToModalities;

	private List<String> retiredTransferSyntaxes;

	private Map<String, ConditionalRequirement> conditionalRequirements;

	private List<String> identifyingAttributes;

	private List<String> unpairedBodyParts;

	private List<String> zeroIsError;

	private List<String> zeroIsWarning;

	public static CuratedValidationRules load() {
		try (InputStream in = CuratedValidationRules.class.getResourceAsStream(RESOURCE);
				Reader reader = new InputStreamReader(Objects.requireNonNull(in, RESOURCE), StandardCharsets.UTF_8)) {
			return new Gson().fromJson(reader, CuratedValidationRules.class);
		}
		catch (Exception e) {
			throw new JsonParseException("Cannot parse json %s correctly".formatted(RESOURCE), e);
		}
	}

	/**
	 * Source and vintage label of the bundled DICOM standard JSON, for report headers.
	 */
	public String getDicomStandardSource() {
		return source == null || source.isBlank() ? DEFAULT_DICOM_STANDARD_SOURCE : source;
	}

	public Map<String, EnumeratedRule> getEnumeratedValues() {
		return enumeratedValues == null ? Map.of() : enumeratedValues;
	}

	public Map<String, List<String>> getSopClassToModalities() {
		return sopClassToModalities == null ? Map.of() : sopClassToModalities;
	}

	public List<String> getRetiredTransferSyntaxes() {
		return retiredTransferSyntaxes == null ? List.of() : retiredTransferSyntaxes;
	}

	/**
	 * Machine-evaluable Type 1C/2C conditions keyed by {@code moduleId/tagPath},
	 * supplementing the free-text conditions of the standard JSON. See
	 * {@link org.karnak.backend.model.validation.ConditionEvaluator}.
	 */
	public Map<String, ConditionalRequirement> getConditionalRequirements() {
		return conditionalRequirements == null ? Map.of() : conditionalRequirements;
	}

	/**
	 * Direct patient identifiers that a de-identification profile is expected to remove
	 * or empty. Their presence with a value in a forwarded instance is flagged as a
	 * residual privacy risk. Keys are lowercase or upper 8-digit hex tags in the JSON;
	 * malformed entries are skipped.
	 */
	public Set<Integer> getIdentifyingAttributes() {
		if (identifyingAttributes == null) {
			return Set.of();
		}
		return parseTags(identifyingAttributes);
	}

	/**
	 * Body Part Examined (0018,0015) Defined Terms that denote an unpaired (midline)
	 * structure, for which a left/right Laterality is not expected. Mirrors the unpaired
	 * body-part list in dciodvfy's {@code LateralityRequired} condition (condn.tpl): any
	 * body part not in this list is treated as paired. Uppercase.
	 */
	public Set<String> getUnpairedBodyParts() {
		return unpairedBodyParts == null ? Set.of() : new LinkedHashSet<>(unpairedBodyParts);
	}

	/**
	 * Numeric attributes for which a zero value is illegal (structural counts and
	 * geometry, e.g. Rows, Columns, Bits Allocated, Pixel Spacing) — dciodvfy's
	 * {@code NotZeroError} set.
	 */
	public Set<Integer> getZeroIsErrorAttributes() {
		return parseTags(zeroIsError);
	}

	/**
	 * Numeric attributes for which a zero value is physically implausible and almost
	 * always a dummy fill (acquisition / physics parameters, e.g. KVP, X-Ray Tube
	 * Current) — dciodvfy's {@code NotZeroWarning} set.
	 */
	public Set<Integer> getZeroIsWarningAttributes() {
		return parseTags(zeroIsWarning);
	}

	private static Set<Integer> parseTags(List<String> hexTags) {
		if (hexTags == null) {
			return Set.of();
		}
		Set<Integer> tags = new LinkedHashSet<>();
		for (String hex : hexTags) {
			try {
				tags.add((int) Long.parseLong(hex, 16));
			}
			catch (NumberFormatException _) {
				// Skip malformed tag entries rather than failing the whole rule set
			}
		}
		return tags;
	}

}
