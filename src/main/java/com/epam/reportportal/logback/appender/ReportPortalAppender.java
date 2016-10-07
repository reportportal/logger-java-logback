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
import com.epam.reportportal.logback.appender.classic.ClassicAppenderService;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * Logback classic module appender for report portal
 */
public class ReportPortalAppender extends AppenderBase<ILoggingEvent> {

	private Supplier<IAppenderService> classicAppenderService;

	private PatternLayoutEncoder encoder;

	public ReportPortalAppender() {
		/*
		 * Lazy initialization of appender service. We are adding this due to
		 * appender service uses logging by yourself (httpclient, for example).
		 * This is a cause of logback substitution issue described here:
		 * http://www.slf4j.org/codes.html#substituteLogger
		 */
		classicAppenderService = Suppliers.memoize(new Supplier<IAppenderService>() {

			@Override
			public IAppenderService get() {
				return new ClassicAppenderService();
			}
		});
	}

	@Override
	protected void append(ILoggingEvent eventObject) {
		classicAppenderService.get().processEvent(eventObject, encoder);
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
