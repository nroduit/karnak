/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profiles;

class TagPatternProfileTest {

	/*
	 * private static final String CURVES = "50xxxxxx"; private static final String
	 * OVERLAYS_DATA = "60xx3000"; private static final String OVERLAYS_COMMENTS =
	 * "60xx4000"; private static final String TRAILING_PADDING = "fffcfffx"; private
	 * static final String TRAILING_PADDING2 = "FFFCFFFX";
	 *
	 * private static DicomObject dataset = DicomObject.newDicomObject();
	 *
	 * @BeforeAll protected static void setUpBeforeClass() throws Exception {
	 * dataset.setString(TagEntity.CurveLabel, VR.LO, "curve label");
	 * dataset.newDicomSequence(TagEntity.CurveReferencedOverlaySequence);
	 * dataset.setNull(TagEntity.OverlayData | (1 << 17), VR.OB);
	 * dataset.setNull(TagEntity.OverlayData | (2 << 17), VR.OB);
	 * dataset.setNull(TagEntity.OverlayData | (5 << 17), VR.OB);
	 * dataset.setString(TagEntity.OverlayComments, VR.LT, "overlay comments"); }
	 *
	 * @Test void isValid() { assertEquals(true, TagPatternProfile.isValid(CURVES));
	 * assertEquals(true, TagPatternProfile.isValid(OVERLAYS_DATA)); assertEquals(true,
	 * TagPatternProfile.isValid(OVERLAYS_COMMENTS)); assertEquals(true,
	 * TagPatternProfile.isValid(TRAILING_PADDING)); assertEquals(true,
	 * TagPatternProfile.isValid(TRAILING_PADDING2));
	 *
	 * assertEquals(false, TagPatternProfile.isValid(null)); assertEquals(false,
	 * TagPatternProfile.isValid("")); assertEquals(false,
	 * TagPatternProfile.isValid("fffcfffc"), "must be invalid when no x");
	 * assertEquals(false, TagPatternProfile.isValid("00181000"),
	 * "must be invalid when no x"); assertEquals(false,
	 * TagPatternProfile.isValid("60xx3000A")); assertEquals(false,
	 * TagPatternProfile.isValid("G0xx3000")); assertEquals(false,
	 * TagPatternProfile.isValid("60xx-3000")); assertEquals(false,
	 * TagPatternProfile.isValid("(60003000)")); }
	 *
	 * @Test void getAction() { // WHITELIST policy for curves pattern with an exception
	 * to keep (CurveReferencedOverlaySequence) TagPatternProfile curves =
	 * buildTagPatternProfile(CURVES);
	 * curves.put(TagEntity.CurveReferencedOverlaySequence, Action.KEEP);
	 * Assertions.assertThrows(IllegalStateException.class, () -> {
	 * curves.put(TagEntity.CurveReferencedOverlaySequence, Action.REMOVE); });
	 * Assertions.assertThrows(IllegalStateException.class, () -> {
	 * curves.put(TagEntity.CurveReferencedOverlaySequence, Action.REPLACE_NULL); });
	 * assertEquals(Action.REMOVE,
	 * curves.getAction(dataset.get(TagEntity.CurveLabel).orElse(null)));
	 * assertEquals(Action.KEEP,
	 * curves.getAction(dataset.get(TagEntity.CurveReferencedOverlaySequence).orElse(null)
	 * )); assertEquals(null, curves.getAction(dataset.get(TagEntity.OverlayData | (1 <<
	 * 17)).orElse(null)));
	 *
	 * // WHITELIST policy for overlay pattern with an exception to keep (first layer of
	 * OverlayData) TagPatternProfile overlayData = buildTagPatternProfile(OVERLAYS_DATA);
	 * overlayData.put(TagEntity.OverlayData | (1 << 17), Action.KEEP);
	 * assertEquals(Action.KEEP, overlayData.getAction(dataset.get(TagEntity.OverlayData |
	 * (1 << 17)).orElse(null))); assertEquals(Action.REMOVE,
	 * overlayData.getAction(dataset.get(TagEntity.OverlayData | (2 <<
	 * 17)).orElse(null))); assertEquals(Action.REMOVE,
	 * overlayData.getAction(dataset.get(TagEntity.OverlayData | (5 <<
	 * 17)).orElse(null))); assertEquals(null,
	 * overlayData.getAction(dataset.get(TagEntity.CurveLabel).orElse(null)));
	 *
	 * assertEquals(Action.REMOVE,
	 * buildTagPatternProfile(OVERLAYS_COMMENTS).getAction(dataset.get(TagEntity.
	 * OverlayComments).orElse(null)));
	 *
	 * // BLACKLIST policy for curves pattern with parent profile TagPatternProfile
	 * curves2 = new TagPatternProfile("", CURVES, overlayData);
	 * curves2.put(TagEntity.CurveReferencedOverlaySequence, Action.REMOVE);
	 * curves2.put(TagEntity.CurveLabel, Action.DEFAULT_DUMMY);
	 * Assertions.assertThrows(IllegalStateException.class, () -> {
	 * curves2.put(TagEntity.CurveReferencedOverlaySequence, Action.KEEP); });
	 * assertEquals(Action.DEFAULT_DUMMY,
	 * curves2.getAction(dataset.get(TagEntity.CurveLabel).orElse(null)));
	 * assertEquals(Action.REMOVE,
	 * curves2.getAction(dataset.get(TagEntity.CurveReferencedOverlaySequence).orElse(null
	 * ))); assertEquals(Action.KEEP, curves2.getAction(dataset.get(TagEntity.OverlayData
	 * | (1 << 17)).orElse(null))); }
	 *
	 * @Test void buildProfile() { Assertions.assertThrows(IllegalArgumentException.class,
	 * () -> { buildTagPatternProfile(Integer.toHexString(TagEntity.OverlayData)); });
	 * Assertions.assertThrows(IllegalArgumentException.class, () -> {
	 * buildTagPatternProfile("5518100"); }); }
	 *
	 * private static TagPatternProfile buildTagPatternProfile(String pattern) { return
	 * new TagPatternProfile("", pattern, null); }
	 */

}
