package org.miracum.recruit.query.routes;

import com.google.common.collect.Sets;
import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.miracum.recruit.query.CohortSelectorConfig;
import org.miracum.recruit.query.LabelExtractor;
import org.miracum.recruit.query.models.CohortDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class AtlasWebApiRoute extends RouteBuilder {
    static final String GET_COHORT_DEFINITIONS = "direct:atlas.getCohortDefinitions";
    private static final Logger LOG = LoggerFactory.getLogger(AtlasWebApiRoute.class);
    private static final String HEADER_GENERATION_STATUS = "generationStatus";
    private final Set<String> matchLabels;
    private final LabelExtractor labelExtractor;
    @Value("${atlas.url}")
    private String baseUrl;
    @Value("${atlas.dataSource}")
    private String dataSourceName;

    @Autowired
    public AtlasWebApiRoute(CohortSelectorConfig selectorConfig, LabelExtractor labelExtractor) {
        this.matchLabels = selectorConfig.getMatchLabels();
        this.labelExtractor = labelExtractor;
    }

    @Override
    public void configure() {
        // @formatter:off
        from(GET_COHORT_DEFINITIONS).to(baseUrl + "/cohortdefinition")// https://camel.apache.org/components/latest/http-component.html
                .convertBodyTo(String.class)
                .log(LoggingLevel.DEBUG, LOG, "response from webapi: ${body}")
                .split().jsonpathWriteAsString("$[*]") // foreach cohort. https://camel.apache.org/components/latest/jsonpath-component.html
                    .log("processing cohort: ${body}")
                    .unmarshal().json(JsonLibrary.Jackson, CohortDefinition.class)// Convert from json to CohortDefinition-Object
                    .filter().method(this, "isMatchingCohort")
                        .log(LoggingLevel.INFO, LOG, "cohort matches the selector labels")
                        .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                        // needed otherwise ConvertException
                        .setHeader("cohort", body())
                        .setBody(constant(null))
                        .toD(baseUrl + "/cohortdefinition/${header.cohort.id}/generate/" + dataSourceName)
                        .setHeader(HEADER_GENERATION_STATUS, constant("PENDING"))
                        // Check Status of generation and loop while still running
                        .loopDoWhile(header(HEADER_GENERATION_STATUS).regex("PENDING|RUNNING"))
                            .toD(baseUrl + "/cohortdefinition/${header.cohort.id}/info")
                            .convertBodyTo(String.class)
                            .setHeader(HEADER_GENERATION_STATUS, jsonpath("$.[0].status"))
                            .log("current status: ${header.generationStatus}")
                            .delay(simple("${properties:atlas.cohortStatusCheckBackoffTime}"))
                        .end()
                        .choice().when(header(HEADER_GENERATION_STATUS).isEqualTo("COMPLETE"))
                        .setBody(header("cohort"))
                        .to(Router.DONE_COHORT_GENERATION)
                    .end();
        // @formatter:on
    }

    public boolean isMatchingCohort(@Body CohortDefinition definition) {
        log.info("Checking against match labels {}.", matchLabels);

        if (matchLabels.isEmpty()) {
            // if no match labels are specified, simply accept all cohorts
            return true;
        }

        var allLabels = new HashSet<String>();
        allLabels.addAll(labelExtractor.extract(definition.getDescription()));
        allLabels.addAll(labelExtractor.extract(definition.getName()));

        // if there is at least some overlap between all labels extracted from
        // the cohort and the labels to match, i.e. the intersection between these
        // sets is not empty, then the cohort matches.
        return !Sets.intersection(allLabels, matchLabels).isEmpty();
    }
}
