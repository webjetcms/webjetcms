package sk.iway.iwcm.system.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import sk.iway.iwcm.Constants;

/**
 * Trieda sluzi na logovanie do pamate pomocou logback
 */
public class InMemoryLoggerAppender extends AppenderBase<ILoggingEvent> {
    @Override
    protected void append(ILoggingEvent loggingEvent) {
        if (!Constants.getBoolean("loggingInMemoryEnabled")) {
            return;
        }

        InMemoryLoggingDB.getInstance().add(new InMemoryLoggingEvent(loggingEvent));
    }
}
