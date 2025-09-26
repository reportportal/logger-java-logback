/*
 * Copyright 2025 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.epam.reportportal.service.ReportPortal.emitLog;

/**
 * Logback classic module appender for report portal
 */
public class ReportPortalAppender extends AppenderBase<ILoggingEvent> {

	private static final MessageParser MESSAGE_PARSER = new HashMarkSeparatedMessageParser();
	private static final List<String> LOGGING_ISSUE = Collections.singletonList("com.epam.reportportal.service.logs.LoggingSubscriber");
	private PatternLayoutEncoder encoder;

	@Override
	protected void append(final ILoggingEvent event) {
		String loggerName = event.getLoggerName();
		for (String packagePrefix : LOGGING_ISSUE) {
			if (loggerName.startsWith(packagePrefix)) {
				return;
			}
		}

		emitLog(itemUuid -> {
			final String message = event.getFormattedMessage();
			final String level = event.getLevel().toString();
			final Instant time = event.getInstant();

			SaveLogRQ request = new SaveLogRQ();
			request.setLevel(level);
			request.setLogTime(time);
			request.setItemUuid(itemUuid);

			try {
				if (MESSAGE_PARSER.supports(message)) {
					ReportPortalMessage rpMessage = MESSAGE_PARSER.parse(message);
					TypeAwareByteSource data = rpMessage.getData();
					SaveLogRQ.File file = new SaveLogRQ.File();
					file.setContent(data.read());
					file.setContentType(data.getMediaType());
					file.setName(UUID.randomUUID().toString());

					request.setFile(file);
					request.setMessage(rpMessage.getMessage());
				} else {
					request.setMessage(encoder.getLayout().doLayout(event));
				}
			} catch (Exception e) {
				//skip
			}
			return request;
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

	@SuppressWarnings("unused")
	public PatternLayoutEncoder getEncoder() {
		return encoder;
	}

	public void setEncoder(PatternLayoutEncoder encoder) {
		this.encoder = encoder;
	}
}
