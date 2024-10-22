package sk.iway.iwcm.system.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;

/**
 * Trieda sluzi na logovanie do pamate pomocou logback
 */
public class InMemoryLoggerAppender extends AppenderBase<ILoggingEvent> {
    @Override
    protected void append(ILoggingEvent loggingEvent) {

        if ("com.zaxxer.hikari.pool.ProxyLeakTask".equals(loggingEvent.getLoggerName())) {
            auditEvent(Adminlog.TYPE_SQLERROR, loggingEvent);
        }

        if (!Constants.getBoolean("loggingInMemoryEnabled")) {
            return;
        }

        InMemoryLoggingDB.getInstance().add(new InMemoryLoggingEvent(loggingEvent));
    }

    /**
     * Log event into database (Adminlog table)
     * @param logType - type of event Adminlog.TYPE_*
     * @param loggingEvent
     */
    private void auditEvent(int logType, ILoggingEvent loggingEvent) {
        try {
            //get stacktrace from ILoggingEvent
            StringBuilder stackTraceBuilder = new StringBuilder();
            if (loggingEvent.getThrowableProxy() != null) {
                stackTraceBuilder.append(loggingEvent.getThrowableProxy().getClassName())
                                .append(": ")
                                .append(loggingEvent.getThrowableProxy().getMessage())
                                .append("\n");

                for (StackTraceElementProxy elementProxy : loggingEvent.getThrowableProxy().getStackTraceElementProxyArray()) {
                    stackTraceBuilder.append("\t")
                                    .append(elementProxy.toString())
                                    .append("\n");
                }
            }

            Adminlog.add(logType, loggingEvent.getFormattedMessage()+"\n"+stackTraceBuilder.toString(), -1, -1);
        } catch (Exception ex) {
            //do not use logger because we can get into infinite loop
            ex.printStackTrace(System.err);
        }
    }
}
