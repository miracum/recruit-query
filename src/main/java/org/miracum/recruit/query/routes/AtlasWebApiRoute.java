package org.miracum.recruit.query.routes;

import com.google.common.collect.Sets;
import java.util.HashSet;
import java.util.Set;
import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.miracum.recruit.query.CohortSelectorConfig;
import org.miracum.recruit.query.LabelExtractor;
import org.miracum.recruit.query.models.CohortDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AtlasWebApiRoute extends RouteBuilder {

  static final String GET_COHORT_DEFINITIONS = "direct:atlas.getCohortDefinitions";
  static final String GET_COHORT_DEFINITION = "direct:atlas.getCohortDefinition";
  static final String RUN_COHORT_GENERATION = "direct:atlas.runCohortGeneration";
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
    // general error handler
    errorHandler(
        defaultErrorHandler()
            .maximumRedeliveries(5)
            .redeliveryDelay(5000)
            .retryAttemptedLogLevel(LoggingLevel.WARN));

    // in case of a http exception then retry at most 3 times
    onException(HttpOperationFailedException.class)
        .maximumRedeliveries(2)
        .handled(true)
        .delay(5000)
        .log(
            LoggingLevel.WARN,
            LOG,
            "HTTP error during request processing. Failing after retrying.");

    // when running all cohorts
    from(GET_COHORT_DEFINITIONS)
        .removeHeaders("CamelHttp*")
        .setHeader(Exchange.HTTP_METHOD, constant("GET"))
        .to(baseUrl + "/cohortdefinition")
        .convertBodyTo(String.class)
        .log(LoggingLevel.DEBUG, LOG, "response from webapi: ${body}")
        .split()
        .jsonpathWriteAsString("$[*]") // foreach cohort.
        // https://camel.apache.org/components/latest/jsonpath-component.html
        .unmarshal()
        .json(
            JsonLibrary.Jackson,
            CohortDefinition.class) // Convert from json to CohortDefinition-Object
        .to(RUN_COHORT_GENERATION)
        .end();

    // when running just one cohort
    from(GET_COHORT_DEFINITION)
        .removeHeaders("CamelHttp*")
        .log("processing cohort: ${body}")
        .setHeader(Exchange.HTTP_METHOD, constant("GET"))
        .toD(baseUrl + "/cohortdefinition/${body}")
        .convertBodyTo(String.class)
        .unmarshal()
        .json(
            JsonLibrary.Jackson,
            CohortDefinition.class) // Convert from json to CohortDefinition-Object
        .log(LoggingLevel.DEBUG, LOG, "[cohort ${body.id}] response from webapi: ${body}")
        .process(
            ex -> {
              // expression macht probleme, daher muss sie immer genullt werden
              var body = (CohortDefinition) ex.getIn().getBody();
              body.setExpression(null);
              ex.getIn().setBody(body);
            })
        .to(RUN_COHORT_GENERATION);

    // generate a cohort
    from(RUN_COHORT_GENERATION)
        .removeHeaders("CamelHttp*")
        .log("[Cohort ${body.id}] processing cohort: ${body}")
        .filter()
        .method(this, "isMatchingCohort")
        .log(LoggingLevel.INFO, LOG, "[Cohort ${body.id}] cohort matches the selector labels")
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
        .log("[Cohort ${header.cohort.id}] current status: ${header.generationStatus}")
        .delay(simple("${properties:atlas.cohortStatusCheckBackoffTime}"))
        .end()
        .choice()
        .when(header(HEADER_GENERATION_STATUS).isEqualTo("COMPLETE"))
        .setBody(header("cohort"))
        .to(Router.DONE_COHORT_GENERATION);
  }

  public boolean isMatchingCohort(@Body CohortDefinition definition) {
    log.info("[Cohort {}] Checking against match labels {}.", definition.getId(), matchLabels);
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
