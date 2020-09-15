package org.miracum.recruit.query.routes;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.util.List;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.miracum.recruit.query.FhirCohortTransactionBuilder;
import org.miracum.recruit.query.LabelExtractor;
import org.miracum.recruit.query.models.CohortDefinition;
import org.miracum.recruit.query.models.OmopPerson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FhirRoute extends RouteBuilder {

    static final String CREATE_SCREENING_LIST = "direct:fhir.createScreeningList";
    
    private static final Logger LOG = LoggerFactory.getLogger(FhirRoute.class);
    
    private final FhirCohortTransactionBuilder fhirBuilder;
    
private IParser fhirParser;

    @Autowired

    public FhirRoute(FhirCohortTransactionBuilder fhirBuilder, FhirContext fhirContext) {
		this.fhirBuilder = fhirBuilder;
        this.fhirParser = fhirContext.newJsonParser().setPrettyPrint(true);
    }

    @Override
    public void configure() {
        // Gets the Ids of the patients for one cohort in "body" and CohortDefinition in "header.cohort"
        from(CREATE_SCREENING_LIST)
                .log(LoggingLevel.INFO, LOG,
                        "[Cohort ${header.cohort.id}] adding ${body.size()} patient(s) for cohort '${header.cohort.id} - ${header.cohort.name}'")
                .process(ex ->
                {
                    // get data from omop db and save it in variables
                    @SuppressWarnings("unchecked")
                    var patients = (List<OmopPerson>) ex.getIn().getBody();
                    var cohortDefinition = (CohortDefinition) ex.getIn().getHeader("cohort");
                    var transaction = fhirBuilder.buildFromOmopCohort(cohortDefinition, patients);
                    LOG.debug(fhirParser.encodeResourceToString(transaction));
                    // set bundle as http body
                    ex.getIn().setBody(transaction);
                })
                .to("fhir:transaction/withBundle?log={{fhir.logEnabled}}&serverUrl={{fhir.url}}&inBody=bundle&fhirVersion=R4&fhirContext=#bean:fhirContext")
                .process(ex -> {
                    var response = (Bundle) ex.getIn().getBody();
                    LOG.debug(fhirParser.encodeResourceToString(response));
                })
                .log(LoggingLevel.INFO, LOG, "[Cohort ${header.cohort.id}] processing finished");

    }
}
