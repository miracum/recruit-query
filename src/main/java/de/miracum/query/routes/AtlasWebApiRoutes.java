package de.miracum.query.routes;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.miracum.query.model.atlas.CohortDefinition;

/**
 * All routes for the Atlas-WebAPI
 *
 * @author penndorfp
 * @date 11.10.2019
 */
public class AtlasWebApiRoutes extends RouteBuilder
{

	private static final String HEADER_GENERATION_STATUS = "generationStatus";
	private static final Logger logger = LoggerFactory.getLogger(AtlasWebApiRoutes.class);
	static final String GET_COHORTS = "direct:atlas.getCohorts";
	static final String GET_COHORT_DEFINITIONS = "direct:atlas.getCohortDefinitions";

	@Override
	public void configure() throws Exception
	{
		//@// @formatter:off

		from(GET_COHORT_DEFINITIONS)
				.to("{{ATLAS_WEBAPI_URL}}/cohortdefinition")// https://camel.apache.org/components/latest/http-component.html
				.convertBodyTo(String.class)
				.log(LoggingLevel.DEBUG, logger, "response from webapi: ${body}")
				.split().jsonpathWriteAsString("$[*]")// foreach cohort. https://camel.apache.org/components/latest/jsonpath-component.html
					.log("###### processing cohort: ${body}")
					.unmarshal().json(JsonLibrary.Jackson, CohortDefinition.class)// Convert from json to CohortDefinition-Object
					.setHeader(Exchange.HTTP_METHOD, constant("GET"))
					// needed otherwise ConvertException
					.setHeader("cohort", body())
					.setBody(constant(null))
					.toD("{{ATLAS_WEBAPI_URL}}/cohortdefinition/${header.cohort.id}/generate/OHDSI-CDMV5")
					.setHeader(HEADER_GENERATION_STATUS, constant("PENDING"))
					// Check Status of generation and loop while still running
					.loopDoWhile(header(HEADER_GENERATION_STATUS).regex("PENDING|RUNNING"))
						.toD("{{ATLAS_WEBAPI_URL}}/cohortdefinition/${header.cohort.id}/info")
						.convertBodyTo(String.class)
						.setHeader(HEADER_GENERATION_STATUS, jsonpath("$.[0].status"))
						.log("current status: ${header.generationStatus}")
						.delay(simple("${properties:ATLAS_STATUS_REQUEST_WAITTIME}"))
					.end()
						.choice().when(header(HEADER_GENERATION_STATUS).isEqualTo("COMPLETE"))
							.setBody(header("cohort"))
							.to(MainRoutes.DONE_COHORT_GENERATION)
				;
		// @formatter:on
	}

}
