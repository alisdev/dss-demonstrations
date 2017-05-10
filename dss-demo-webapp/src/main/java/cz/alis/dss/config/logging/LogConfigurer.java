package cz.alis.dss.config.logging;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cz.alis.dss.config.ConfigFileFinder;


/**
 * Konfigurátor logování.
 * <p>
 * Konfigurační soubor se načítá pomocí classloaderu třídy {@link #targetProjectClass}.
 * <p>
 */
public class LogConfigurer {

    Logger log = LoggerFactory.getLogger( LogConfigurer.class );

    /** Defaultní základ jména konfiguračního souboru. */
    public static final String DEFAULT_PROPERTY_FILE_CORE_FILENAME = "logback";

    public static final String LOG_CONFIGURATION_FILE_PROPERTY = "logConfigurationFile";

    /** Základ jména konfiguračního souboru. */
    String propertyFileCoreFileName = DEFAULT_PROPERTY_FILE_CORE_FILENAME;

    /** Třída pro získání classloaderu, přes který se hledají soubory. */
    Class<?> targetProjectClass = LogConfigurer.class;

    String classpathDir = "";

    public void configureFromClasspath() {
        ConfigFileFinder finder = new ConfigFileFinder( targetProjectClass );

        // timto zaridim, aby se nelogovalo (resp. loguje se na urovni trace)
        finder.setVerbose( false );

        // konfigurace logbacku
        String resourceName = finder.find( classpathDir, propertyFileCoreFileName, "xml" );
        if ( resourceName != null && !resourceName.isEmpty() ) {
            LogHelper.configureLogback( targetProjectClass, resourceName );
            log.info( "Logovani nakonfigurovano podle - classpath:{}", resourceName );
        }
    }

    public void configureFromFile( File configurationFile ) {
        LogHelper.configureLogback( targetProjectClass, configurationFile );
        log.info( "Logovani nakonfigurovano podle souboru: {}", configurationFile.getPath() );
    }

    /**
     * Nastaví třídu, ze které bude načten classloader pro získání konfiguračního souboru.
     * 
     * @param targetProjectClass
     */
    public void setTargetProjectClass( Class<?> targetProjectClass ) {
        this.targetProjectClass = targetProjectClass;
    }

    /**
     * Nastaví základ jména konfiguračního souboru pro pro logback. Defaultní hodnota je
     * {@value #DEFAULT_PROPERTY_FILE_CORE_FILENAME}.
     */
    public void setPropertyFileCoreFileName( String propertyFileCoreFileName ) {
        this.propertyFileCoreFileName = propertyFileCoreFileName;
    }

    public void setClasspathDir( String classpathDir ) {
        this.classpathDir = classpathDir;
    }

}
