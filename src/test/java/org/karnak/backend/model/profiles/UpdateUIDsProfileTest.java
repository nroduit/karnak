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

class UpdateUIDsProfileTest {

	/*
	 * private static DicomObject dataset = DicomObject.newDicomObject(); private static
	 * UpdateUIDsProfile uidProfile;
	 *
	 * @BeforeAll protected static void setUpBeforeClass() throws Exception {
	 * TagPatternProfile curves = new TagPatternProfile("", "50xxxxxx", null);
	 * curves.put(TagEntity.CurveReferencedOverlaySequence, Action.KEEP); uidProfile = new
	 * UpdateUIDsProfile("", AbstractProfileItem.Type.REPLACE_UID.getClassAlias(),
	 * curves); dataset.setNull(TagEntity.OverlayData | (1 << 17), VR.OB);
	 * dataset.setString(TagEntity.CurveLabel, VR.LO, "curve label");
	 * dataset.newDicomSequence(TagEntity.CurveReferencedOverlaySequence);
	 * dataset.setString(TagEntity.SOPInstanceUID, VR.UI, UIDUtils.randomUID());
	 * dataset.setString(TagEntity.UID, VR.UI, UIDUtils.randomUID()); }
	 *
	 * @Test void getAction() { uidProfile.clearTagMap();
	 * uidProfile.put(TagEntity.SOPInstanceUID, Action.UID); assertEquals(Action.UID,
	 * uidProfile.getAction(dataset.get(TagEntity.SOPInstanceUID).orElse(null)));
	 * assertEquals(null, uidProfile.getAction(dataset.get(TagEntity.UID).orElse(null)));
	 *
	 * assertEquals(Action.KEEP,
	 * uidProfile.getAction(dataset.get(TagEntity.CurveReferencedOverlaySequence).orElse(
	 * null))); assertEquals(Action.REMOVE,
	 * uidProfile.getAction(dataset.get(TagEntity.CurveLabel).orElse(null)));
	 * assertEquals(null, uidProfile.getAction(dataset.get(TagEntity.OverlayData | (1 <<
	 * 17)).orElse(null))); }
	 *
	 * @Test void put() { uidProfile.clearTagMap(); assertEquals(0,
	 * uidProfile.tagMap.size());
	 *
	 * uidProfile.put(TagEntity.SOPInstanceUID, Action.UID); uidProfile.put(TagEntity.UID,
	 * Action.REMOVE);
	 *
	 * assertEquals(Action.REMOVE, uidProfile.remove(TagEntity.UID));
	 *
	 * Assertions.assertThrows(IllegalStateException.class, () -> {
	 * uidProfile.put(TagEntity.StudyInstanceUID, Action.KEEP); });
	 *
	 * // TODO UI options, should give the list of all UIDs and not allowed other tags //
	 * TODO Generate random UIDs // TODO Generate random UIDs with consistency }
	 */

}
