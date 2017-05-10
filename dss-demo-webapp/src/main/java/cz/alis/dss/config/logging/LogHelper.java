package cz.alis.dss.config.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

/**
 * Užitečné metody pro generické logování.
 */
public class LogHelper {

    /**
     * Zaloguje data do loggeru na level.
     * 
     * @param logger
     * @param level
     * @param format text
     * @param argArray data
     */
    public static void log( Logger logger, Level level, String format, Object... argArray ) {
        switch ( level.levelInt ) {
        case Level.OFF_INT:
            logger.trace( format, argArray );
            break;
        case Level.DEBUG_INT:
            logger.debug( format, argArray );
            break;
        case Level.INFO_INT:
            logger.info( format, argArray );
            break;
        case Level.WARN_INT:
            logger.warn( format, argArray );
            break;
        case Level.ERROR_INT:
            logger.error( format, argArray );
            break;
        }
    }

    /**
     * Zaloguje chybu do loggeru na level.
     * 
     * @param logger
     * @param level
     * @param format text
     * @param throwable chyba
     */

    public static void log( Logger logger, Level level, String format, Throwable throwable ) {
        switch ( level.levelInt ) {
        case Level.OFF_INT:
            logger.trace( format, throwable );
            break;
        case Level.DEBUG_INT:
            logger.debug( format, throwable );
            break;
        case Level.INFO_INT:
            logger.info( format, throwable );
            break;
        case Level.WARN_INT:
            logger.warn( format, throwable );
            break;
        case Level.ERROR_INT:
            logger.error( format, throwable );
            break;
        }
    }

    /**
     * Nakonfiguruje logback podle konfiguráku na classpath.
     * 
     * @param clazz
     * @param classPathResourcePath
     */
    public static void configureLogback( Class<?> clazz, String classPathResourcePath ) {
        configureLogback( clazz, clazz.getResourceAsStream( classPathResourcePath ) );
    }

    /**
     * Nakonfiguruje logback podle souboru.
     * 
     * @param clazz
     * @param configurationFile
     */
    public static void configureLogback( Class<?> clazz, File configurationFile ) {

        try {
            FileInputStream configurationStream = new FileInputStream( configurationFile );

            configureLogback( clazz, configurationStream );

            configurationStream.close();
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Nepodarilo se nacist soubor s konfiguraci logovani", e );
        }
    }

    /**
     * Nakonfiguruje logback podle konfiguráku ze streamu.
     * 
     * @param clazz
     * @param configurationStream
     */
    public static void configureLogback( Class<?> clazz, InputStream configurationStream ) {
        // assume SLF4J is bound to logback in the current environment
        LoggerContext lc = ( LoggerContext ) LoggerFactory.getILoggerFactory();

        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext( lc );
            // the context was probably already configured by default configuration rules
            lc.reset();
            configurator.doConfigure( configurationStream );
        }
        catch ( JoranException je ) {
            // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings( lc );
    }

    /**
     * @param logger
     * @param level
     * @return true, pokud je logování na vybraný level zapnuto.
     */
    public static boolean isLevelEnabled( Logger logger, Level level ) {
        switch ( level.levelInt ) {
        case Level.OFF_INT:
            return false;
        case Level.DEBUG_INT:
            return logger.isDebugEnabled();
        case Level.INFO_INT:
            return logger.isInfoEnabled();
        case Level.WARN_INT:
            return logger.isWarnEnabled();
        case Level.ERROR_INT:
            return logger.isErrorEnabled();
        }
        return false;
    }

    /**
     * Převede pole argumentů na řetězec vhodný k zápisu do logu.
     * 
     * @param b výstupní builder
     * @param args argumenty k zapsání
     */
    public static void argumentsToString( StringBuilder b, Object[] args ) {
        if ( args == null )
            return;

        int length = args.length;

        if ( length < 0 )
            return;

        if ( length == 0 ) {
            b.append( "[]" );
            return;
        }

        int iMax = length - 1;
        b.append( '[' );

        for ( int i = 0;; i++ ) {
            Object arg = args[i];

            LogHelper.objectToString( b, arg );

            if ( i == iMax )
                break;

            b.append( ", " );
        }
        b.append( ']' );
    }

