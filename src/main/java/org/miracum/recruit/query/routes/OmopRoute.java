package org.miracum.recruit.query.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.miracum.recruit.query.models.OmopPerson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OmopRoute extends RouteBuilder {
    static final String GET_PATIENT_IDS = "direct:omop.getPatientIds";
    static final String GET_PATIENTS = "direct:omop.getPatients";
    private static final Logger logger = LoggerFactory.getLogger(OmopRoute.class);

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
        from(GET_PATIENTS)
                //https://camel.apache.org/components/latest/sql-component.html
                .to("sql:SELECT "
                        + "{{omop.cdmSchema}}.person.person_id, "
                        + "{{omop.cdmSchema}}.person.year_of_birth, "
                        + "{{omop.cdmSchema}}.person.month_of_birth, "
                        + "{{omop.cdmSchema}}.person.day_of_birth, "
                        + "{{omop.cdmSchema}}.concept.concept_name, "
                        + "{{omop.cdmSchema}}.concept.vocabulary_id"
                        + " FROM {{omop.resultsSchema}}.cohort"
                        + " INNER JOIN {{omop.cdmSchema}}.person ON {{omop.resultsSchema}}.cohort.subject_id={{omop.cdmSchema}}.person.person_id"
                        + " LEFT JOIN {{omop.cdmSchema}}.concept ON {{omop.cdmSchema}}.concept.concept_id={{omop.cdmSchema}}.person.gender_concept_id"
                        + " WHERE {{omop.resultsSchema}}.cohort.cohort_definition_id=:#${body.id};")
                //.to("sql:SELECT * FROM " + personTable + ";")
                .process(ex -> {
                    @SuppressWarnings("unchecked")
                    var result = (List<Map<String, Object>>) ex.getIn().getBody();
                    var patients = new ArrayList<OmopPerson>();
                    for (Map<String, Object> row : result) {
                        OmopPerson patient = new OmopPerson();
                        patient.setPersonId((int) row.get("person_id"));

                        if (row.get("year_of_birth") != null) {
                            patient.setYearOfBirth(Year.of((int) row.get("year_of_birth")));
                        }

                        if (row.get("month_of_birth") != null) {
                            patient.setMonthOfBirth(Month.of(((int) row.get("month_of_birth"))));
                        }

                        if (row.get("day_of_birth") != null) {
                            patient.setDayOfBirth((int) row.get("day_of_birth"));
                        }

                        if ((row.get("vocabulary_id")).equals("Gender")) {
                            patient.setGender((String) row.getOrDefault("concept_name", null));
                        }

                        patients.add(patient);
                    }
                    ex.getIn().setBody(patients);
                })
                .to("log:?level=INFO&showBody=true")
                .log(LoggingLevel.DEBUG, logger, "found ${body.size()} patient(s) for cohort id ${header.cohort.id}")
//                .loop(Integer.parseInt("${body.size()}"))
//                	.to("sql:SELECT year_of_birth FROM " + personTable + " WHERE person_id=" + LOOP_INDEX)
                .to(Router.DONE_GET_PATIENTS);
        // @formatter:on
    }
}
