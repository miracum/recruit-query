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
    private final FhirSystems systems;

    @Autowired
    public FhirCohortTransactionBuilder(FhirSystems fhirSystems) {
        systems = fhirSystems;
    }

    /**
     * Transforms gender in OMOP data
     *
     * @param gender String representation in OMOP of the gender
     * @return gender in Fhir format
     */
    private static AdministrativeGender getGenderFromOmop(String gender) {
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

    public Bundle buildFromOmopCohort(CohortDefinition cohort, List<OmopPerson> personsInCohort) {
        String cohortId = Integer.toString(cohort.getId());
        String listId = "screeninglist-" + cohortId;
        // create random UUIDs
        UUID studyUuid = UUID.randomUUID();

        //BUNDLE
        // create
        Bundle transaction = new Bundle()
                .setType(Bundle.BundleType.TRANSACTION);

        //RESEARCHSTUDY
        // create
        ResearchStudy study = new ResearchStudy()
                .setStatus(ResearchStudy.ResearchStudyStatus.ACTIVE)
                .setTitle(cohort.getName())
                .setDescription(cohort.getDescription())
                .addIdentifier(new Identifier()
                        .setSystem(systems.getOmopCohortIdentifier())
                        .setValue(cohortId)
                );

        //SCREENINGLIST
        // create
        ListResource screeningList = new ListResource()
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
        screeningList.addExtension(new Extension()
                .setUrl(systems.getScreeningListStudyReferenceExtension())
                .setValue(new Reference("urn:uuid:" + studyUuid).setDisplay(cohort.getName()))
        );


        //LOOP trough all Patients
        for (OmopPerson personInCohort : personsInCohort) {
            //PATIENT

            // create Patient with OMOP ID as an Identifier
            Patient patient = new Patient()
                    // add birth year
                    .setBirthDateElement(parseBirthDate(personInCohort))
                    .addIdentifier(new Identifier()
                            .setSystem(systems.getOmopSubjectIdentifier())
                            .setValue(Integer.toString(personInCohort.getPersonId()))
                    );

            // only set the gender if it is present on the person
            if (personInCohort.getGender() != null) {
                patient.setGender(getGenderFromOmop(personInCohort.getGender()));
            }

            // create random UUID as a Identifier
            UUID patientUuid = UUID.randomUUID();
            // add patient to bundle
            transaction.addEntry(new BundleEntryComponent()
                    .setResource(patient)
                    .setFullUrl("urn:uuid:" + patientUuid)
                    .setRequest(new BundleEntryRequestComponent()
                            .setMethod(Bundle.HTTPVerb.PUT)
                            .setUrl("Patient?identifier=" + systems.getOmopSubjectIdentifier() + "|" + personInCohort.getPersonId()))
            );

            //RESEARCHSUBJECT
            // create ResearchSubject with Reference on Patient and Study
            ResearchSubject researchSubject = new ResearchSubject()
                    .setStatus(ResearchSubject.ResearchSubjectStatus.CANDIDATE)
                    .setStudy(new Reference("urn:uuid:" + studyUuid))
                    .setIndividual(new Reference("urn:uuid:" + patientUuid));
            // create random UUID for referencing ResearchSubject
            UUID subjectUuid = UUID.randomUUID();
            // add ResearchSubject to bundle
            transaction.addEntry(new BundleEntryComponent()
                    .setResource(researchSubject)
                    .setFullUrl("urn:uuid:" + subjectUuid)
                    .setRequest(new BundleEntryRequestComponent()
                            .setMethod(Bundle.HTTPVerb.PUT)
                            .setUrl("ResearchSubject?patient.identifier=" + systems.getOmopSubjectIdentifier() + "|" + personInCohort.getPersonId()
                                    + "&" + "study.identifier=" + systems.getOmopCohortIdentifier() + "|" + cohortId))
            );

            //SCREENINGLISTE
            // Reference to ResearchSubject
            screeningList.addEntry(new ListResource.ListEntryComponent()
                    .setItem(new Reference("urn:uuid:" + subjectUuid)));

        }

        //ADD STUDY TO BUNDLE
        transaction.addEntry(new BundleEntryComponent()
                .setResource(study)
                .setFullUrl("urn:uuid:" + studyUuid)
                .setRequest(new BundleEntryRequestComponent()
                        .setMethod(Bundle.HTTPVerb.PUT)
                        .setUrl("ResearchStudy?identifier=" + systems.getOmopCohortIdentifier() + "|" + cohortId)
                )
        );
        //ADD LIST TO BUNDLE
        transaction.addEntry(new BundleEntryComponent()
                .setResource(screeningList)
                .setFullUrl("urn:uuid:" + UUID.randomUUID())
                .setRequest(new BundleEntryRequestComponent()
                        .setMethod(Bundle.HTTPVerb.PUT)
                        .setUrl("List?identifier=" + systems.getScreeningListIdentifier() + "|" + listId)));

        return transaction;
    }
}
