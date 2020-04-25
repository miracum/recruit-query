package org.miracum.recruit.query.routes;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class Router extends RouteBuilder {
    public static final String DONE_COHORT_GENERATION = "direct:main.doneWithCohort";
    public static final String DONE_GET_PATIENTS = "direct:main.doneGetPatients";
    private static final String START_COHORT_GENERATION = "direct:main.startWithCohort";

    @Override
    public void configure() {
        from("timer:getCohorts?period={{query.getCohortsInterval}}&repeatCount=0&delay={{query.startupDelay}}")
                .to(START_COHORT_GENERATION);

        from(START_COHORT_GENERATION)
                .to(AtlasWebApiRoute.GET_COHORT_DEFINITIONS);

        from(DONE_COHORT_GENERATION)
                .to(OmopRoute.GET_PATIENTS);

        from(DONE_GET_PATIENTS)
                .to(FhirRoute.CREATE_SCREENING_LIST);
    }
}
