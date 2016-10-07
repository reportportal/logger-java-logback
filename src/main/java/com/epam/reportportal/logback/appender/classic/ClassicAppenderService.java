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
package com.epam.reportportal.logback.appender.classic;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.epam.reportportal.guice.Injector;
import com.epam.reportportal.listeners.ReportPortalListenerContext;
import com.epam.reportportal.message.MessageParser;
import com.epam.reportportal.message.ReportPortalMessage;
import com.epam.reportportal.service.BatchedReportPortalService;
import com.epam.reportportal.utils.files.ImageConverter;
import com.epam.reportportal.utils.properties.ListenerProperty;
import com.epam.reportportal.logback.appender.IAppenderService;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.common.io.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.UUID;

/**
 * Appender service for logback classic module.
 */
public class ClassicAppenderService implements IAppenderService {

	private final Logger logger = LoggerFactory.getLogger(ClassicAppenderService.class); //NOSONAR

	private BatchedReportPortalService reportPortalService;

	private MessageParser messageParser;

	private boolean shouldConvertImage;

	public ClassicAppenderService() {
		this.reportPortalService = Injector.getInstance().getBean(BatchedReportPortalService.class);
		this.messageParser = Injector.getInstance().getBean(MessageParser.class);
		String isConvertProperty = Injector.getInstance().getProperty(ListenerProperty.IS_CONVERT_IMAGE);
		shouldConvertImage = Boolean.parseBoolean(isConvertProperty == null ? "false" : isConvertProperty);

	}

	@Override
	public void processEvent(ILoggingEvent event, PatternLayoutEncoder encoder) {
		String currentItemId = ReportPortalListenerContext.getRunningNowItemId();
		if (null == currentItemId) {
			/* there is no active test. Leaving... */
			return;
		}

		ReportPortalMessage message = null;
		if (messageParser.supports(event.getFormattedMessage())) {
			/* formatting will be applied only once */
			message = messageParser.parse(event.getFormattedMessage());
		}

		/* If this logging event doesn't contain binary data so just format message in default way */
		String text = null == message ? encoder.getLayout().doLayout(event) : message.getMessage();

		SaveLogRQ saveLogRQ = buildSaveLogRQ(new Date(event.getTimeStamp()), currentItemId, text, event.getLevel().toString(),
				message == null ? null : message.getData());
		saveLogToRP(saveLogRQ);
	}

	/**
	 * Save log log object to report portal
	 *
	 * @param saveLogRQ Save Log request
	 */
	private void saveLogToRP(SaveLogRQ saveLogRQ) {
		try {
			reportPortalService.log(saveLogRQ);
		} catch (Exception e) {
			e.printStackTrace(); //NOSONAR
			logger.error("Unable to send log message to Report Portal.", e);
		}
	}

	/**
	 * Build {@link SaveLogRQ} object
	 */
	private SaveLogRQ buildSaveLogRQ(Date time, String currentItemId, String message, String level, final ByteSource content) {
		SaveLogRQ saveLogRQ = new SaveLogRQ();
		saveLogRQ.setMessage(message);
		saveLogRQ.setLogTime(time);
		saveLogRQ.setTestItemId(currentItemId);
		saveLogRQ.setLevel(level);

		if (null != content) {
			SaveLogRQ.File file = new SaveLogRQ.File();
			file.setName(UUID.randomUUID().toString());
			file.setContent(shouldConvertImage ? ImageConverter.convertIfImage(content) : content);
			saveLogRQ.setFile(file);
		}

		return saveLogRQ;
	}
}
