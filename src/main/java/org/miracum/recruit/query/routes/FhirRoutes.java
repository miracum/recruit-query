package org.miracum.recruit.query.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.formats.JsonParser;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.ListResource.ListEntryComponent;
import org.hl7.fhir.r4.model.ListResource.ListStatus;
import org.hl7.fhir.r4.model.ResearchStudy.ResearchStudyStatus;
import org.miracum.recruit.query.model.atlas.CohortDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

import java.util.List;
import java.util.UUID;

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
                    // get data from omop db and save it in variables
                    @SuppressWarnings("unchecked")
                    var ids = (List<Long>) ex.getIn().getBody();
                    CohortDefinition cohortDefinition = (CohortDefinition) ex.getIn().getHeader("cohort");
                    String cohortId = Integer.toString(cohortDefinition.getId());
                    String listId = "screeninglist-" + cohortId;


                    // create FHIR bundle
                    Bundle transaction = new Bundle()
                            .setType(BundleType.TRANSACTION);

                    // create ScreeningList
                    ListResource screeningList = new ListResource()
                            .setStatus(ListStatus.CURRENT)
                            .setMode(ListResource.ListMode.WORKING)
                            .setCode(new CodeableConcept()
                                    .addCoding(new Coding()
                                            .setSystem(CONFIG.getProperty("fhir.systems.screeningListCoding"))
                                            .setCode("screening-recommendations")))
                            .addIdentifier(new Identifier()
                                    .setSystem(CONFIG.getProperty("fhir.systems.screeningListIdentifier"))
                                    .setValue(listId));


                    // create ResearchStudy and add it as an Extension
                    ResearchStudy study = new ResearchStudy()
                            .setStatus(ResearchStudyStatus.ACTIVE)
                            .setTitle(cohortDefinition.getName())
                            .setDescription(cohortDefinition.getDescription())
                            .addIdentifier(new Identifier()
                                    .setSystem(CONFIG.getProperty("fhir.systems.omopCohortIdentifier"))
                                    .setValue(cohortId)
                            );
                    UUID studyUuid = UUID.randomUUID();


                    // add study to bundle
                    transaction.addEntry().setResource(study)
                            .setFullUrl("urn:uuid:" + studyUuid)
                            .getRequest()
                            .setMethod(HTTPVerb.PUT)
                            .setUrl("ResearchStudy?identifier=" + CONFIG.getProperty("fhir.systems.omopCohortIdentifier") + "|" + cohortId);

                    // add Study to screeninglist as an extension
                    screeningList.addExtension(new Extension()
                            .setUrl(CONFIG.getProperty("fhir.systems.screeningListStudyReferenceExtension"))
                            .setValue(new Reference("urn:uuid:" + studyUuid))
                    );

                    // iterate over all found Patient ID's in this cohort
                    for (var id : ids) {

                        // create Patient with OMOP ID as an Identifier and nothing else
                    	Patient patient = new Patient().addIdentifier(new Identifier()
                                .setSystem(CONFIG.getProperty("fhir.systems.omopSubjectIdentifier"))
                                .setValue(id.toString())
                        );

                        // create an temporary ID for each patient and add it to the screening list as a reference
                        UUID patientUuid = UUID.randomUUID();
                        screeningList.addEntry(new ListEntryComponent()
                                .setItem(new Reference("urn:uuid:" + patientUuid)));

                        // add patient to bundle
                        transaction.addEntry().setResource(patient)
                                .setFullUrl("urn:uuid:" + patientUuid)
                                .getRequest()
                                .setMethod(HTTPVerb.POST)
                                .setUrl("Patient")
                                .setIfNoneExist("identifier=" + CONFIG.getProperty("fhir.systems.omopSubjectIdentifier") + "|" + id.toString());
                    }

                    // add screening list to bundle
                   
                    transaction.addEntry().setResource(screeningList)
                    		.setFullUrl("urn:uuid:" + UUID.randomUUID())
                            .getRequest()
                            .setMethod(HTTPVerb.PUT)
                            .setUrl("List?identifier=" + CONFIG.getProperty("fhir.systems.screeningListIdentifier") + "|" + listId);
					
                    
                    IParser jsonParser = FhirContext.forR4().newJsonParser();
            		jsonParser.setPrettyPrint(true);

                    logger.debug(jsonParser.encodeResourceToString(transaction));

                    // set bundle as http body
                    ex.getIn().setBody(transaction);
                })
                .to("fhir:transaction/withBundle?log={{FHIR_LOG_ENABLED}}&serverUrl={{FHIR_BASE_URL}}&inBody=bundle&fhirVersion=R4");
    }
}
