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
  private String localIdentifier;
  private String localIdentifierType;

  public String getLocalIdentifier() {
    return localIdentifier;
  }

  public String getLocalIdentifierType() {
    return localIdentifierType;
  }

  public String getOmopCohortIdentifier() {
    return omopCohortIdentifier;
  }

  public String getOmopSubjectIdentifier() {
    return omopSubjectIdentifier;
  }

  public String getResearchStudyAcronym() {
    return researchStudyAcronym;
  }

  public String getScreeningListCoding() {
    return screeningListCoding;
  }

  public String getScreeningListIdentifier() {
    return screeningListIdentifier;
  }

  public String getScreeningListStudyReferenceExtension() {
    return screeningListStudyReferenceExtension;
  }

  public String getStudySource() {
    return studySource;
  }

  public void setLocalIdentifier(String localIdentifier) {
    this.localIdentifier = localIdentifier;
  }

  public void setLocalIdentifierType(String localIdentifierType) {
    this.localIdentifierType = localIdentifierType;
  }

  public void setOmopCohortIdentifier(String omopCohortIdentifier) {
    this.omopCohortIdentifier = omopCohortIdentifier;
  }

  public void setOmopSubjectIdentifier(String omopSubjectIdentifier) {
    this.omopSubjectIdentifier = omopSubjectIdentifier;
  }

  public void setResearchStudyAcronym(String researchStudyAcronym) {
    this.researchStudyAcronym = researchStudyAcronym;
  }

  public void setScreeningListCoding(String screeningListCoding) {
    this.screeningListCoding = screeningListCoding;
  }

  public void setScreeningListIdentifier(String screeningListIdentifier) {
    this.screeningListIdentifier = screeningListIdentifier;
  }

  public void setScreeningListStudyReferenceExtension(String screeningListStudyReferenceExtension) {
    this.screeningListStudyReferenceExtension = screeningListStudyReferenceExtension;
  }

  public void setStudySource(String studySource) {
    this.studySource = studySource;
  }
}
