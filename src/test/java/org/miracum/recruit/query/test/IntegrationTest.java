package org.miracum.recruit.query.test;

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.miracum.recruit.query.routes.MainRoutes;
import org.miracum.recruit.query.util.InitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Properties;

public class IntegrationTest extends CamelTestSupport {
	private static final Properties CONFIG_PROPERTIES = new Properties();
	private static final Logger logger = LoggerFactory.getLogger(IntegrationTest.class);
	@EndpointInject(uri = "mock:result")
	private MockEndpoint resultEndpoint;

	@Produce(uri = "direct:testStart")
	private ProducerTemplate template;

	@Override
	protected CamelContext createCamelContext() throws Exception {
		System.setProperty("query.testing", "true");
		CONFIG_PROPERTIES.load(Objects.requireNonNull(IntegrationTest.class.getClassLoader().getResourceAsStream("config.properties")));
		var context = InitUtils.getContext(CONFIG_PROPERTIES);
		context.addRoutes(new RouteBuilder() {
			@Override
			public void configure() {
				from("direct:testStart").to(MainRoutes.START_COHORT_GENERATION).log("got mock message").to("mock:result");
			}
		});
		return context;
	}

	//TODO: just an example whicht works but it's not a real test. real integrationtest need to have a pre-filled omop-db with test-data
	@Test
	public void test() throws InterruptedException {
		resultEndpoint.expectedBodyReceived().body().contains("test1");
		resultEndpoint.expectedBodyReceived().body().contains("test3");
		template.sendBody(null);
		resultEndpoint.assertIsSatisfied();
	}

}
