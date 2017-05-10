package cz.alis.dss.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import ch.qos.logback.classic.Level;
import cz.alis.dss.config.logging.HostnameResolver;
import cz.alis.dss.config.logging.LogHelper;

/**
 * Najde konfigurační soubor v classpath.
 * 
 * <ol>
 * <li>zkusí najít konfigurák podle jména počítače (COMPUTERNAME, HOSTNAME)
 * <li>zkusí najít implicitní konfigurák - bez jména počítače
 * <li>pokud žádný konfigurák nalezen není, je chování určeno nastavením atributů
 * {@link #throwExceptionWhenNotFound} a {@link #defaultPath}
 * </ol>
 * 
 * Pokud není nalezen žádný konfigurační soubor, je zalogována chyba.
 * <p>
 * Testováno na Windows Vista, Ubuntu.
 * 
 * @author gargii
 * 
 */
public class ConfigFileFinder {
    private final Logger log = LoggerFactory.getLogger( ConfigFileFinder.class );

    /** Třída, jejímž classloaderem se bude hledat. */
    private final Class<?> clazz;

    /** Určuje zda se bude logovat na DEBUG (verbose) nebo TRACE. */
    private boolean verbose = true;

    /** Pokud je nastavena, bude vrácena v případě nenalezení jiného konfiguráku. */
    private String defaultPath = null;

    /**
     * Pokud true, bude vyhozena výjimka, když nebyl nalezen žádný konfigurák. Pokud false, bude při
     * nenalezení konfiguráku pouze vrácen <code>null</code>.
     */
    private boolean throwExceptionWhenNotFound = true;

    public ConfigFileFinder() {
        clazz = this.getClass();
    }

    /**
     * @param clazz Třída, jejímž classloaderem se bude hledat.
     */
    public ConfigFileFinder( Class<?> clazz ) {
        this.clazz = clazz;
    }

    public String find( String coreFileName, String extension ) {
        return find( "", coreFileName, extension );
    }

    /**
     * Najdi nejvhodnější konfigurační soubor.
     * 
     * @param directory Cesta v classpath, kde se budou soubory hledat (bez úvodního lomítka)
     * @param coreFileName Základní jméno konfiguračního souboru.
     * @param extension Přípona konfiguračního souboru.
     * @return Cesta ke konfiguračnímu souboru vztažená ke classpath.
     */
    public String find( String directory, String coreFileName, String extension ) {
        String fileName = null;

        // hledame konfigurak podle hostname
        String hostname = null;

        hostname = HostnameResolver.getHostname();

        Level logLevel = verbose ? Level.DEBUG : Level.TRACE;
        Level resultLogLevel = verbose ? Level.DEBUG : Level.TRACE;

        LogHelper.log( log, logLevel, "Hostname: {}", hostname );

        if ( hostname != null && !hostname.isEmpty() ) {
            hostname = hostname.toLowerCase();
            String testFileName = createFileName( directory, coreFileName, extension, hostname );
            if ( classpathResourceExist( testFileName ) ) {
                fileName = testFileName;
            }
            else {
                LogHelper.log( log, logLevel, "Custom-konfiguracni soubor nebyl nalezen: {}", testFileName );
            }
        }

        // zkusime dohledat zakladni konfigurak (bez hostname)
        if ( fileName == null ) {
            fileName = createFileName( directory, coreFileName, extension, null );
            if ( !classpathResourceExist( fileName ) ) {
                LogHelper.log( log, logLevel, "Konfiguracni soubor nebyl nalezen: {}", fileName );
                fileName = null;
            }
        }

        // vyhodnotime vysledek hledani konfiguraku
        if ( fileName != null ) {
            // konfigurak nalezen
            LogHelper.log( log, resultLogLevel, "Nacitam konfiguracni soubor: {}", fileName );
        }
        else if ( defaultPath != null ) {
            // konfigurak nenalezen, ale muzeme vratit defaultPath
            LogHelper.log( log, resultLogLevel, "Konfiguracni soubor nenalezen, pouzit default: {}", defaultPath );
            return defaultPath;
        }
        else if ( throwExceptionWhenNotFound ) {
            // konfigurak nenalezen, coz je chyba - vyhodime vyjimku
            throw new RuntimeException( "Konfiguracni soubor nenalezen.\n" + " directory: " + directory + ", coreFileName:" + coreFileName + ", extension: " + extension + ", computerName: " + hostname );
        }
        else {
            // konfigurak nenalezen, coz je prijatelny stav - vratime null
            LogHelper.log( log, resultLogLevel, "Konfiguracni soubor nenalezen - vracim null." );
            return null;
        }

        return fileName;
    }

