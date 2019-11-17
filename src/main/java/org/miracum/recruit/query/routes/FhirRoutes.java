package org.miracum.recruit.query.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.ListResource.ListStatus;
import org.hl7.fhir.r4.model.ResearchStudy.ResearchStudyStatus;
import org.miracum.recruit.query.model.atlas.CohortDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.miracum.recruit.query.util.InitUtils.CONFIG;

/**
 * All FHIR routes
 */
public class FhirRoutes extends RouteBuilder {
    static final String CREATE_SCREENING_LIST = "direct:fhir.createScreeningList";
    private static final Logger logger = LoggerFactory.getLogger(FhirRoutes.class);

    @Override
    public void configure() {
        // Gets the Ids of the patients for one cohort in "body" and CohortDefinition in
        // "header.cohort"
        from(CREATE_SCREENING_LIST)
                .log(LoggingLevel.DEBUG, logger, "adding ${body.size()} patient(s) for cohort '${header.cohort.id} - ${header.cohort.name}'")
                .process(ex ->
                {
                    var ids = (List<Long>) ex.getIn().getBody();
                    var cohortDefinition = (CohortDefinition) ex.getIn().getHeader("cohort");
                    var cohortId = Integer.toString(cohortDefinition.getId());

                    var transaction = new Bundle()
                            .setType(BundleType.TRANSACTION);

                    var listIdentifier = new Identifier()
                            .setSystem(CONFIG.getProperty("fhir.systems.screeningListIdentifier"))
                            .setValue(cohortId);

                    var screeningList = new ListResource()
                            .setStatus(ListStatus.CURRENT)
                            .setMode(ListResource.ListMode.WORKING)
                            .addIdentifier(listIdentifier);

                    var studyId = new Identifier()
                            .setSystem(CONFIG.getProperty("fhir.systems.researchStudyAcronym"))
                            .setValue(cohortDefinition.getName());

                    var study = new ResearchStudy()
                            .setStatus(ResearchStudyStatus.ACTIVE)
                            .setDescription(cohortDefinition.getDescription())
                            .addIdentifier(studyId);

                    var studyReference = new Extension()
                            .setUrl(CONFIG.getProperty("fhir.systems.screeningListStudyReferenceExtension"))
                            .setValue(new Reference(study));

                    screeningList.addExtension(studyReference);

                    for (var id : ids) {
                        var patientId = new Identifier()
                                .setSystem(CONFIG.getProperty("fhir.systems.omopSubjectIdentifier"))
                                .setValue(id.toString());

                        var patient = new Patient()
                                .addIdentifier(patientId);

                        transaction.addEntry()
                                .setResource(patient)
                                .getRequest()
                                .setMethod(HTTPVerb.POST)
                                .setUrl("Patient")
                                .setIfNoneExist("identifier=" + CONFIG.getProperty("fhir.systems.omopSubjectIdentifier") + "|" + id.toString());
                    }

                    // TODO: only create a new list if it doesn't already exist, update it if it does.
                    transaction.addEntry()
                            .setResource(screeningList)
                            .getRequest()
                            .setMethod(HTTPVerb.POST)
                            .setUrl("List");

                    ex.getIn().setBody(transaction);
                })
                .to("fhir:transaction/withBundle?log={{FHIR_LOG_ENABLED}}&serverUrl={{FHIR_BASE_URL}}&inBody=bundle&fhirVersion=R4");
    }
}
