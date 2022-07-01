package com.epam.reportportal.logback.appender;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.LoggingContext;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.service.logs.LoggingSubscriber;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ReportPortalAppenderTest {

    @Mock
    private ReportPortalClient client;

    private final ExecutorService executor = CommonUtils.testExecutor();
    private final Scheduler scheduler = Schedulers.from(executor);

    private static Logger createLoggerFor(Class<?> clazz) {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
        ple.setContext(lc);
        ple.start();
        ReportPortalAppender appender = new ReportPortalAppender();
        appender.setEncoder(ple);
        appender.setContext(lc);
        appender.start();

        Logger logger = (Logger) LoggerFactory.getLogger(clazz);
        logger.addAppender(appender);
        logger.setLevel(Level.DEBUG);
        logger.setAdditive(false);
        return logger;
    }

    @SuppressWarnings("unchecked")
    private static void mockBatchLogging(ReportPortalClient client) {
        when(client.log(any(List.class))).thenReturn(Maybe.just(new BatchSaveOperatingRS()));
    }

    @AfterEach
    public void tearDown() {
        CommonUtils.shutdownExecutorService(executor);
    }

    @Test
    @SuppressWarnings({"unchecked", "ReactiveStreamsUnusedPublisher"})
    public void test_logger_append() {
        mockBatchLogging(client);
        LoggingContext.init(Maybe.just("launch_uuid"), Maybe.just("item_uuid"), client, scheduler);
        Logger logger = createLoggerFor(Launch.class);
        logger.info("test message");
        LoggingContext.complete();
        verify(client).log(any(List.class));
    }

    @Test
    @SuppressWarnings({"unchecked", "ReactiveStreamsUnusedPublisher"})
    public void test_logger_skip() {
        LoggingContext.init(Maybe.just("launch_uuid"), Maybe.just("item_uuid"), client, scheduler);
        Logger logger = createLoggerFor(LoggingSubscriber.class);
        logger.info("test message");
        LoggingContext.complete();
        verify(client, timeout(100).times(0)).log(any(List.class));
    }

}
