/*
 * Copyright (c) 2020-2021 Karnak Team and other contributors.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0, or the Apache
 * License, Version 2.0 which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 */
package org.karnak.backend.model.editor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.karnak.backend.dicom.DateTimeUtils;
import org.karnak.backend.model.Series;
import org.karnak.backend.model.SopInstance;
import org.karnak.backend.model.Study;
import org.weasis.dicom.param.AttributeEditor;
import org.weasis.dicom.param.AttributeEditorContext;
import org.weasis.dicom.param.AttributeEditorContext.Abort;
import org.weasis.dicom.param.DicomProgress;
import org.weasis.dicom.util.DateUtil;

@Slf4j
public class StreamRegistryEditor implements AttributeEditor {

	private final Map<String, Study> studyMap = new HashMap<>();

	private boolean enable = false;

	public StreamRegistryEditor() {
	}

	private static LocalDateTime getDateTime(Attributes dicom, int date, int time) {
		LocalDate d = DateUtil.getDicomDate(dicom.getString(date));
		LocalTime t = DateUtil.getDicomTime(dicom.getString(time));
		return DateTimeUtils.dateTime(d, t);
	}

	@Override
	public void apply(Attributes dcm, AttributeEditorContext context) {
		if (enable) {
			String studyUID = dcm.getString(Tag.StudyInstanceUID);
			Study study = getStudy(studyUID);
			if (study == null) {
				study = new Study(studyUID, dcm.getString(Tag.PatientID));
				study.setOtherPatientIDs(dcm.getStrings(Tag.OtherPatientIDs));
				study.setAccessionNumber(dcm.getString(Tag.AccessionNumber));
				study.setStudyDescription(dcm.getString(Tag.StudyDescription, ""));
				study.setStudyDate(getDateTime(dcm, Tag.StudyDate, Tag.StudyTime));
				addStudy(study);
			}

			String seriesUID = dcm.getString(Tag.SeriesInstanceUID);
			Series series = study.getSeries(seriesUID);
			if (series == null) {
				series = new Series(seriesUID);
				series.setSeriesDescription(dcm.getString(Tag.SeriesDescription, study.getStudyDescription()));
				LocalDateTime dateTime = getDateTime(dcm, Tag.SeriesDate, Tag.SeriesTime);
				series.setSeriesDate(dateTime == null ? study.getStudyDate() : dateTime);
				study.addSeries(series);
			}

			String sopUID = dcm.getString(Tag.SOPInstanceUID);
			SopInstance sopInstance = series.getSopInstance(sopUID);
			if (sopInstance == null) {
				sopInstance = new SopInstance(sopUID);
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
	}

	public void addStudy(Study study) {
		if (study != null) {
			studyMap.put(study.getStudyInstanceUID(), study);
		}
	}

	public Study removeStudy(String studyUID) {
		return studyMap.remove(studyUID);
	}

	public Study getStudy(String studyUID) {
		return studyMap.get(studyUID);
	}

	public Set<Entry<String, Study>> getEntrySet() {
		return studyMap.entrySet();
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public void update(DicomProgress progress) {
		if (enable) {
			Attributes dcm = progress.getAttributes();
			if (dcm != null) {
				String sopUID = dcm.getString(Tag.AffectedSOPInstanceUID);
				Iterator<Entry<String, Study>> studyIt = studyMap.entrySet().iterator();
				while (studyIt.hasNext()) {
					Study study = studyIt.next().getValue();
					Iterator<Entry<String, Series>> seriesIt = study.getEntrySet().iterator();
					while (seriesIt.hasNext()) {
						Series series = seriesIt.next().getValue();
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
