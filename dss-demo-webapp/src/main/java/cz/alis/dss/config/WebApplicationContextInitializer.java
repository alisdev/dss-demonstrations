package cz.alis.dss.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.web.context.ConfigurableWebApplicationContext;

/**
 * Konfiguruje application context. Jméno této třídy je parametrem ve web.xml. Podrobnosti o
 * konfiguraci kontextu najdete na
 * http://blog.springsource.org/2011/02/15/spring-3-1-m1-unified-property-management
 * <p>
 * Nastavuje seznam {@link PropertySource} v aplikačním kontextu.
 * <p>
 * Seznam zdrojů properties dle přednosti
 * <ol>
 * <li>property zadané z příkazové řádky
 * <li>property z tomcatího kontextu
 * <li>property ze souboru dss.[HOSTNAME].properties
 * <li>property ze souboru dss.properties
 * </ol>
 */
public class WebApplicationContextInitializer implements
        ApplicationContextInitializer<ConfigurableWebApplicationContext> {

    Logger log = LoggerFactory.getLogger( WebApplicationContextInitializer.class );

    @Override
    public void initialize( ConfigurableWebApplicationContext ctx ) {
        try {
            MutablePropertySources sources = ctx.getEnvironment().getPropertySources();
            
            // dss.properties
            sources.addFirst( new ResourceTrimmingPropertySource( "classpath:dss.properties" ) );

            // dss.[HOSTNAME].properties
            String customConf = ConfigFileFinder.getInstanceWithReturnNullWhenNotFound().find( "dss", "properties" );
            if ( customConf != null )
                sources.addFirst( new ResourceTrimmingPropertySource( "classpath:" + customConf ) );

            // servlet context properties
            sources.addFirst( new ServletContextTrimmingPropertySource( "servletContextPropertySource", ctx.getServletContext() ) );

            // System.getEnv()
            sources.addFirst( new SystemEnvironmentPropertySource( "systemEnvironment", ctx.getEnvironment().getSystemEnvironment() ) );

            // System.getProperties()
            sources.addFirst( new MapPropertySource( "systemProperties", ctx.getEnvironment().getSystemProperties() ) );
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Chyba pri konfiguraci property zdroju aplikacniho kontextu.", e );
        }
    }
}
