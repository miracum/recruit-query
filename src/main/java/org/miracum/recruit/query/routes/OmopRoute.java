package org.miracum.recruit.query.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class OmopRoute extends RouteBuilder {
    static final String GET_PATIENT_IDS = "direct:omop.getPatientIds";
    private static final Logger logger = LoggerFactory.getLogger(OmopRoute.class);

    @Value("${omop.cohortResultsTable}")
    private String cohortResultsTable;

    @Bean
    public static DataSource dataSource(@Value("${omop.jdbcUrl}") String jdbcUrl,
                                        @Value("${omop.username}") String username,
                                        @Value("${omop.password}") String password) {
        var ds = new DriverManagerDataSource(jdbcUrl);
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(jdbcUrl);
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
    }

    @Override
    public void configure() {

        //@// @formatter:off

        // gets the CohortDefinition in the body
        from(GET_PATIENT_IDS)
                //https://camel.apache.org/components/latest/sql-component.html
                .to("sql:SELECT subject_id FROM " + cohortResultsTable + " WHERE cohort_definition_id=:#${body.id}?dataSource=dataSource")
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
                .to(Router.DONE_GET_PATIENTS);
        // @formatter:on
    }
}

