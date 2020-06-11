package org.miracum.recruit.query;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryRequestComponent;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.miracum.recruit.query.models.CohortDefinition;
import org.miracum.recruit.query.models.OmopPerson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class FhirCohortTransactionBuilder {
    private static final String UUID_URN_PREFIX = "urn:uuid:";

    private final FhirSystems systems;

    @Autowired
    public FhirCohortTransactionBuilder(FhirSystems fhirSystems) {
        systems = fhirSystems;
    }

    private static AdministrativeGender getGenderFromOmop(String gender) {
        if (gender == null) return AdministrativeGender.NULL;
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

    private static DateType parseBirthDate(OmopPerson person) {
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

    /**
     * Builds an FHIR Transaction with a list of ResearchSubjects from a given OMOP cohort
     * includes Patients, ResearchStudy, ResearchSubjects, List
     *
     * @param cohort          OMOP CohortDefinition from an existing cohort
     * @param personsInCohort all persons in cohort
     * @return Bundle of type transaction
     */
    public Bundle buildFromOmopCohort(CohortDefinition cohort, List<OmopPerson> personsInCohort) {
        String cohortId = cohort.getId().toString();
        String listId = "screeninglist-" + cohortId;

        //create BUNDLE
        Bundle transaction = new Bundle().setType(Bundle.BundleType.TRANSACTION);

        // create RESEARCHSTUDY and add to bundle
        ResearchStudy study = createResearchStudy(cohort);
        UUID studyUuid = UUID.randomUUID();
        transaction.addEntry(createStudyBundleEntryComponent(study, cohortId, studyUuid));

        //create SCREENINGLIST
        ListResource screeningList = createScreeninglist(listId, studyUuid);

        //LOOP trough all Patients
        for (OmopPerson personInCohort : personsInCohort) {
            // create PATIENT with OMOP ID as an Identifier and add to bundle
            var patient = createPatient(personInCohort);
            var patientUuid = UUID.randomUUID();
            transaction.addEntry(createPatientBundleEntryComponent(patient, patientUuid));
            // create RESEARCHSUBJECT with Reference on Patient and Study and add to bundle
            var researchSubject = createResearchSubject(studyUuid, patientUuid);
            var subjectUuid = UUID.randomUUID();
            transaction.addEntry(createResearchSubjectBundleEntryComponent(researchSubject, subjectUuid, personInCohort.getPersonId(), cohortId));
            // add to SCREENINGLIST
            screeningList.addEntry(new ListResource.ListEntryComponent()
                    .setItem(new Reference(UUID_URN_PREFIX + subjectUuid)));
        }
        //ADD LIST TO BUNDLE
        transaction.addEntry(createListBundleEntryComponent(screeningList, listId));

        return transaction;
    }

    private BundleEntryComponent createStudyBundleEntryComponent(ResearchStudy study, String cohortId, UUID studyUuid) {
        return new BundleEntryComponent()
                .setResource(study)
                .setFullUrl(UUID_URN_PREFIX + studyUuid)
                .setRequest(new BundleEntryRequestComponent()
                        .setMethod(Bundle.HTTPVerb.PUT)
                        .setUrl("ResearchStudy?identifier=" + systems.getOmopCohortIdentifier() + "|" + cohortId));
    }

    private BundleEntryComponent createResearchSubjectBundleEntryComponent(ResearchSubject researchSubject, UUID subjectUuid, int personId, String cohortId) {
        return new BundleEntryComponent()
                .setResource(researchSubject)
                .setFullUrl("urn:uuid:" + subjectUuid)
                .setRequest(new BundleEntryRequestComponent()
                        .setMethod(Bundle.HTTPVerb.PUT)
                        .setUrl("ResearchSubject?patient.identifier=" + systems.getOmopSubjectIdentifier() + "|" + personId
                                + "&" + "study.identifier=" + systems.getOmopCohortIdentifier() + "|" + cohortId));
    }

    private BundleEntryComponent createPatientBundleEntryComponent(Patient patient, UUID patientUuid) {
        return new BundleEntryComponent()
                .setResource(patient)
                .setFullUrl("urn:uuid:" + patientUuid)
                .setRequest(new BundleEntryRequestComponent()
                        .setMethod(Bundle.HTTPVerb.PUT)
                        .setUrl("Patient?identifier=" + patient.getIdentifier().get(0).getSystem() + "|" + patient.getIdentifier().get(0).getValue()));
    }

    private BundleEntryComponent createListBundleEntryComponent(ListResource screeningList, String listId) {
        return new BundleEntryComponent()
                .setResource(screeningList)
                .setFullUrl("urn:uuid:" + UUID.randomUUID())
                .setRequest(new BundleEntryRequestComponent()
                        .setMethod(Bundle.HTTPVerb.PUT)
                        .setUrl("List?identifier=" + systems.getScreeningListIdentifier() + "|" + listId));
    }

    // CREATE RESOURCES
    private ResearchSubject createResearchSubject(UUID studyUUID, UUID patientUuid) {
        return new ResearchSubject()
                .setStatus(ResearchSubject.ResearchSubjectStatus.CANDIDATE)
                .setStudy(new Reference(UUID_URN_PREFIX + studyUUID))
                .setIndividual(new Reference(UUID_URN_PREFIX + patientUuid));
    }

    private Patient createPatient(OmopPerson personInCohort) {
        return new Patient()
                .setBirthDateElement(parseBirthDate(personInCohort))
                .setGender(getGenderFromOmop(personInCohort.getGender()))
                .addIdentifier(new Identifier()
                        .setSystem(systems.getOmopSubjectIdentifier())
                        .setValue(Integer.toString(personInCohort.getPersonId()))
                );
    }

    private ListResource createScreeninglist(String listId, UUID studyUuid) {
        var list = new ListResource()
                .setStatus(ListResource.ListStatus.CURRENT)
                .setMode(ListResource.ListMode.WORKING)
                .setCode(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem(systems.getScreeningListCoding())
                                .setCode("screening-recommendations")))
                .addIdentifier(new Identifier()
                        .setSystem(systems.getScreeningListIdentifier())
                        .setValue(listId));

        // add Study to screeninglist as an extension
        list.addExtension(new Extension()
                .setUrl(systems.getScreeningListStudyReferenceExtension())
                .setValue(new Reference(UUID_URN_PREFIX + studyUuid)));
        return list;
    }

    private ResearchStudy createResearchStudy(CohortDefinition cohort) {
        var study = new ResearchStudy()
                .setStatus(ResearchStudy.ResearchStudyStatus.ACTIVE)
                .addIdentifier(new Identifier()
                        .setSystem(systems.getOmopCohortIdentifier())
                        .setValue(cohort.getId().toString())
                );
        study.getMeta().setSource(systems.getStudySource());

        if (cohort.getName() != null) {
            var title = cohort.getName().replaceAll("\\[.*]", "").trim();
            study.setTitle(title);
            study.addExtension(systems.getResearchStudyAcronym(), new StringType(title));
        }

        if (cohort.getDescription() != null) {
            var description = cohort.getDescription().replaceAll("\\[.*]", "").trim();
            study.setDescription(description);
        }

        return study;
    }
}
