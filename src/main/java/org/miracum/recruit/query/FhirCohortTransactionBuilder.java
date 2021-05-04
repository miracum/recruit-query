package org.miracum.recruit.query;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResearchStudy;
import org.hl7.fhir.r4.model.ResearchSubject;
import org.hl7.fhir.r4.model.StringType;
import org.miracum.recruit.query.models.CohortDefinition;
import org.miracum.recruit.query.models.Person;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FhirCohortTransactionBuilder {

  private static final String UUID_URN_PREFIX = "urn:uuid:";

  private static AdministrativeGender getGenderFromOmop(String gender) {
    if (gender == null) {
      return null;
    }
    switch (gender.toUpperCase()) {
      case "FEMALE":
        return AdministrativeGender.FEMALE;
      case "MALE":
        return AdministrativeGender.MALE;
      case "OTHER":
      case "AMBIGIOUS":
        return AdministrativeGender.OTHER;
      case "UNKNOWN":
      default:
        return AdministrativeGender.UNKNOWN;
    }
  }

  private static DateType parseBirthDate(Person person) {
    DateType date = new DateType();
    // if year of birth is present
    if (person.getYearOfBirth() != null) {
      if (person.getMonthOfBirth() != null) {
        if (person.getDayOfBirth() != null) {
          date.setPrecision(TemporalPrecisionEnum.DAY);
          date.setDay(person.getDayOfBirth());
        } else {
          // no day is available, so the maximum precision is month
          date.setPrecision(TemporalPrecisionEnum.MONTH);
        }
        date.setYear(person.getYearOfBirth().getValue());
        date.setMonth(person.getMonthOfBirth().getValue() - 1);
      } else {
        // no month is available, so the maximum precision is year
        date.setPrecision(TemporalPrecisionEnum.YEAR);
        date.setYear(person.getYearOfBirth().getValue());
      }
    }
    return date;
  }

  private final FhirSystems systems;

  private int maxListSize;
  private final boolean shouldNotCreateEncounters;
  private final VisitToEncounterMapper visitToEncounterMapper;

  private final LabelExtractor labelExtractor = new LabelExtractor();

  public FhirCohortTransactionBuilder(
      FhirSystems fhirSystems,
      @Value("${query.cohortSizeThreshold}") int cohortSizeThreshold,
      @Value("${query.excludePatientParameters.encounter}") boolean shouldNotCreateEncounters,
      VisitToEncounterMapper visitToEncounterMapper) {
    this.systems = fhirSystems;
    this.maxListSize = cohortSizeThreshold;
    this.visitToEncounterMapper = visitToEncounterMapper;
    this.shouldNotCreateEncounters = shouldNotCreateEncounters;
  }

  /**
   * Builds an FHIR Transaction with a list of ResearchSubjects from a given OMOP cohort includes
   * Patients, ResearchStudy, ResearchSubjects, List
   *
   * @param cohort OMOP CohortDefinition from an existing cohort
   * @param personsInCohort persons who should be packed to screening list
   * @param cohortSize number of persons in omop cohort
   * @return Bundle of type transaction
   */
  public Bundle buildFromOmopCohort(
      CohortDefinition cohort, List<Person> personsInCohort, long cohortSize) {
    String cohortId = cohort.getId().toString();
    String listId = "screeninglist-" + cohortId;

    // create BUNDLE
    Bundle transaction = new Bundle().setType(Bundle.BundleType.TRANSACTION);

    // create RESEARCHSTUDY and add to bundle
    ResearchStudy study = createResearchStudy(cohort);
    UUID studyUuid = UUID.randomUUID();
    transaction.addEntry(createStudyBundleEntryComponent(study, cohortId, studyUuid));

    // create SCREENINGLIST
    ListResource screeningList = createScreeninglist(listId, studyUuid);
    if (cohortSize > personsInCohort.size()) {
      screeningList.addNote(
          new Annotation()
              .setAuthor(new StringType("UC1-Query Module"))
              .setText(
                  "Es wurden mehr passende Patienten gefunden als auf dieser Liste dargestellt werden können (insgesamt "
                      + cohortSize
                      + "). Nur die ersten "
                      + this.maxListSize
                      + " Vorschläge werden angezeigt."));
    }
    // LOOP trough all Patients
    for (Person personInCohort : personsInCohort) {
      // create PATIENT with OMOP ID as an Identifier and add to bundle
      var patient = createPatient(personInCohort);
      var patientUuid = UUID.randomUUID();
      transaction.addEntry(createPatientBundleEntryComponent(patient, patientUuid));
      // create RESEARCHSUBJECT with Reference on Patient and Study and add to bundle
      var researchSubject = createResearchSubject(studyUuid, patientUuid);
      var subjectUuid = UUID.randomUUID();
      transaction.addEntry(
          createResearchSubjectBundleEntryComponent(
              researchSubject, subjectUuid, personInCohort.getPersonId(), cohortId));
      // add to SCREENINGLIST
      screeningList.addEntry(
          new ListResource.ListEntryComponent()
              .setItem(new Reference(UUID_URN_PREFIX + subjectUuid)));

      if (!shouldNotCreateEncounters) {
        var patientReference = new Reference(UUID_URN_PREFIX + patientUuid);
        var encounterBundleEntries =
            visitToEncounterMapper.map(personInCohort.getVisitOccurrences(), patientReference);
        for (var encounterBundleEntry : encounterBundleEntries) {
          transaction.addEntry(encounterBundleEntry);
        }
      }
    }
    // ADD LIST TO BUNDLE
    transaction.addEntry(createListBundleEntryComponent(screeningList, listId));

    return transaction;
  }

  // ------------------PRIVATE HELPER METHODS-------------------------//

  private BundleEntryComponent createListBundleEntryComponent(
      ListResource screeningList, String listId) {
    return new BundleEntryComponent()
        .setResource(screeningList)
        .setFullUrl(UUID_URN_PREFIX + UUID.randomUUID())
        .setRequest(
            new BundleEntryRequestComponent()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl("List?identifier=" + systems.getScreeningListIdentifier() + "|" + listId));
  }

  private Patient createPatient(Person personInCohort) {
    var patient =
        new Patient()
            .setBirthDateElement(parseBirthDate(personInCohort))
            .setGender(getGenderFromOmop(personInCohort.getGender()));
    if (StringUtils.isNotEmpty(personInCohort.getSourceId())) {
      patient.addIdentifier(
          new Identifier()
              .setSystem(systems.getPatientId())
              .setValue(personInCohort.getSourceId())
              .setType(
                  new CodeableConcept()
                      .addCoding(
                          new Coding().setSystem(systems.getIdentifierType()).setCode("MR"))));
    }
    patient.addIdentifier(
        new Identifier()
            .setSystem(systems.getOmopSubjectIdentifier())
            .setValue(personInCohort.getPersonId().toString()));
    return patient;
  }

  private BundleEntryComponent createPatientBundleEntryComponent(
      Patient patient, UUID patientUuid) {
    return new BundleEntryComponent()
        .setResource(patient)
        .setFullUrl(UUID_URN_PREFIX + patientUuid)
        .setRequest(
            new BundleEntryRequestComponent()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl(
                    "Patient?identifier="
                        + patient.getIdentifier().get(0).getSystem()
                        + "|"
                        + patient.getIdentifier().get(0).getValue()));
  }

  private ResearchStudy createResearchStudy(CohortDefinition cohort) {
    var study =
        new ResearchStudy()
            .setStatus(ResearchStudy.ResearchStudyStatus.ACTIVE)
            .addIdentifier(
                new Identifier()
                    .setSystem(systems.getOmopCohortIdentifier())
                    .setValue(cohort.getId().toString()));
    study.getMeta().setSource(systems.getStudySource());

    if (cohort.getName() != null) {
      var title = cohort.getName().replaceAll("\\[.*]", "").trim();
      study.setTitle(title);
    }

    if (cohort.getDescription() != null) {
      var description = cohort.getDescription().replaceAll("\\[.*]", "").trim();
      study.setDescription(description);
    }
    // Search in Description and then in Title for a description
    String acronym = labelExtractor.extractByTag("acronym", cohort.getDescription());
    if (acronym == null) {
      acronym = labelExtractor.extractByTag("acronym", cohort.getName());
    }
    if (acronym != null) {
      study.addExtension(systems.getResearchStudyAcronym(), new StringType(acronym));
    }

    return study;
  }

  // CREATE RESOURCES
  private ResearchSubject createResearchSubject(UUID studyUUID, UUID patientUuid) {
    return new ResearchSubject()
        .setStatus(ResearchSubject.ResearchSubjectStatus.CANDIDATE)
        .setStudy(new Reference(UUID_URN_PREFIX + studyUUID))
        .setIndividual(new Reference(UUID_URN_PREFIX + patientUuid));
  }

  private BundleEntryComponent createResearchSubjectBundleEntryComponent(
      ResearchSubject researchSubject, UUID subjectUuid, Long personId, String cohortId) {
    return new BundleEntryComponent()
        .setResource(researchSubject)
        .setFullUrl(UUID_URN_PREFIX + subjectUuid)
        .setRequest(
            new BundleEntryRequestComponent()
                .setMethod(Bundle.HTTPVerb.POST)
                .setIfNoneExist(
                    "patient.identifier="
                        + systems.getOmopSubjectIdentifier()
                        + "|"
                        + personId
                        + "&"
                        + "study.identifier="
                        + systems.getOmopCohortIdentifier()
                        + "|"
                        + cohortId)
                .setUrl("ResearchSubject"));
  }

  private ListResource createScreeninglist(String listId, UUID studyUuid) {
    var list =
        new ListResource()
            .setStatus(ListResource.ListStatus.CURRENT)
            .setMode(ListResource.ListMode.WORKING)
            .setCode(
                new CodeableConcept()
                    .addCoding(
                        new Coding()
                            .setSystem(systems.getScreeningListCoding())
                            .setCode("screening-recommendations")))
            .addIdentifier(
                new Identifier().setSystem(systems.getScreeningListIdentifier()).setValue(listId));

    // add Study to screeninglist as an extension
    list.addExtension(
        new Extension()
            .setUrl(systems.getScreeningListStudyReferenceExtension())
            .setValue(new Reference(UUID_URN_PREFIX + studyUuid)));
    return list;
  }

  private BundleEntryComponent createStudyBundleEntryComponent(
      ResearchStudy study, String cohortId, UUID studyUuid) {
    return new BundleEntryComponent()
        .setResource(study)
        .setFullUrl(UUID_URN_PREFIX + studyUuid)
        .setRequest(
            new BundleEntryRequestComponent()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl(
                    "ResearchStudy?identifier="
                        + systems.getOmopCohortIdentifier()
                        + "|"
                        + cohortId));
  }

  public FhirSystems getSystems() {
    return this.systems;
  }

  // --------------------------------------GETTERS AND
  // SETTERS-------------------------------------------------------------//
  public void setMaxListSize(int maxListSize) {
    this.maxListSize = maxListSize;
  }
}
