/*
 * Copyright (c) 2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.profiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import org.karnak.backend.exception.ProfileException;
import org.karnak.backend.service.profilepipe.Profile;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Adding an attribute inside a sequence. The fixture is an X-Ray Angiographic image,
 * whose SOP class holds ScheduledProcedureStepID (0040,0009) inside
 * RequestAttributesSequence (0040,0275).
 */
@SpringBootTest
@DisplayNameGeneration(ReplaceUnderscores.class)
class AddTagInSequenceTest {

	private static final String XA_SOP_CLASS = "1.2.840.10008.5.1.4.1.1.12.1";

	private static final String STEP_ID_IN_REQUEST = "(0040,0275).(0040,0009)";

	private static final String ADDED_VALUE = "STEP-1";

	/** An X-Ray Angiographic instance, without RequestAttributesSequence. */
	private static Attributes instance() {
		Attributes dcm = new Attributes();
		dcm.setString(Tag.SOPClassUID, VR.UI, XA_SOP_CLASS);
		dcm.setString(Tag.SOPInstanceUID, VR.UI, "1.2.3.4.5");
		dcm.setString(Tag.Modality, VR.CS, "XA");
		return dcm;
	}

	/**
	 * The same instance, with a RequestAttributesSequence holding {@code items} items.
	 */
	private static Attributes instanceWithRequestAttributes(int items) {
		Attributes dcm = instance();
		Sequence sequence = dcm.newSequence(Tag.RequestAttributesSequence, items);
		for (int i = 0; i < items; i++) {
			Attributes item = new Attributes();
			item.setString(Tag.RequestedProcedureID, VR.SH, "PROC-" + i);
			sequence.add(item);
		}
		return dcm;
	}

	private static ProfileElementEntity addTagElement(String tagValue, String value) {
		ProfileElementEntity element = new ProfileElementEntity("Add " + tagValue, "action.add.tag", null, null, null,
				0, null);
		element.addArgument(new ArgumentEntity("value", value, element));
		element.addIncludedTag(new IncludedTagEntity(tagValue, element));
		return element;
	}

	private static Profile profileOf(ProfileElementEntity... elements) {
		ProfileEntity profileEntity = new ProfileEntity();
		profileEntity.setProfileElementEntities(Set.of(elements));
		return new Profile(profileEntity);
	}

	private static void apply(Profile profile, Attributes dcm) {
		profile.applyAction(dcm, new Attributes(dcm), null, null, null, null);
	}

	private static List<Attributes> requestItems(Attributes dcm) {
		Sequence sequence = dcm.getSequence(Tag.RequestAttributesSequence);
		return sequence == null ? List.of() : List.copyOf(sequence);
	}

	@Nested
	class Adding {

		@Test
		void writes_the_tag_inside_an_existing_sequence_item() {
			Attributes dcm = instanceWithRequestAttributes(1);

			apply(profileOf(addTagElement(STEP_ID_IN_REQUEST, ADDED_VALUE)), dcm);

			assertEquals(ADDED_VALUE, requestItems(dcm).getFirst().getString(Tag.ScheduledProcedureStepID));
			// The attribute belongs to the sequence item, not to the dataset
			assertNull(dcm.getString(Tag.ScheduledProcedureStepID));
		}

		@Test
		void creates_the_sequence_when_the_object_does_not_hold_it() {
			Attributes dcm = instance();

			apply(profileOf(addTagElement(STEP_ID_IN_REQUEST, ADDED_VALUE)), dcm);

			assertNotNull(dcm.getSequence(Tag.RequestAttributesSequence));
			assertEquals(1, requestItems(dcm).size());
			assertEquals(ADDED_VALUE, requestItems(dcm).getFirst().getString(Tag.ScheduledProcedureStepID));
		}

