package org.miracum.recruit.query.routes;

import ca.uhn.fhir.context.FhirContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.miracum.recruit.query.FhirCohortTransactionBuilder;
import org.miracum.recruit.query.models.CohortDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FhirRoute extends RouteBuilder {
    static final String CREATE_SCREENING_LIST = "direct:fhir.createScreeningList";
    private static final Logger LOG = LoggerFactory.getLogger(FhirRoute.class);

    private final FhirCohortTransactionBuilder fhirBuilder;

    @Autowired
    public FhirRoute(FhirCohortTransactionBuilder fhirBuilder) {
        this.fhirBuilder = fhirBuilder;
    }

    @Override
    public void configure() {
        // Gets the Ids of the patients for one cohort in "body" and CohortDefinition in
        // "header.cohort"
        from(CREATE_SCREENING_LIST)
                .log(LoggingLevel.DEBUG, LOG, "adding ${body.size()} patient(s) for cohort '${header.cohort.id} - ${header.cohort.name}'")
                .process(ex ->
                {
                    // get data from omop db and save it in variables
                    @SuppressWarnings("unchecked")
                    var ids = (List<Long>) ex.getIn().getBody();
                    var cohortDefinition = (CohortDefinition) ex.getIn().getHeader("cohort");

                    var transaction = fhirBuilder.buildFromOmopCohort(cohortDefinition, ids);

                    var jsonParser = FhirContext.forR4().newJsonParser().setPrettyPrint(true);
                    LOG.debug(jsonParser.encodeResourceToString(transaction));

                    // set bundle as http body
                    ex.getIn().setBody(transaction);
                })
                .to("fhir:transaction/withBundle?log={{fhir.logEnabled}}&serverUrl={{fhir.url}}&inBody=bundle&fhirVersion=R4");
    }
}

