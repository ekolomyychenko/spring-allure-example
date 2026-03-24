package com.example.order.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.qameta.allure.Allure;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AllureLogsExtension implements TestExecutionListener {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    private static final String APPENDER_KEY = AllureLogsExtension.class.getName();

    @Override
    public void beforeTestMethod(TestContext testContext) {
        CapturingAppender appender = new CapturingAppender();
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        appender.setContext(loggerContext);
        appender.start();

        Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(appender);

        testContext.setAttribute(APPENDER_KEY, appender);
    }

    @Override
    public void afterTestMethod(TestContext testContext) {
        CapturingAppender appender = (CapturingAppender) testContext.getAttribute(APPENDER_KEY);
        if (appender == null) {
            return;
        }

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        root.detachAppender(appender);
        appender.stop();

        List<String> lines = appender.getLines();
        Allure.addAttachment("Application Logs", "text/plain",
                lines.isEmpty() ? "No logs captured" : String.join("\n", lines));
    }

    private static class CapturingAppender extends AppenderBase<ILoggingEvent> {

        private final List<String> lines = Collections.synchronizedList(new ArrayList<>());

        @Override
        protected void append(ILoggingEvent event) {
            String line = FMT.format(Instant.ofEpochMilli(event.getTimeStamp()))
                    + " " + String.format("%-5s", event.getLevel())
                    + " [" + event.getThreadName() + "] "
                    + event.getLoggerName()
                    + " - " + event.getFormattedMessage();
            lines.add(line);
        }

        public List<String> getLines() {
            return lines;
        }
    }
}
