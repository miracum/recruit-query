package org.miracum.recruit.query;

import com.google.common.base.Strings;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.miracum.recruit.query.models.CareSite;
import org.miracum.recruit.query.models.VisitDetail;
import org.miracum.recruit.query.models.VisitOccurrence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VisitToEncounterMapper {
  private static final Logger LOG = LoggerFactory.getLogger(VisitToEncounterMapper.class);

  private static final String UUID_URN_PREFIX = "urn:uuid:";
  private static final Integer VISIT_TYPE_CONCEPT_STILL_PATIENT = 32220;
  private static final Integer VISIT_CONCEPT_IN_PATIENT = 9201;
  private static final Integer VISIT_CONCEPT_OUT_PATIENT = 9202;
  private static final Integer VISIT_CONCEPT_EMERGENCY_ROOM = 9203;

  private final Coding impCoding;
  private final FhirSystems fhirSystems;
  private final Map<Integer, Coding> visitConceptToEncounterClassMap;

  public VisitToEncounterMapper(FhirSystems fhirSystems) {
    this.fhirSystems = fhirSystems;
    this.impCoding = new Coding(fhirSystems.getActEncounterCode(), "IMP", "inpatient encounter");

    visitConceptToEncounterClassMap = new HashMap<>();
    visitConceptToEncounterClassMap.put(
        VISIT_CONCEPT_IN_PATIENT,
        new Coding(fhirSystems.getActEncounterCode(), "IMP", "inpatient encounter"));
    visitConceptToEncounterClassMap.put(
        VISIT_CONCEPT_OUT_PATIENT,
        new Coding(fhirSystems.getActEncounterCode(), "AMB", "ambulatory"));
    visitConceptToEncounterClassMap.put(
        VISIT_CONCEPT_EMERGENCY_ROOM,
        new Coding(fhirSystems.getActEncounterCode(), "EMER", "emergency"));
  }

  public Collection<BundleEntryComponent> map(
      Collection<VisitOccurrence> visitOccurrences, Reference patientReference) {
    if (visitOccurrences == null) {
      return Collections.emptyList();
    }
    return visitOccurrences.stream()
        .flatMap(v -> map(v, patientReference).stream())
        .collect(Collectors.toList());
  }

  private Collection<BundleEntryComponent> map(
      VisitOccurrence visitOccurrence, Reference patientReference) {
    if (Strings.isNullOrEmpty(visitOccurrence.getVisitSourceValue())) {
      LOG.error("Given encounter does not have its source_value set. Not processing.");
      return Collections.emptyList();
    }

    var result = new ArrayList<BundleEntryComponent>();

    var mainEncounterEntryComponent =
        mapVisitOccurrenceToMainEncounter(visitOccurrence, patientReference);

    result.add(mainEncounterEntryComponent);

    var mainEncounterReference = new Reference(mainEncounterEntryComponent.getFullUrl());

    for (var visitDetail : visitOccurrence.getVisitDetails()) {
      var subEncounter =
          mapVisitDetailToSubEncounter(
              visitDetail, visitOccurrence, patientReference, mainEncounterReference);

      result.add(subEncounter);
    }

    return result;
  }

  private BundleEntryComponent mapVisitOccurrenceToMainEncounter(
      VisitOccurrence visitOccurrence, Reference patientReference) {

    var mainEncounterFullUrl = UUID_URN_PREFIX + UUID.randomUUID();

    // both status and class are required fields so they should be filled as soon as possible
    // to ensure validation passes.
    var mainEncounter = new Encounter().setStatus(EncounterStatus.UNKNOWN).setClass_(impCoding);
    var period = new Period();

    if (visitOccurrence.getVisitStartDate() != null) {
      period.setStart(Date.valueOf(visitOccurrence.getVisitStartDate()));
    } else {
      // TODO: cleanup using logback for key-value based structured logging
      LOG.debug(
          "visit start date not set for visitSourceValue={}",
          visitOccurrence.getVisitSourceValue());
    }

    if (visitOccurrence.getVisitTypeConceptId() != null) {
      if (visitOccurrence.getVisitTypeConceptId().equals(VISIT_TYPE_CONCEPT_STILL_PATIENT)) {
        mainEncounter.setStatus(EncounterStatus.INPROGRESS);
      } else {
        mainEncounter.setStatus(EncounterStatus.FINISHED);
        if (visitOccurrence.getVisitEndDate() != null) {
          period.setEnd(Date.valueOf(visitOccurrence.getVisitEndDate()));
        }
      }
    }

    mainEncounter.setPeriod(period);

    var encounterClass =
        visitConceptToEncounterClassMap.getOrDefault(
            visitOccurrence.getVisitConceptId(), impCoding);
    mainEncounter.setClass_(encounterClass);

    mainEncounter
        .addIdentifier()
        .setSystem(fhirSystems.getEncounterId())
        .setType(
            new CodeableConcept()
                .addCoding(new Coding().setSystem(fhirSystems.getIdentifierType()).setCode("VN")))
        .setValue(visitOccurrence.getVisitSourceValue());

    mainEncounter.setSubject(patientReference);

    if (visitOccurrence.getCareSite() != null) {
      var locationReference = createReferenceFromCareSite(visitOccurrence.getCareSite());
      mainEncounter.addLocation().setLocation(locationReference);
    }

    return new BundleEntryComponent()
        .setResource(mainEncounter)
        .setFullUrl(mainEncounterFullUrl)
        .setRequest(
            new BundleEntryRequestComponent()
                .setMethod(Bundle.HTTPVerb.POST)
                .setIfNoneExist(
                    String.format(
                        "identifier=%s|%s",
                        mainEncounter.getIdentifierFirstRep().getSystem(),
                        mainEncounter.getIdentifierFirstRep().getValue()))
                .setUrl("Encounter"));
  }

  private BundleEntryComponent mapVisitDetailToSubEncounter(
      VisitDetail visitDetail,
      VisitOccurrence visitOccurrence,
      Reference patientReference,
      Reference mainEncounterReference) {
    var subEncounter = new Encounter().setStatus(EncounterStatus.UNKNOWN).setClass_(impCoding);

    var period = new Period();

    if (visitDetail.getVisitDetailStartDate() != null) {
      period.setStart(Date.valueOf(visitDetail.getVisitDetailStartDate()));
    } else {
      LOG.debug(
          "visit start date not set for visitSourceValue={} and visitDetailSourceValue={}",
          visitOccurrence.getVisitSourceValue(),
          visitDetail.getVisitDetailSourceValue());
    }

    if (visitDetail.getVisitDetailTypeConceptId() != null) {
      if (visitDetail.getVisitDetailTypeConceptId().equals(VISIT_TYPE_CONCEPT_STILL_PATIENT)) {
        subEncounter.setStatus(EncounterStatus.INPROGRESS);
      } else {
        subEncounter.setStatus(EncounterStatus.FINISHED);
        if (visitDetail.getVisitDetailEndDate() != null) {
          period.setEnd(Date.valueOf(visitDetail.getVisitDetailEndDate()));
        }
      }
    }

    subEncounter.setPeriod(period);

    var encounterClass =
        visitConceptToEncounterClassMap.getOrDefault(
            visitDetail.getVisitDetailTypeConceptId(), impCoding);
    subEncounter.setClass_(encounterClass);

    if (visitDetail.getCareSite() != null) {
      var locationReference = createReferenceFromCareSite(visitDetail.getCareSite());
      subEncounter.addLocation().setLocation(locationReference);
    }

    // the visit_detail table does not contain stable identification data as opposed to the
    // visit_occurrence's visit_source_value. So we'll have to create a surrogate identifier
    // from the visit_source_value, the visit_detail's start date and the visit_detail_source_value
    // which should at least approximate the location of the visit to some extent.
    var identifierValue =
        String.format(
            "%s-%s-%s",
            visitOccurrence.getVisitSourceValue(),
            visitDetail.getVisitDetailStartDate(),
            visitDetail.getVisitDetailSourceValue());

    subEncounter.setSubject(patientReference);
    subEncounter.setPartOf(mainEncounterReference);
    subEncounter
        .addIdentifier()
        .setType(
            new CodeableConcept()
                .addCoding(new Coding().setSystem(fhirSystems.getIdentifierType()).setCode("VN")))
        .setSystem(fhirSystems.getSubEncounterId())
        .setValue(identifierValue);

    return new BundleEntryComponent()
        .setResource(subEncounter)
        .setFullUrl(UUID_URN_PREFIX + UUID.randomUUID())
        .setRequest(
            new BundleEntryRequestComponent()
                .setMethod(Bundle.HTTPVerb.POST)
                .setIfNoneExist(
                    String.format(
                        "identifier=%s|%s",
                        subEncounter.getIdentifierFirstRep().getSystem(),
                        subEncounter.getIdentifierFirstRep().getValue()))
                .setUrl("Encounter"));
  }

  private static Reference createReferenceFromCareSite(CareSite careSite) {
    return new Reference()
        .setType(ResourceType.Location.name())
        .setDisplay(careSite.getCareSiteName());
  }
}
