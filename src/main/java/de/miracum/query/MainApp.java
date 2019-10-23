package de.miracum.query;

import java.util.Properties;

import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.miracum.query.util.InitUtils;

/**
 * Main app to start camel as standalone app
 * @author penndorfp
 * @date 11.10.2019
 */
public class MainApp
{
	private static final Logger logger = LoggerFactory.getLogger(MainApp.class);

	public static void main(final String... args) throws Exception
	{
		// Camel initialization
		final Main main = new Main();

		// Properties configuration
		Properties CONFIG_PROPERTIES = new Properties();
		CONFIG_PROPERTIES.load(MainApp.class.getClassLoader().getResourceAsStream("config.properties"));

		// Startup
		main.getCamelContexts().add(InitUtils.getContext(CONFIG_PROPERTIES));
		logger.debug("Poperties: {}", CONFIG_PROPERTIES);
		main.start();
		main.run(new String[] {});
		logger.info("SHUTTING DOWN");
		main.stop();
		main.shutdown();
	}
}
