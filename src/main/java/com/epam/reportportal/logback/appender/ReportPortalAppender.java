/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/logger-java-logback
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.epam.reportportal.logback.appender;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.epam.reportportal.message.HashMarkSeparatedMessageParser;
import com.epam.reportportal.message.MessageParser;
import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.message.TypeAwareByteSource;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import rp.com.google.common.base.Function;

import java.util.Date;
import java.util.UUID;

import static com.epam.reportportal.service.ReportPortal.emitLog;

/**
 * Logback classic module appender for report portal
 */
public class ReportPortalAppender extends AppenderBase<ILoggingEvent> {

    private static final MessageParser MESSAGE_PARSER = new HashMarkSeparatedMessageParser();
    private PatternLayoutEncoder encoder;

    @Override
    protected void append(final ILoggingEvent event) {
        emitLog(new Function<String, com.epam.ta.reportportal.ws.model.log.SaveLogRQ>() {
            @Override
            public com.epam.ta.reportportal.ws.model.log.SaveLogRQ apply(String itemId) {
                final String message = event.getFormattedMessage();
                final String level = event.getLevel().toString();
                final Date time = new Date(event.getTimeStamp());

                SaveLogRQ rq = new SaveLogRQ();
                rq.setLevel(level);
                rq.setLogTime(time);
                rq.setTestItemId(itemId);

                try {
                    if (MESSAGE_PARSER.supports(message)) {
                        ReportPortalMessage rpMessage = MESSAGE_PARSER.parse(message);
                        TypeAwareByteSource data = rpMessage.getData();
                        com.epam.ta.reportportal.ws.model.log.SaveLogRQ.File file = new com.epam.ta.reportportal.ws.model.log.SaveLogRQ.File();
                        file.setContent(data.read());
                        file.setContentType(data.getMediaType());
                        file.setName(UUID.randomUUID().toString());

                        rq.setFile(file);
                        rq.setMessage(rpMessage.getMessage());
                    } else {
                        rq.setMessage(encoder.getLayout().doLayout(event));
                    }

                } catch (Exception e) {
                    //skip
                }

                return rq;
            }
        });
    }

    @Override
    public void start() {
        if (this.encoder == null) {
            addError("No encoder set for the appender named [" + name + "].");
            return;
        }
        this.encoder.start();
        super.start();
    }

    public PatternLayoutEncoder getEncoder() {
        return encoder;
    }

    public void setEncoder(PatternLayoutEncoder encoder) {
        this.encoder = encoder;
    }
}
