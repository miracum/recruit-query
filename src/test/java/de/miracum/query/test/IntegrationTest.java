package de.miracum.query.test;

import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.miracum.query.routes.MainRoutes;
import de.miracum.query.util.InitUtils;

/**
 *
 * @author penndorfp
 * @date 11.10.2019
 */
public class IntegrationTest extends CamelTestSupport
{
	private static final Logger logger = LoggerFactory.getLogger(IntegrationTest.class);
	static final Properties CONFIG_PROPERTIES = new Properties();
	@EndpointInject(uri = "mock:result")
	protected MockEndpoint resultEndpoint;

	@Produce(uri = "direct:testStart")
	protected ProducerTemplate template;

	@Override
	protected CamelContext createCamelContext() throws Exception
	{
		System.setProperty("query.testing", "true");
		CONFIG_PROPERTIES.load(IntegrationTest.class.getClassLoader().getResourceAsStream("config.properties"));
		CamelContext context = InitUtils.getContext(CONFIG_PROPERTIES);
		context.addRoutes(new RouteBuilder()
		{
			@Override
			public void configure() throws Exception
			{
				from("direct:testStart").to(MainRoutes.START_COHORT_GENERATION).log("got mock message").to("mock:result");
			}
		});
		return context;
	}

	//TODO: just an example whicht works but it's not a real test. real integrationtest need to have a pre-filled omop-db with test-data
	@Test
	public void test() throws InterruptedException
	{
		resultEndpoint.expectedBodyReceived().body().contains("test1");
		resultEndpoint.expectedBodyReceived().body().contains("test3");
		template.sendBody(null);
		resultEndpoint.assertIsSatisfied();
	}

}
