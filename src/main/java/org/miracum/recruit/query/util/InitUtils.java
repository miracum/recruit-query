package org.miracum.recruit.query.util;

import org.apache.camel.CamelContext;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.miracum.recruit.query.routes.AtlasWebApiRoutes;
import org.miracum.recruit.query.routes.FhirRoutes;
import org.miracum.recruit.query.routes.MainRoutes;
import org.miracum.recruit.query.routes.OmopRoutes;

import java.util.Properties;

/**
 * Utils to get configuration and routes
 */
public class InitUtils {
    public static Properties CONFIG = new Properties();

    private InitUtils() {

    }

    public static CamelContext getContext(Properties props) throws Exception {
        CONFIG = props;
        //override all config params with the ones from environment
        for (var key : CONFIG.keySet()) {
            if (key instanceof String && System.getenv().containsKey(key)) {
                CONFIG.setProperty(key.toString(), System.getenv(key.toString()));
            }
        }
        final var registry = new SimpleRegistry();
        final var cont = new DefaultCamelContext(registry);
        cont.disableJMX();

        // Properties configuration
        var pc = cont.getComponent("properties", PropertiesComponent.class);
        registry.put("configProperties", props);
        pc.setLocation("ref:configProperties");

        // Postgres Datasource
        var dataSource = OmopRoutes.setupDataSource();
        registry.put(props.getProperty("OMOP_DSNAME"), dataSource);

        cont.setTracing(Boolean.parseBoolean(props.getProperty("CAMEL_TRACING_ENABLED", "false")));

        // Routes
        cont.addRoutes(new MainRoutes());
        cont.addRoutes(new OmopRoutes());
        cont.addRoutes(new AtlasWebApiRoutes());
        cont.addRoutes(new FhirRoutes());

        return cont;
    }
}