    /** Maximální počet vypsaných prvků z polí a {@link Iterable}s. */
    private static final int ARRAY_LIMIT = 21;

    /**
     * Převede libovolný objekt na řetězec vhodný k vypsání do logu. Běžné objekty převádí pomocí
     * klasického toString().
     * <p>
     * Pole a {@link Iterable}s: vypíše jen prvních
     * 
     * @param b
     * @param object
     */
    public static void objectToString( StringBuilder b, Object object ) {
        if ( object == null )
            b.append( "null" );
        else if ( object.getClass().isArray() ) {
            // dlouha pole zkratit
            int len = Array.getLength( object );

            if ( len <= ARRAY_LIMIT )
                b.append( arrayToString( object ) );
            else {
                b.append( "[long-array]" );

                // jina varianta: vypsat zkracene pole
                // shortenArray( b, object, len );
            }
        }
        else if ( object instanceof Iterable ) {
            // dlouha pole zkratit
            Iterable<?> iterable = ( Iterable<?> ) object;

            if ( !iterable.iterator().hasNext() )
                b.append( "[]" );
            else {
                b.append( '[' );
                int j = 0;
                for ( Iterator<?> it = iterable.iterator();; ) {
                    j += 1;
                    b.append( String.valueOf( it.next() ) );

                    boolean hasNext = it.hasNext();
                    if ( !hasNext || j == ARRAY_LIMIT ) {
                        if ( hasNext )
                            b.append( ",..." );
                        break;
                    }
                    b.append( ", " );
                }
                b.append( ']' );
            }
        }
        else
            // vse ostatni proste vypsat
            b.append( String.valueOf( object ) );
    }

    /**
     * Převede libovolné pole na řetězec. Podporuje i pole primitivních typů.
     * 
     * @param object
     * @return
     */
    private static String arrayToString( Object object ) {
        Class<?> clazz = object.getClass();
        Class<?> elementType = clazz.getComponentType();
        if ( elementType.isPrimitive() ) {
            if ( elementType == boolean.class )
                return Arrays.toString( ( boolean[] ) object );
            else if ( elementType == byte.class )
                return Arrays.toString( ( byte[] ) object );
            else if ( elementType == char.class )
                return Arrays.toString( ( char[] ) object );
            else if ( elementType == double.class )
                return Arrays.toString( ( double[] ) object );
            else if ( elementType == float.class )
                return Arrays.toString( ( float[] ) object );
            else if ( elementType == int.class )
                return Arrays.toString( ( int[] ) object );
            else if ( elementType == long.class )
                return Arrays.toString( ( long[] ) object );
            else if ( elementType == short.class )
                return Arrays.toString( ( short[] ) object );
            else if ( elementType == int.class )
                return Arrays.toString( ( int[] ) object );
            throw new IllegalArgumentException( "Neznamy typ pole " + elementType.getName() );
        }
        else
            return Arrays.toString( ( Object[] ) object );
    }

    /**
     * Ořízne dlouhé pole na převede ho na řetězec.
     * <p>
     * Zatím nepoužito, ale ponechávám protože se v budoucnu může hodit.
     * 
     * @param b výstup builder
     * @param object pole
     * @param len délka pole
     */
    static void shortenArray( StringBuilder b, Object object, int len ) {
        int limit = Math.min( len, ARRAY_LIMIT );
        int border = limit - 1;

        b.append( '[' );
        for ( int j = 0;; j++ ) {
            b.append( String.valueOf( Array.get( object, j ) ) );
            if ( j == border )
                break;
            b.append( ", " );
        }
        if ( len > limit )
            b.append( ",..." );

        b.append( ']' );
    }
}
