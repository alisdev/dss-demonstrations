package cz.alis.dss.config.logging;

import java.io.File;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.web.context.ContextLoaderListener;

/**
 * Konfigurátor logu, který by měl být zaregistrován jako listener v <code>web.xml</code> ještě před
 * zaregistrováním {@link ContextLoaderListener}, aby se uplatnilo nastavení logu hned od startu
 * aplikace.
 * 
 * @see LogConfigurer
 */
public class ServerLogConfigurer implements ServletContextListener {

    @Override
    public void contextDestroyed( ServletContextEvent servletContextEvent ) {
    }

    @Override
    public void contextInitialized( ServletContextEvent servletContextEvent ) {

        String logConfigFile = servletContextEvent.getServletContext().getInitParameter( LogConfigurer.LOG_CONFIGURATION_FILE_PROPERTY );
        setupLogging( logConfigFile );
    }

    private void setupLogging( final String logConfigPath ) {

        final Logger log = LoggerFactory.getLogger( ServerLogConfigurer.class );

        LogConfigurer logConfigurer = new LogConfigurer();

        if ( logConfigPath != null ) {

            File configurationFile = new File( logConfigPath );

            if ( !configurationFile.isAbsolute() ) {
                log.error( "Cesta ke konfiguracnimu souboru pro logovani musi byt zadana absolutne: {}", logConfigPath );
            }

            if ( configurationFile.exists() ) {
                logConfigurer.configureFromFile( configurationFile );
                return;
            }
            else {
                log.warn( "Konfiguracni soubor nenalezen: {}", logConfigPath );
            }

        }

        logConfigurer.setTargetProjectClass( ServerLogConfigurer.class );
        logConfigurer.configureFromClasspath();

        SLF4JBridgeHandler.install();
    }

}
