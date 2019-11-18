package org.miracum.recruit.query;

import org.apache.camel.main.Main;
import org.miracum.recruit.query.util.InitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Properties;

/**
 * Main app to start camel as standalone app
 */
class MainApp {
    private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

    private MainApp() {

    }

    public static void main(final String... args) throws Exception {
        // Camel initialization
        final Main main = new Main();

        // Properties configuration
        var CONFIG_PROPERTIES = new Properties();
        CONFIG_PROPERTIES.load(Objects.requireNonNull(MainApp.class.getClassLoader().getResourceAsStream("config.properties")));

        // Startup
        main.getCamelContexts().add(InitUtils.getContext(CONFIG_PROPERTIES));
        logger.debug("Poperties: {}", CONFIG_PROPERTIES);
        main.start();
        main.run();
        logger.info("SHUTTING DOWN");
        main.stop();
        main.shutdown();
    }
}
