package org.miracum.recruit.query.routes;

import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.miracum.recruit.query.models.OmopPerson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

@Component
public class OmopRoute extends RouteBuilder {

  static final String GET_PATIENT_IDS = "direct:omop.getPatientIds";
  static final String GET_PATIENTS = "direct:omop.getPatients";
  private static final Logger logger = LoggerFactory.getLogger(OmopRoute.class);

  @Bean
  public static DataSource dataSource(
      @Value("${omop.jdbcUrl}") String jdbcUrl,
      @Value("${omop.username}") String username,
      @Value("${omop.password}") String password) {
    var ds = new DriverManagerDataSource(jdbcUrl);
    ds.setDriverClassName("org.postgresql.Driver");
    ds.setUrl(jdbcUrl);
    ds.setUsername(username);
    ds.setPassword(password);
    return ds;
  }

  // catch SQL params from application.yml
  @Value("${query.inxcludePatientParameters.demographics}")
  private boolean catchPatientDemographics;

  /**
   * Create SQL-String to request data from OMOP DB Parameter to be requested can be set in
   * application.yml
   *
   * @param cohortId Cohort of which data has to be requested
   * @return SQL-String
   */
  private String buildSQLString(String cohortId) {
    StringBuilder sqlRequest =
        new StringBuilder(
            "sql:SELECT {{omop.cdmSchema}}.person.person_id, {{omop.cdmSchema}}.person.person_source_value");

    // Check if params should be requested
    if (!this.catchPatientDemographics) {
      sqlRequest.append(
          ", {{omop.cdmSchema}}.concept.concept_name, {{omop.cdmSchema}}.concept.vocabulary_id");
      sqlRequest.append(", {{omop.cdmSchema}}.person.year_of_birth");
      sqlRequest.append(", {{omop.cdmSchema}}.person.month_of_birth");
      sqlRequest.append(", {{omop.cdmSchema}}.person.day_of_birth");
    }
    sqlRequest.append(" FROM {{omop.resultsSchema}}.cohort");
    sqlRequest.append(
        " INNER JOIN {{omop.cdmSchema}}.person ON {{omop.resultsSchema}}.cohort.subject_id={{omop.cdmSchema}}.person.person_id");

    // Join is only necessary for gender
    if (!this.catchPatientDemographics) {
      sqlRequest.append(
          " LEFT JOIN {{omop.cdmSchema}}.concept ON {{omop.cdmSchema}}.concept.concept_id={{omop.cdmSchema}}.person.gender_concept_id");
    }
    sqlRequest.append(" WHERE {{omop.resultsSchema}}.cohort.cohort_definition_id=" + cohortId);
    sqlRequest.append(" ORDER BY {{omop.resultsSchema}}.cohort.cohort_end_date DESC");
    sqlRequest.append(" LIMIT {{query.cohortSizeThreshold}};");
    return sqlRequest.toString();
  }

  @Override
  public void configure() {

    // @formatter:off
    // gets number of persons in cohort as cohortSize in header
    // gets the CohortDefinition in the body
    from(GET_PATIENTS)
        .to(
            "sql:SELECT count(*) from {{omop.resultsSchema}}.cohort where {{omop.resultsSchema}}.cohort.cohort_definition_id = :#${body.id};")
        .process(
            ex -> {
              @SuppressWarnings("unchecked")
              var result = (List<Map<String, Object>>) ex.getIn().getBody();
              ex.getIn().setHeader("cohortSize", result.get(0).get("count"));
            })
        .log(buildSQLString("${header.cohort.id}"))
        .toD(buildSQLString("${header.cohort.id}"))
        .process(
            ex -> {
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
                if (row.get("person_source_value") != null) {
                  patient.setSourceId((String) row.get("person_source_value"));
                }
                if (row.get("vocabulary_id") != null
                    && (row.get("vocabulary_id")).equals("Gender")) {
                  patient.setGender((String) row.getOrDefault("concept_name", null));
                }
                patients.add(patient);
              }
              ex.getIn().setBody(patients);
            })
        .to("log:?level=INFO&showBody=true")
        .log(
            LoggingLevel.DEBUG,
            logger,
            "[Cohort ${header.cohort.id}] found ${body.size()} patient(s) for cohort id ${header.cohort.id}")
        .to(Router.DONE_GET_PATIENTS);
    // @formatter:on
  }
}
