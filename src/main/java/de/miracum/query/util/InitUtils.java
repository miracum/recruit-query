package de.miracum.query.util;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.camel.CamelContext;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;

import de.miracum.query.routes.AtlasWebApiRoutes;
import de.miracum.query.routes.FhirRoutes;
import de.miracum.query.routes.MainRoutes;
import de.miracum.query.routes.OmopRoutes;

/**
 * Utils to get configuration and routes
 * @author penndorfp
 * @date 11.10.2019
 */
public class InitUtils
{
	public static Properties CONFIG = new Properties();

	public static CamelContext getContext(Properties props) throws Exception
	{
		CONFIG = props;
		//override all config params with the ones from environment
		for(Object key : CONFIG.keySet())
		{
			if(key instanceof String && System.getenv().containsKey(key))
			{
				CONFIG.setProperty(key.toString(), System.getenv(key.toString()));
			}
		}
		final SimpleRegistry registry = new SimpleRegistry();
		final CamelContext cont = new DefaultCamelContext(registry);
		cont.disableJMX();

	    //Properties configuration
		PropertiesComponent pc = cont.getComponent("properties", PropertiesComponent.class);
		registry.put("configProperties", props);
		pc.setLocation("ref:configProperties");

		//Postgres Datasource
		DataSource dataSource = OmopRoutes.setupDataSource();
		registry.put(props.getProperty("OMOP_DSNAME"), dataSource);

		cont.setTracing(Boolean.parseBoolean(props.getProperty("CAMEL_TRACING_ENABLED", "false")));

		//Routes
		cont.addRoutes(new MainRoutes());
		cont.addRoutes(new OmopRoutes());
		cont.addRoutes(new AtlasWebApiRoutes());
		cont.addRoutes(new FhirRoutes());

		return cont;
	}
}
