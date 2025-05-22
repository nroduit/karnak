package org.karnak.backend.service;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.karnak.backend.data.entity.ArgumentEntity;
import org.karnak.backend.data.entity.IncludedTagEntity;
import org.karnak.backend.data.entity.ProfileElementEntity;
import org.karnak.backend.data.entity.ProfileEntity;
import static org.karnak.backend.service.EndpointService.evaluateStringWithExpression;

public class EndpointServiceTest {

	@Test
	void test_replaceURL_noParameters() {
		String testUrl = "http://example.com/endpoint";

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "action.replace.api", null, null,
				null, 0, profileEntity);
		profileElementEntity.addArgument(new ArgumentEntity("url", testUrl, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("responsePath", "path", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("authConfig", "endpoint", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("defaultValue", "dummy", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,0020)", profileElementEntity));
		profileElementEntity.setProfileEntity(profileEntity);

		Attributes attributes = new Attributes();
		attributes.setString(Tag.PatientID, VR.LO, "1234");
		assertEquals(testUrl, evaluateStringWithExpression(testUrl, attributes));
	}

	@Test
	void test_replaceURL_withParameters() {
		String testUrl = "http://example.com/{{getString(#Tag.PatientID)}}/endpoint";

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "action.replace.api", null, null,
				null, 0, profileEntity);
		profileElementEntity.addArgument(new ArgumentEntity("url", testUrl, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("responsePath", "path", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("authConfig", "endpoint", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("defaultValue", "dummy", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,0020)", profileElementEntity));
		profileElementEntity.setProfileEntity(profileEntity);

		Attributes attributes = new Attributes();
		attributes.setString(Tag.PatientID, VR.LO, "1234");
		assertEquals("http://example.com/1234/endpoint", evaluateStringWithExpression(testUrl, attributes));
	}

	@Test
	void test_replaceURL_withMultipleParameters() {
		String testUrl = "http://example.com/{{getString(#Tag.ClinicalTrialSponsorName)}}/patient/{{getString(#Tag.PatientID)}}/endpoint";

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "action.replace.api", null, null,
				null, 0, profileEntity);
		profileElementEntity.addArgument(new ArgumentEntity("url", testUrl, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("responsePath", "path", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("authConfig", "endpoint", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("defaultValue", "dummy", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,0020)", profileElementEntity));
		profileElementEntity.setProfileEntity(profileEntity);

		Attributes attributes = new Attributes();
		attributes.setString(Tag.PatientID, VR.LO, "1234");
		attributes.setString(Tag.ClinicalTrialSponsorName, VR.LO, "project_hug_research");
		assertEquals("http://example.com/project_hug_research/patient/1234/endpoint",
				evaluateStringWithExpression(testUrl, attributes));
	}

	@Test
	void test_replaceBody_noParameters() {
		String testBody = "{ \"patientId\": 22 }";

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "action.replace.api", null, null,
				null, 0, profileEntity);
		profileElementEntity
			.addArgument(new ArgumentEntity("url", "http://example.com/endpoint", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("responsePath", "path", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("method", "post", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("body", testBody, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("authConfig", "endpoint", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("defaultValue", "dummy", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,0020)", profileElementEntity));
		profileElementEntity.setProfileEntity(profileEntity);

		Attributes attributes = new Attributes();
		attributes.setString(Tag.PatientID, VR.LO, "1234");
		attributes.setString(Tag.ClinicalTrialSponsorName, VR.LO, "project_hug_research");
		assertEquals(testBody, evaluateStringWithExpression(testBody, attributes));
	}

	@Test
	void test_replaceBody_withParameters() {
		String testBody = "{ \"patientId\": {{getString(#Tag.PatientID)}} }";

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "action.replace.api", null, null,
				null, 0, profileEntity);
		profileElementEntity
			.addArgument(new ArgumentEntity("url", "http://example.com/endpoint", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("responsePath", "path", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("method", "post", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("body", testBody, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("authConfig", "endpoint", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("defaultValue", "dummy", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,0020)", profileElementEntity));
		profileElementEntity.setProfileEntity(profileEntity);

		Attributes attributes = new Attributes();
		attributes.setString(Tag.PatientID, VR.LO, "1234");
		attributes.setString(Tag.ClinicalTrialSponsorName, VR.LO, "project_hug_research");
		assertEquals("{ \"patientId\": 1234 }", evaluateStringWithExpression(testBody, attributes));
	}

	@Test
	void test_replaceBody_withMultipleParameters() {
		String testBody = "{ \"patientId\": {{getString(#Tag.PatientID)}}, \"project\": \"{{getString(#Tag.ClinicalTrialSponsorName)}}\" }";

		ProfileEntity profileEntity = new ProfileEntity("TEST", "0.9.1", "0.9.1", "DPA");
		ProfileElementEntity profileElementEntity = new ProfileElementEntity("Expr", "action.replace.api", null, null,
				null, 0, profileEntity);
		profileElementEntity
			.addArgument(new ArgumentEntity("url", "http://example.com/endpoint", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("responsePath", "path", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("method", "post", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("body", testBody, profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("authConfig", "endpoint", profileElementEntity));
		profileElementEntity.addArgument(new ArgumentEntity("defaultValue", "dummy", profileElementEntity));
		profileElementEntity.addIncludedTag(new IncludedTagEntity("(0010,0020)", profileElementEntity));
		profileElementEntity.setProfileEntity(profileEntity);

		Attributes attributes = new Attributes();
		attributes.setString(Tag.PatientID, VR.LO, "1234");
		attributes.setString(Tag.ClinicalTrialSponsorName, VR.LO, "project_hug_research");
		assertEquals("{ \"patientId\": 1234, \"project\": \"project_hug_research\" }",
				evaluateStringWithExpression(testBody, attributes));
	}

}
