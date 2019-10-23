package de.miracum.query.routes;

import static de.miracum.query.util.InitUtils.CONFIG;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * All routes for coommunication with the omop-database
 *
 * @author penndorfp
 * @date 11.10.2019
 */
public class OmopRoutes extends RouteBuilder
{
	private static final Logger logger = LoggerFactory.getLogger(OmopRoutes.class);
	static final String GET_PATIENT_IDS = "direct:omop.getPatientIds";

	@Override
	public void configure() throws Exception
	{
		//@// @formatter:off

		//gets the CohortDefinition in the body
		from(GET_PATIENT_IDS)
				.to("sql:select subject_id from synpuf_results.cohort where cohort_definition_id=:#${body.id}?dataSource={{OMOP_DSNAME}}")//https://camel.apache.org/components/latest/sql-component.html
				.process(ex -> {
					List<Map<String, Object>> result = (List<Map<String, Object>>) ex.getIn().getBody();
					ex.getIn().setBody(
							//convert result to List<Long>
							result.stream().<Long>map(
									e -> Long.parseLong(Objects.nonNull(e.get("subject_id")) ? e.get("subject_id").toString() : null)
							)
							.collect(Collectors.toList())
					);
				})
				.to("log:?level=INFO&showBody=true")
				.log(LoggingLevel.DEBUG, logger, "found ${body.size()} patient(s) for cohort id ${header.cohort.id}")
				.to(MainRoutes.DONE_GET_PATIENTS)
				;

		// @formatter:on
	}

	public static DataSource setupDataSource()
	{
		String url = CONFIG.getProperty("OMOP_JDBC_URL");
		DriverManagerDataSource ds = new DriverManagerDataSource(url);
		ds.setDriverClassName("org.postgresql.Driver");
		ds.setUrl(url);
		return ds;
	}
}
