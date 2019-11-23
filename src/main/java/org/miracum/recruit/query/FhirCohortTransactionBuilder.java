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


        // create FHIR bundle
        Bundle transaction = new Bundle()
                .setType(Bundle.BundleType.TRANSACTION);

        // create ScreeningList
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


        // create ResearchStudy and add it as an Extension
        ResearchStudy study = new ResearchStudy()
                .setStatus(ResearchStudy.ResearchStudyStatus.ACTIVE)
                .setTitle(cohort.getName())
                .setDescription(cohort.getDescription())
                .addIdentifier(new Identifier()
                        .setSystem(systems.getOmopCohortIdentifier())
                        .setValue(cohortId)
                );
        UUID studyUuid = UUID.randomUUID();


        // add study to bundle
        transaction.addEntry().setResource(study)
                .setFullUrl("urn:uuid:" + studyUuid)
                .getRequest()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl("ResearchStudy?identifier=" + systems.getOmopCohortIdentifier() + "|" + cohortId);

        // add Study to screeninglist as an extension
        screeningList.addExtension(new Extension()
                .setUrl(systems.getScreeningListStudyReferenceExtension())
                .setValue(new Reference("urn:uuid:" + studyUuid).setDisplay(cohort.getName()))
        );

        // iterate over all found Patient ID's in this cohort
        for (var id : subjectIds) {

            // create Patient with OMOP ID as an Identifier and nothing else
            Patient patient = new Patient().addIdentifier(new Identifier()
                    .setSystem(systems.getOmopSubjectIdentifier())
                    .setValue(id.toString())
            );

            // create an temporary ID for each patient and add it to the screening list as a reference
            UUID patientUuid = UUID.randomUUID();
            screeningList.addEntry(new ListResource.ListEntryComponent()
                    .setItem(new Reference("urn:uuid:" + patientUuid)));

            // add patient to bundle
            transaction.addEntry().setResource(patient)
                    .setFullUrl("urn:uuid:" + patientUuid)
                    .getRequest()
                    .setMethod(Bundle.HTTPVerb.POST)
                    .setUrl("Patient")
                    .setIfNoneExist("identifier=" + systems.getOmopSubjectIdentifier() + "|" + id.toString());
        }

        // add screening list to bundle
        transaction.addEntry().setResource(screeningList)
                .setFullUrl("urn:uuid:" + UUID.randomUUID())
                .getRequest()
                .setMethod(Bundle.HTTPVerb.PUT)
                .setUrl("List?identifier=" + systems.getScreeningListIdentifier() + "|" + listId);

        return  transaction;
    }
}
