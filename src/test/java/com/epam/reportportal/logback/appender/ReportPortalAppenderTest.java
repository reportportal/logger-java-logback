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
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ReportPortalAppenderTest {

    @Mock
    private ReportPortalClient client;

    private final ExecutorService executor = CommonUtils.testExecutor();
    private final Scheduler scheduler = Schedulers.from(executor);

    private static Logger createLoggerFor(Class<?> clazz) {
        LoggerContext lc = new LoggerContext();
        PatternLayoutEncoder ple = new PatternLayoutEncoder();

        ple.setPattern("%date %level [%thread] %logger{10} [%file:%line] %msg%n");
        ple.setContext(lc);
        ple.start();
        ReportPortalAppender appender = new ReportPortalAppender();
        appender.setEncoder(ple);
        appender.setContext(lc);
        appender.start();

        Logger logger = lc.getLogger(clazz);
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

    @Test
    @SuppressWarnings({"unchecked", "ReactiveStreamsUnusedPublisher"})
    public void test_binary_file_message_encoding() throws IOException {
        mockBatchLogging(client);
        LoggingContext.init(Maybe.just("launch_uuid"), Maybe.just("item_uuid"), client, scheduler);
        String message = "test message";
        Logger logger = createLoggerFor(this.getClass());
        byte[] content;
        try (InputStream is = ofNullable(Thread.currentThread().getContextClassLoader().getResourceAsStream("pug/unlucky.jpg"))
                .orElseThrow(() -> new IllegalStateException("Unable to find test image file"))) {
            content = IOUtils.toByteArray(is);
        }
        logger.info("RP_MESSAGE#BASE64#{}#{}", Base64.getEncoder().encodeToString(content), message);
        LoggingContext.complete();
        ArgumentCaptor<List<MultipartBody.Part>> captor = ArgumentCaptor.forClass(List.class);
        verify(client).log(captor.capture());

        List<MultipartBody.Part> request = captor.getValue();
        assertThat(request, hasSize(2));

        RequestBody jsonPart = request.get(0).body();
        MediaType jsonPartType = jsonPart.contentType();
        assertThat(jsonPartType, notNullValue());
        assertThat(jsonPartType.toString(), Matchers.startsWith("application/json"));

        RequestBody binaryPart = request.get(1).body();
        MediaType binaryPartType = binaryPart.contentType();
        assertThat(binaryPartType, notNullValue());
        assertThat(binaryPartType.toString(), equalTo("image/jpeg"));
        assertThat(binaryPart.contentLength(), equalTo((long) content.length));
    }
}
