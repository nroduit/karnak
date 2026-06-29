/*
 * Copyright (c) 2020-2026 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.editor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.img.util.DicomObjectUtil;
import org.jspecify.annotations.Nullable;
import org.karnak.backend.model.Series;
import org.karnak.backend.model.SopInstance;
import org.karnak.backend.model.Study;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.param.AttributeEditorContext;
import org.weasis.dicom.param.AttributeEditorContext.Abort;
import org.weasis.dicom.param.DicomProgress;

@Slf4j
public class StreamRegistryEditor implements AttributeEditor {

	private final Map<String, Study> studyMap = new HashMap<>();

	@Getter
	@Setter
	private boolean enable = false;

	@Override
	public void apply(Attributes dcm, AttributeEditorContext context) {
		if (!enable) {
			return;
		}
		Study study = getOrCreateStudy(dcm);
		Series series = getOrCreateSeries(study, dcm);

		String sopUID = dcm.getString(Tag.SOPInstanceUID);
		if (series.getSopInstance(sopUID) == null) {
			SopInstance sopInstance = new SopInstance(sopUID);
			sopInstance.setSopClassUID(dcm.getString(Tag.SOPClassUID));
			series.addSopInstance(sopInstance);
		}
		else {
			context.setAbort(Abort.FILE_EXCEPTION);
			context.setAbortMessage("Duplicate transfer of " + sopUID);
		}
		// When it is a duplicate, avoid to send again a partial exam.
		study.setTimeStamp(System.currentTimeMillis());
	}

	private Study getOrCreateStudy(Attributes dcm) {
		String studyUID = dcm.getString(Tag.StudyInstanceUID);
		Study study = getStudy(studyUID);
		if (study == null) {
			study = new Study(studyUID, dcm.getString(Tag.PatientID));
			study.setOtherPatientIDs(dcm.getStrings(Tag.OtherPatientIDs));
			study.setAccessionNumber(dcm.getString(Tag.AccessionNumber));
			study.setStudyDescription(dcm.getString(Tag.StudyDescription, ""));
			study.setStudyDate(DicomObjectUtil.dateTime(dcm, Tag.StudyDate, Tag.StudyTime));
			addStudy(study);
		}
		return study;
	}

	private Series getOrCreateSeries(Study study, Attributes dcm) {
		String seriesUID = dcm.getString(Tag.SeriesInstanceUID);
		Series series = study.getSeries(seriesUID);
		if (series == null) {
			series = new Series(seriesUID);
			series.setSeriesDescription(dcm.getString(Tag.SeriesDescription, study.getStudyDescription()));
			LocalDateTime dateTime = DicomObjectUtil.dateTime(dcm, Tag.SeriesDate, Tag.SeriesTime);
			series.setSeriesDate(dateTime == null ? study.getStudyDate() : dateTime);
			study.addSeries(series);
		}
		return series;
	}

	public void addStudy(Study study) {
		if (study != null) {
			studyMap.put(study.getStudyInstanceUID(), study);
		}
	}

	public @Nullable Study removeStudy(String studyUID) {
		return studyMap.remove(studyUID);
	}

	public @Nullable Study getStudy(String studyUID) {
		return studyMap.get(studyUID);
	}

	public Set<Entry<String, Study>> getEntrySet() {
		return studyMap.entrySet();
	}

	public void update(DicomProgress progress) {
		if (enable) {
			Attributes dcm = progress.getAttributes();
			if (dcm != null) {
				String sopUID = dcm.getString(Tag.AffectedSOPInstanceUID);
				for (Entry<String, Study> stringStudyEntry : studyMap.entrySet()) {
					Study study = stringStudyEntry.getValue();
					for (Entry<String, Series> stringSeriesEntry : study.getEntrySet()) {
						Series series = stringSeriesEntry.getValue();
						SopInstance sopInstance = series.getSopInstance(sopUID);
						if (sopInstance != null) {
							sopInstance.setSent(!progress.isLastFailed());
							return;
						}
					}
				}
				log.error("sopUID [{}] doesn't exist for notify the state", sopUID);
			}
		}
	}

}
