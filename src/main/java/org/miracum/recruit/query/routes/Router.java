package org.miracum.recruit.query.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.stereotype.Component;

@Component
public class Router extends RouteBuilder {
    public static final String DONE_COHORT_GENERATION = "direct:main.doneWithCohort";
    public static final String DONE_GET_PATIENTS = "direct:main.doneGetPatients";
    public static final String START_COHORT_GENERATION = "direct:main.startWithCohort";

    @Override
    public void configure() {

        // run via REST
        restConfiguration().component("servlet")
                .host("{{query.url}}")
                .port("{{query.port}}")
                .bindingMode(RestBindingMode.auto);

        rest("/run")
                // run all cohorts
                .post()
                .route()
                .log(LoggingLevel.INFO, "Run Query module from external call")
                .process(ex -> {
                    var template = ex.getContext().createProducerTemplate();
                    template.asyncSendBody(START_COHORT_GENERATION, null);
                })
                .transform().constant("Successfully started Query Module")
                .endRest()
                // run a cohort from the omop cohort-id
                .post("/{cohortId}")
                .route()
                .log(LoggingLevel.INFO, "Run cohort ${header.cohortId} in query module from external call")
                .process(ex -> {
                    var template = ex.getContext().createProducerTemplate();
                    template.asyncSendBody(AtlasWebApiRoute.GET_COHORT_DEFINITION, ex.getIn().getHeader("cohortId"));
                })
                .transform().simple("Successfully started Query Module for cohort ${header.cohortId}")
                .endRest();

        // Run from timer
        from("cron:getCohorts?schedule=0+{{query.schedule.unixCron}}")
                .autoStartup("{{query.schedule.enable}}")
                .to(START_COHORT_GENERATION);

        // Processing
        from(START_COHORT_GENERATION)
                .to(AtlasWebApiRoute.GET_COHORT_DEFINITIONS);

        from(DONE_COHORT_GENERATION)
                .to(OmopRoute.GET_PATIENTS);

        from(DONE_GET_PATIENTS)
                .to(FhirRoute.CREATE_SCREENING_LIST);
    }
}