    private String createFileName( String directory, String coreFileName, String extension, String hostname ) {
        StringBuilder sb = new StringBuilder( "/" );

        if ( directory != null && !directory.isEmpty() )
            sb.append( directory + "/" );

        sb.append( coreFileName );

        if ( hostname != null && !hostname.isEmpty() )
            sb.append( "." + hostname );

        sb.append( "." + extension );

        return sb.toString();
    }

    /**
     * Zjistí, zda lze soubor v classpath najít.
     * 
     * @param testFileName Cesta k souboru (v classpath)
     * @return True pokud soubor existuje.
     */
    private boolean classpathResourceExist( String testFileName ) {
        ClassPathResource resource = new ClassPathResource( testFileName, clazz );
        return resource.exists();
    }

    /**
     * Nalezení konfiguračního souboru v jedné kompaktní statické metodě.
     * 
     * @param coreFileName
     * @param extension
     * @return
     */
    public static String findStatic( String coreFileName, String extension ) {
        ConfigFileFinder finder = new ConfigFileFinder();
        return finder.find( coreFileName, extension );
    }

    /**
     * Pokud je nastaveno na true, bude se logovat jen na urovni trace. Jinak se loguje beznym
     * zpusobem (debug).
     * 
     * @param verbose
     * @see #verbose
     */
    public void setVerbose( boolean verbose ) {
        this.verbose = verbose;
    }

    /** @see #defaultPath */
    public void setDefaultPath( String defaultPath ) {
        this.defaultPath = defaultPath;
    }

    /** @see #throwExceptionWhenNotFound */
    public void setThrowExceptionWhenNotFound( boolean throwExceptionWhenNotFound ) {
        this.throwExceptionWhenNotFound = throwExceptionWhenNotFound;
    }

    /**
     * Vrátí novou instanci {@link ConfigFileFinder}, která vrací null pokud metodou find() nenajde
     * žádný konfigurák.
     * 
     * @see #throwExceptionWhenNotFound
     */
    public static ConfigFileFinder getInstanceWithReturnNullWhenNotFound() {
        return getInstanceWithReturnNullWhenNotFound( ConfigFileFinder.class );
    }

    /**
     * Vrátí novou instanci {@link ConfigFileFinder}, která vrací null pokud metodou find() nenajde
     * žádný konfigurák.
     * 
     * @see #clazz
     * @see #throwExceptionWhenNotFound
     * 
     */
    public static ConfigFileFinder getInstanceWithReturnNullWhenNotFound( Class<?> clazz ) {
        ConfigFileFinder c = new ConfigFileFinder( clazz );
        c.setThrowExceptionWhenNotFound( false );
        return c;
    }

    // -------------------------------------------------------------------------------------
    // Priprava pro vraceni prazdneho property souboru, pokud nebyl nalezen jiny konfigurak.

    // public static ConfigFileFinder getInstanceWithEmptyPropertiesDefault() {
    // return getInstanceWithEmptyPropertiesDefault( ConfigFileFinder.class );
    // }
    //
    // public static ConfigFileFinder getInstanceWithEmptyPropertiesDefault( Class<?> clazz ) {
    // ConfigFileFinder c = new ConfigFileFinder( clazz );
    // c.setDefaultPath( ConfigFileFinder.class.getResource( "empty.properties" ).getPath() );
    // return c;
    // }
}
