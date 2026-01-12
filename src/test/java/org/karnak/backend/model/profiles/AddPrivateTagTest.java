/*
 * Copyright (c) 2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashSet;
import java.util.Set;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.dcm4che3.util.TagUtils;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.model.standard.StandardDICOM;
import org.karnak.backend.service.profilepipe.Profile;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AddPrivateTagTest {

	@Test
	void addPrivateTag() {
		ProfileEntity profileEntity = new ProfileEntity();
		Attributes attributes = new Attributes();
		attributes.setString(Tag.Modality, VR.CS, "XA");
		attributes.setString(Tag.SOPClassUID, VR.UI, "1.2.840.10008.5.1.4.1.1.12.1");

		Set<ProfileElementEntity> profileElementEntities = new HashSet<>();
		ProfileElementEntity profileElementEntityAddBurnedAttr = new ProfileElementEntity();
		profileElementEntityAddBurnedAttr.setCodename("action.add.private.tag");
		profileElementEntityAddBurnedAttr.setName("Add private tag");
		profileElementEntityAddBurnedAttr
			.addArgument(new ArgumentEntity("value", "PrivateValue", profileElementEntityAddBurnedAttr));
		profileElementEntityAddBurnedAttr
			.addArgument(new ArgumentEntity("vr", "LO", profileElementEntityAddBurnedAttr));
		profileElementEntityAddBurnedAttr
			.addArgument(new ArgumentEntity("privateCreator", "SIEMENS", profileElementEntityAddBurnedAttr));

		profileElementEntityAddBurnedAttr
			.addIncludedTag(new IncludedTagEntity("(0021,1000)", profileElementEntityAddBurnedAttr));
		profileElementEntityAddBurnedAttr.setPosition(1);

		profileElementEntities.add(profileElementEntityAddBurnedAttr);
		profileEntity.setProfileElementEntities(profileElementEntities);
		Profile profile = new Profile(profileEntity);

		profile.applyAction(attributes, attributes, null, null, null, null);

		assertEquals("PrivateValue",
				attributes.getString(TagUtils.intFromHexString(StandardDICOM.cleanTagPath("(0021,1000)"))));
		assertEquals("SIEMENS",
				attributes.getString(TagUtils.intFromHexString(StandardDICOM.cleanTagPath("(0021,0010)"))));
	}

	@Test
	void addPrivateTagWithExistingPrivateCreator() {
		ProfileEntity profileEntity = new ProfileEntity();
		Attributes attributes = new Attributes();
		attributes.setString(Tag.Modality, VR.CS, "XA");
		attributes.setString(Tag.SOPClassUID, VR.UI, "1.2.840.10008.5.1.4.1.1.12.1");
		attributes.setString(TagUtils.intFromHexString(StandardDICOM.cleanTagPath("(0021,0010)")), VR.LO, "SIEMENS");

		Set<ProfileElementEntity> profileElementEntities = new HashSet<>();
		ProfileElementEntity profileElementEntityAddBurnedAttr = new ProfileElementEntity();
		profileElementEntityAddBurnedAttr.setCodename("action.add.private.tag");
		profileElementEntityAddBurnedAttr.setName("Add private tag");
		profileElementEntityAddBurnedAttr
			.addArgument(new ArgumentEntity("value", "PrivateValue", profileElementEntityAddBurnedAttr));
		profileElementEntityAddBurnedAttr
			.addArgument(new ArgumentEntity("vr", "LO", profileElementEntityAddBurnedAttr));
		profileElementEntityAddBurnedAttr
			.addIncludedTag(new IncludedTagEntity("(0021,1000)", profileElementEntityAddBurnedAttr));
		profileElementEntityAddBurnedAttr.setPosition(1);

		profileElementEntities.add(profileElementEntityAddBurnedAttr);
		profileEntity.setProfileElementEntities(profileElementEntities);
		Profile profile = new Profile(profileEntity);

		profile.applyAction(attributes, attributes, null, null, null, null);

		assertEquals("PrivateValue",
				attributes.getString(TagUtils.intFromHexString(StandardDICOM.cleanTagPath("(0021,1000)"))));
	}

	@Test
	void addPrivateTagWithExistingPrivateCreator_specifyPrivateCreator() {
		ProfileEntity profileEntity = new ProfileEntity();
		Attributes attributes = new Attributes();
		attributes.setString(Tag.Modality, VR.CS, "XA");
		attributes.setString(Tag.SOPClassUID, VR.UI, "1.2.840.10008.5.1.4.1.1.12.1");
		attributes.setString(TagUtils.intFromHexString(StandardDICOM.cleanTagPath("(0021,0010)")), VR.LO, "SIEMENS");

		Set<ProfileElementEntity> profileElementEntities = new HashSet<>();
		ProfileElementEntity profileElementEntityAddBurnedAttr = new ProfileElementEntity();
		profileElementEntityAddBurnedAttr.setCodename("action.add.private.tag");
		profileElementEntityAddBurnedAttr.setName("Add private tag");
		profileElementEntityAddBurnedAttr
			.addArgument(new ArgumentEntity("value", "PrivateValue", profileElementEntityAddBurnedAttr));
		profileElementEntityAddBurnedAttr
			.addArgument(new ArgumentEntity("vr", "LO", profileElementEntityAddBurnedAttr));
		profileElementEntityAddBurnedAttr
			.addArgument(new ArgumentEntity("privateCreator", "SIEMENS", profileElementEntityAddBurnedAttr));
		profileElementEntityAddBurnedAttr
			.addIncludedTag(new IncludedTagEntity("(0021,1000)", profileElementEntityAddBurnedAttr));
		profileElementEntityAddBurnedAttr.setPosition(1);

		profileElementEntities.add(profileElementEntityAddBurnedAttr);
		profileEntity.setProfileElementEntities(profileElementEntities);
		Profile profile = new Profile(profileEntity);

		profile.applyAction(attributes, attributes, null, null, null, null);

		assertEquals("PrivateValue",
				attributes.getString(TagUtils.intFromHexString(StandardDICOM.cleanTagPath("(0021,1000)"))));
	}

	@Test
	void addPrivateTagWithExistingPrivateCreator_PrivateCreatorCollision() {
		ProfileEntity profileEntity = new ProfileEntity();
		Attributes attributes = new Attributes();
		attributes.setString(Tag.Modality, VR.CS, "XA");
		attributes.setString(Tag.SOPClassUID, VR.UI, "1.2.840.10008.5.1.4.1.1.12.1");
		attributes.setString(TagUtils.intFromHexString(StandardDICOM.cleanTagPath("(0021,0010)")), VR.LO, "SIEMENS");

		Set<ProfileElementEntity> profileElementEntities = new HashSet<>();
		ProfileElementEntity profileElementEntityAddBurnedAttr = new ProfileElementEntity();
		profileElementEntityAddBurnedAttr.setCodename("action.add.private.tag");
		profileElementEntityAddBurnedAttr.setName("Add private tag");
		profileElementEntityAddBurnedAttr
			.addArgument(new ArgumentEntity("value", "PrivateValue", profileElementEntityAddBurnedAttr));
		profileElementEntityAddBurnedAttr
			.addArgument(new ArgumentEntity("vr", "LO", profileElementEntityAddBurnedAttr));
		profileElementEntityAddBurnedAttr
			.addArgument(new ArgumentEntity("privateCreator", "PHILIPS", profileElementEntityAddBurnedAttr));
		profileElementEntityAddBurnedAttr
			.addIncludedTag(new IncludedTagEntity("(0021,1000)", profileElementEntityAddBurnedAttr));
		profileElementEntityAddBurnedAttr.setPosition(1);

		profileElementEntities.add(profileElementEntityAddBurnedAttr);
		profileEntity.setProfileElementEntities(profileElementEntities);
		Profile profile = new Profile(profileEntity);

		profile.applyAction(attributes, attributes, null, null, null, null);

		assertNull(attributes.getString(TagUtils.intFromHexString(StandardDICOM.cleanTagPath("(0021,1000)"))));
		assertEquals("SIEMENS",
				attributes.getString(TagUtils.intFromHexString(StandardDICOM.cleanTagPath("(0021,0010)"))));
	}

}
