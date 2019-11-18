package org.miracum.recruit.query.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.miracum.recruit.query.util.InitUtils.CONFIG;

/**
 * All routes for communication with the OMOP database
 */
public class OmopRoutes extends RouteBuilder {
    static final String GET_PATIENT_IDS = "direct:omop.getPatientIds";
    private static final Logger logger = LoggerFactory.getLogger(OmopRoutes.class);

    public static DataSource setupDataSource() {
        var url = CONFIG.getProperty("OMOP_JDBC_URL");
        var ds = new DriverManagerDataSource(url);
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(url);
        return ds;
    }

    @Override
    public void configure() {
        //@// @formatter:off

        // gets the CohortDefinition in the body
        from(GET_PATIENT_IDS)
                .to("sql:select subject_id from synpuf_results.cohort where cohort_definition_id=:#${body.id}?dataSource={{OMOP_DSNAME}}")//https://camel.apache.org/components/latest/sql-component.html
                .process(ex -> {
                    @SuppressWarnings("unchecked")
                    var result = (List<Map<String, Object>>) ex.getIn().getBody();
                    ex.getIn().setBody(
                            // convert result to List<Long>
                            result.stream()
                                    .map(e -> e.get("subject_id"))
                                    .filter(Objects::nonNull)
                                    .map(e -> Long.parseLong(e.toString()))
                                    .collect(Collectors.toList())
                    );
                })
                .to("log:?level=INFO&showBody=true")
                .log(LoggingLevel.DEBUG, logger, "found ${body.size()} patient(s) for cohort id ${header.cohort.id}")
                .to(MainRoutes.DONE_GET_PATIENTS);
        // @formatter:on
    }
}
