package de.miracum.query.routes;

import static de.miracum.query.util.InitUtils.CONFIG;

import java.util.List;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.miracum.query.model.atlas.CohortDefinition;

/**
 * All fhir routes
 * 
 * @author penndorfp
 * @date 11.10.2019
 */
public class FhirRoutes extends RouteBuilder
{
	static final String CREATE_SCREENING_LIST = "direct:fhir.createScreeningList";
	private static final Logger logger = LoggerFactory.getLogger(FhirRoutes.class);

	@Override
	public void configure() throws Exception
	{
		// Gets the Ids of the patients for one cohort in "body" and CohortDefinition in
		// "header.cohort"
		from(CREATE_SCREENING_LIST)
				.log(LoggingLevel.DEBUG, logger, "adding ${body.size()} patient(s) for cohort '${header.cohort.id} - ${header.cohort.name}'")
				.process(ex ->
				{
					List<Long> ids = (List<Long>) ex.getIn().getBody();
					CohortDefinition cd = (CohortDefinition) ex.getIn().getHeader("cohort");
					Bundle transaction = new Bundle();
					transaction.setType(BundleType.TRANSACTION);

					for (Long id : ids)
					{
						Patient patient = new Patient();
						Identifier i = new Identifier();
						i.setSystem(CONFIG.getProperty("fhir.systems.omopSubjectIdentifier"));
						i.setValue(id.toString());
						patient.addIdentifier(i);
						// TODO: check if this is correct. Adds patients but don't know if it's
						// complete
						transaction.addEntry().setResource(patient)
								.getRequest()
								.setMethod(HTTPVerb.POST)
								.setUrl("Patient")
								.setIfNoneExist("identifier=" + CONFIG.getProperty("fhir.systems.omopSubjectIdentifier") + "|" + id.toString());

					}
					// TODO: researchStudy, screeningList

					ex.getIn().setBody(transaction);
				})
				.to("fhir:transaction/withBundle?log={{FHIR_LOG_ENABLED}}&serverUrl={{FHIR_BASE_URL}}&inBody=bundle&fhirVersion=R4");
	}
}