		@Test
		void writes_the_tag_in_every_item_of_the_sequence() {
			Attributes dcm = instanceWithRequestAttributes(3);

			apply(profileOf(addTagElement(STEP_ID_IN_REQUEST, ADDED_VALUE)), dcm);

			assertEquals(3, requestItems(dcm).size());
			requestItems(dcm).forEach(item -> assertEquals(ADDED_VALUE, item.getString(Tag.ScheduledProcedureStepID)));
			// The items keep what they already held
			assertEquals("PROC-0", requestItems(dcm).getFirst().getString(Tag.RequestedProcedureID));
		}

		@Test
		void leaves_an_item_that_already_holds_the_tag_untouched() {
			Attributes dcm = instanceWithRequestAttributes(1);
			requestItems(dcm).getFirst().setString(Tag.ScheduledProcedureStepID, VR.SH, "ALREADY-THERE");

			apply(profileOf(addTagElement(STEP_ID_IN_REQUEST, ADDED_VALUE)), dcm);

			assertEquals("ALREADY-THERE", requestItems(dcm).getFirst().getString(Tag.ScheduledProcedureStepID));
		}

		@Test
		void adds_nothing_when_the_destination_is_not_part_of_the_sop_class() {
			Attributes dcm = instanceWithRequestAttributes(1);

			// PatientName is not defined inside RequestAttributesSequence
			apply(profileOf(addTagElement("(0040,0275).(0010,0010)", "TEST^PATIENT")), dcm);

			assertNull(requestItems(dcm).getFirst().getString(Tag.PatientName));
		}

		@Test
		void keeps_the_added_value_out_of_reach_of_the_other_profile_items() {
			// Removing (0040,0009) is applied while walking the object, the add once the
			// walk is over: what the profile adds is what is forwarded
			ProfileElementEntity remove = new ProfileElementEntity("Remove step id", "action.on.specific.tags", null,
					"X", null, 1, null);
			remove.addIncludedTag(new IncludedTagEntity("(0040,0009)", remove));
			Attributes dcm = instanceWithRequestAttributes(1);
			requestItems(dcm).getFirst().setString(Tag.ScheduledProcedureStepID, VR.SH, "TO-BE-REMOVED");

			apply(profileOf(remove, addTagElement(STEP_ID_IN_REQUEST, ADDED_VALUE)), dcm);

			assertEquals(ADDED_VALUE, requestItems(dcm).getFirst().getString(Tag.ScheduledProcedureStepID));
		}

		@Test
		void still_adds_a_plain_tag_to_the_top_level_dataset() {
			Attributes dcm = instance();

			apply(profileOf(addTagElement("(0028,0301)", "YES")), dcm);

			assertEquals("YES", dcm.getString(Tag.BurnedInAnnotation));
		}

	}

	@Nested
	class Validation {

		private static ProfileException buildFailure(String tagValue) {
			return assertThrows(ProfileException.class, () -> new AddTag(addTagElement(tagValue, ADDED_VALUE)));
		}

		@Test
		void rejects_a_path_holding_a_wildcard_level() {
			assertTrue(buildFailure("**.(0040,0009)").getMessage().contains("wildcard"));
		}

		@Test
		void rejects_a_path_holding_a_wildcard_tag() {
			assertTrue(buildFailure("(0040,0275).0040XXXX").getMessage().contains("wildcard"));
		}

		@Test
		void rejects_an_enclosing_tag_that_is_not_a_sequence() {
			// PatientName is not a sequence, nothing can be nested in it
			assertTrue(buildFailure("(0010,0010).(0040,0009)").getMessage().contains("not a sequence"));
		}

		@Test
		void accepts_a_literal_path_of_sequences() {
			assertTrue(new AddTag(addTagElement(STEP_ID_IN_REQUEST, ADDED_VALUE)).targetsSequence());
		}

		@Test
		void does_not_treat_an_anchored_plain_tag_as_a_sequence_target() throws ProfileException {
			assertEquals(false, new AddTag(addTagElement(".(0028,0301)", "YES")).targetsSequence());
		}

	}

}