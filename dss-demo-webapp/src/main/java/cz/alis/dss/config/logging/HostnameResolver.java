package cz.alis.dss.config.logging;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Metoda {@link #getHostname()} vrací hostname počítače, na kterém JVM běží.
 */
public class HostnameResolver {
    
    static Logger log = LoggerFactory.getLogger( HostnameResolver.class );

    /**
     * Vrací hostname počítače, na kterém JVM běží.
     * @return
     */
    public static String getHostname() {
        Properties props = System.getProperties();

        String jvmHostname = props.getProperty( "hostname" );

        if ( jvmHostname != null ) {
            log.trace("Hostname (z system properties): {}", jvmHostname );
            return jvmHostname;
        }
        else {
            try {
                String inetHostname = InetAddress.getLocalHost().getHostName();
                log.trace("Hostname (z InetAddress): {}", inetHostname );
                return inetHostname;
            }
            catch ( UnknownHostException e ) {
                throw new RuntimeException( "Nelze zjistit hostname.", e );
            }
        }

        // v linuxu toto nefunguje - asi je to proto ze v linuxu je to HOSTNAME,
        // ale neovereno
        // String computerName = System.getenv().get( "COMPUTERNAME" );
    }

}
