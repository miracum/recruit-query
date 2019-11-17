package org.miracum.recruit.query.routes;

import org.apache.camel.builder.RouteBuilder;

/**
 * Main routes that glue all other routes together
 */
public class MainRoutes extends RouteBuilder {
    public static final String DONE_COHORT_GENERATION = "direct:main.doneWithCohort";
    public static final String START_COHORT_GENERATION = "direct:main.startWithCohort";
    public static final String DONE_GET_PATIENTS = "direct:main.doneGetPatients";

    @Override
    public void configure() {
        // Only when not testing, activate timer https://camel.apache.org/components/latest/timer-component.html
        // check if SystemProperty 'query.testing' is set to false (e.g. '-Dquery.testing=false' as cmd-parameter)
        if (!Boolean.getBoolean("query.testing")) {
            from("timer:getCohorts?period={{MAIN_GETCOHORT_INTERVAL}}&repeatCount=0&delay=" + Integer.getInteger("query.startupDelay", 1000))
                    .to(START_COHORT_GENERATION);
        }
        // Start point
        from(START_COHORT_GENERATION)
                .to(AtlasWebApiRoutes.GET_COHORT_DEFINITIONS);

        from(DONE_COHORT_GENERATION)
                .to(OmopRoutes.GET_PATIENT_IDS);

        from(DONE_GET_PATIENTS)
                .to(FhirRoutes.CREATE_SCREENING_LIST);
    }
}
