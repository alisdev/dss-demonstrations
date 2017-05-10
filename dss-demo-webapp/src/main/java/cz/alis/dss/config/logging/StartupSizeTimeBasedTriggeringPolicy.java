package cz.alis.dss.config.logging;

import java.io.File;

import ch.qos.logback.core.joran.spi.NoAutoStart;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;

/**
 * Logback trigger for roll on startUp, size and time
 * <p>
 * Use TimeBasedRollingPolicy and add tag TimeBasedFileNamingAndTriggeringPolicy with this class
 * full name
 * 
 * @see SizeAndTimeBasedFNATP
 */
@NoAutoStart
public class StartupSizeTimeBasedTriggeringPolicy<E> extends SizeAndTimeBasedFNATP<E> {

    private boolean started = false;

    @Override
    public boolean isTriggeringEvent( File activeFile, E event ) {
        if ( !started ) {
            nextCheck = 0L; // Sometimes returned true dont help
            return started = true;
        }

        return super.isTriggeringEvent( activeFile, event );
    };
}