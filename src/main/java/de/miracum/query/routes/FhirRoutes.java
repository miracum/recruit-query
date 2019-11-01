package de.miracum.query.routes;

import static de.miracum.query.util.InitUtils.CONFIG;

import java.util.List;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.ListResource;
import org.hl7.fhir.r4.model.ListResource.ListStatus;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResearchStudy;
import org.hl7.fhir.r4.model.ResearchStudy.ResearchStudyStatus;
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
					//Patient
					List<Long> ids = (List<Long>) ex.getIn().getBody();
					CohortDefinition cohortDefinition = (CohortDefinition) ex.getIn().getHeader("cohort");
					String cohortId = Integer.toString(cohortDefinition.getId());
					Bundle transaction = new Bundle();
					transaction.setType(BundleType.TRANSACTION);
					
					//ScreeningList
					ListResource screeninglist = new ListResource();
					
					Identifier listIdentifier = new Identifier();
					listIdentifier.setSystem("{{ATLAS_WEBAPI_URL}}");
					listIdentifier.setValue(cohortId);
					screeninglist.addIdentifier(listIdentifier);
					
					screeninglist.setStatus(ListStatus.CURRENT);
					screeninglist.setMode(ListResource.ListMode.WORKING);
					
					//TODO: Change from Contained-Reference to Reference to existing Study
					ResearchStudy study = new ResearchStudy();
					Identifier studyId = new Identifier();
					studyId.setSystem("{{ATLAS_WEBAPI_URL}}");
					studyId.setValue(cohortId);
					study.addIdentifier(studyId);
					study.setStatus(ResearchStudyStatus.ACTIVE);
					study.setDescription(cohortDefinition.toString());
					//screeninglist.setSubject(new Reference(study));
					
					Extension researchStudy = new Extension();
					researchStudy.setUrl("http://miracum.org/fhir/StructureDefinition/MyExtension");
					researchStudy.setValue(new Reference(study));
					screeninglist.addExtension(researchStudy);


					for (Long id : ids)
					{
						Patient patient = new Patient();
						Identifier i = new Identifier();
						i.setSystem(CONFIG.getProperty("fhir.systems.omopSubjectIdentifier"));
						i.setValue(id.toString());
						patient.addIdentifier(i);
						// TODO: check if this is correct. Adds patients but don't know if it's
						// complete
						/*
						transaction.addEntry().setResource(patient)
								.getRequest()
								.setMethod(HTTPVerb.POST)
								.setUrl("Patient")
								.setIfNoneExist("identifier=" + CONFIG.getProperty("fhir.systems.omopSubjectIdentifier") + "|" + id.toString());
								*/

					}
				
					transaction.addEntry().setResource(screeninglist)
					.getRequest()
					.setMethod(HTTPVerb.POST)
					.setUrl("List");


					ex.getIn().setBody(transaction);
				})
				.to("fhir:transaction/withBundle?log={{FHIR_LOG_ENABLED}}&serverUrl={{FHIR_BASE_URL}}&inBody=bundle&fhirVersion=R4");
	}
}
