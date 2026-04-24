package com.example.order.allure;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Shared logger for all Allure instrumentation components.
 * Uses java.util.logging because ByteBuddy Advice classes cannot
 * reference SLF4J (class loading constraints in retransformed classes).
 * <p>
 * To see instrumentation debug logs, set the level:
 * {@code -Djava.util.logging.config.file=logging.properties}
 * or programmatically:
 * {@code AllureInstrumentationLogger.logger().setLevel(Level.FINE);}
 */
public final class AllureInstrumentationLogger {

    private static final Logger LOGGER = Logger.getLogger("com.example.order.allure");

    private AllureInstrumentationLogger() {
    }

    public static Logger logger() {
        return LOGGER;
    }

    public static void warn(String component, Throwable t) {
        LOGGER.log(Level.FINE, "[Allure " + component + "] " + t.getMessage(), t);
    }
}
