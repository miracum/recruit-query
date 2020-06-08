package org.miracum.recruit.query;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.yml")
@ConfigurationProperties(prefix = "fhir.systems")
public class FhirSystems {
    private String omopSubjectIdentifier;
    private String omopCohortIdentifier;
    private String screeningListIdentifier;
    private String screeningListStudyReferenceExtension;
    private String researchStudyAcronym;
    private String screeningListCoding;
    private String studySource;

    public String getOmopSubjectIdentifier() {
        return omopSubjectIdentifier;
    }

    public void setOmopSubjectIdentifier(String omopSubjectIdentifier) {
        this.omopSubjectIdentifier = omopSubjectIdentifier;
    }

    public String getOmopCohortIdentifier() {
        return omopCohortIdentifier;
    }

    public void setOmopCohortIdentifier(String omopCohortIdentifier) {
        this.omopCohortIdentifier = omopCohortIdentifier;
    }

    public String getScreeningListIdentifier() {
        return screeningListIdentifier;
    }

    public void setScreeningListIdentifier(String screeningListIdentifier) {
        this.screeningListIdentifier = screeningListIdentifier;
    }

    public String getScreeningListStudyReferenceExtension() {
        return screeningListStudyReferenceExtension;
    }

    public void setScreeningListStudyReferenceExtension(String screeningListStudyReferenceExtension) {
        this.screeningListStudyReferenceExtension = screeningListStudyReferenceExtension;
    }

    public String getResearchStudyAcronym() {
        return researchStudyAcronym;
    }

    public void setResearchStudyAcronym(String researchStudyAcronym) {
        this.researchStudyAcronym = researchStudyAcronym;
    }

    public String getScreeningListCoding() {
        return screeningListCoding;
    }

    public void setScreeningListCoding(String screeningListCoding) {
        this.screeningListCoding = screeningListCoding;
    }

    public String getStudySource() {
        return studySource;
    }

    public void setStudySource(String studySource) {
        this.studySource = studySource;
    }
}
