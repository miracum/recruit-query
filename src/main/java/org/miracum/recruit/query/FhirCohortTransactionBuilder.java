package org.miracum.recruit.query;

import org.hl7.fhir.r4.model.*;
import org.miracum.recruit.query.models.CohortDefinition;
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

    public Bundle buildFromOmopCohort(CohortDefinition cohort, List<Long> subjectIds) {
        String cohortId = Integer.toString(cohort.getId());
        String listId = "screeninglist-" + cohortId;

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
        // create random UUID
        UUID studyUuid = UUID.randomUUID();
        // add study to bundle
        transaction.addEntry().setResource(study)
                .setFullUrl("urn:uuid:" + studyUuid)
                .getRequest()
                .setMethod(Bundle.HTTPVerb.POST)
                .setIfNoneExist("identifier=" + systems.getOmopCohortIdentifier() + "|" + cohortId);
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
        for (var id : subjectIds) {

            //PATIENT
            // create Patient with OMOP ID as an Identifier and nothing else
            Patient patient = new Patient().addIdentifier(new Identifier()
                    .setSystem(systems.getOmopSubjectIdentifier())
                    .setValue(id.toString())
            );
            // create random UUID as a Identifier
            UUID patientUuid = UUID.randomUUID();
            // add patient to bundle
            transaction.addEntry().setResource(patient)
                    .setFullUrl("urn:uuid:" + patientUuid)
                    .getRequest()
                    .setMethod(Bundle.HTTPVerb.POST)
                    .setIfNoneExist("identifier=" + systems.getOmopSubjectIdentifier() + "|" + id.toString());

            //RESEARCHSUBJECT
            // create ResearchSubject with Reference on Patient and Study
            ResearchSubject researchSubject = new ResearchSubject()
                    .setStatus(ResearchSubject.ResearchSubjectStatus.CANDIDATE)
                    .setStudy(new Reference("urn:uuid:" + studyUuid))
                    .setIndividual(new Reference("urn:uuid:" + patientUuid));
            // create random UUID for referencing ResearchSubject
            UUID subjectUuid = UUID.randomUUID();
            // add ResearchSubject to bundle
            transaction.addEntry().setResource(researchSubject)
                    .setFullUrl("urn:uuid:" + subjectUuid)
                    .getRequest()
                    .setMethod(Bundle.HTTPVerb.POST)
                    .setIfNoneExist("patient.identifier=" + systems.getOmopSubjectIdentifier() + "|" + id.toString()
                            + "&" + "study.identifier=" + systems.getOmopCohortIdentifier() + "|" + cohortId);

            //SCREENINGLISTE
            // Reference to ResearchSubject
            screeningList.addEntry(new ListResource.ListEntryComponent()
                    .setItem(new Reference("urn:uuid:" + subjectUuid)));

        }
        // add screening list to bundle
        transaction.addEntry().setResource(screeningList)
                .setFullUrl("urn:uuid:" + UUID.randomUUID())
                .getRequest()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl("List?identifier=" + systems.getScreeningListIdentifier() + "|" + listId);

        return transaction;
    }
}
