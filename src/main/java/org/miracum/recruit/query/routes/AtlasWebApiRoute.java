package org.miracum.recruit.query.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.miracum.recruit.query.models.CohortDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AtlasWebApiRoute extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasWebApiRoute.class);
    static final String GET_COHORT_DEFINITIONS = "direct:atlas.getCohortDefinitions";
    private static final String HEADER_GENERATION_STATUS = "generationStatus";

    @Value("${atlas.url}")
    private String atlasBaseUrl;

    @Override
    public void configure() {
        //@// @formatter:off

        from(GET_COHORT_DEFINITIONS).to(atlasBaseUrl + "/cohortdefinition")// https://camel.apache.org/components/latest/http-component.html
                .convertBodyTo(String.class)
                .log(LoggingLevel.DEBUG, LOG, "response from webapi: ${body}")
                .split().jsonpathWriteAsString("$[*]")// foreach cohort. https://camel.apache.org/components/latest/jsonpath-component.html
                    .log("processing cohort: ${body}")
                    .unmarshal().json(JsonLibrary.Jackson, CohortDefinition.class)// Convert from json to CohortDefinition-Object
                    .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                    // needed otherwise ConvertException
                    .setHeader("cohort", body())
                    .setBody(constant(null))
                    .toD(atlasBaseUrl + "/cohortdefinition/${header.cohort.id}/generate/OHDSI-CDMV5")
                    .setHeader(HEADER_GENERATION_STATUS, constant("PENDING"))
                    // Check Status of generation and loop while still running
                    .loopDoWhile(header(HEADER_GENERATION_STATUS).regex("PENDING|RUNNING"))
                        .toD(atlasBaseUrl + "/cohortdefinition/${header.cohort.id}/info")
                        .convertBodyTo(String.class)
                        .setHeader(HEADER_GENERATION_STATUS, jsonpath("$.[0].status"))
                        .log("current status: ${header.generationStatus}")
                        .delay(simple("${properties:atlas.cohortStatusCheckBackoffTime}"))
                    .end()
                    .choice().when(header(HEADER_GENERATION_STATUS).isEqualTo("COMPLETE"))
                    .setBody(header("cohort"))
                    .to(Router.DONE_COHORT_GENERATION);
        // @formatter:on
    }

}
