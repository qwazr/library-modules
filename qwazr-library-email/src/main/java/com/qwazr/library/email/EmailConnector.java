/**
 * Copyright 2015-2017 Emmanuel Keller / QWAZR
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package com.qwazr.library.email;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qwazr.library.AbstractPasswordLibrary;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.ImageHtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;

import java.util.Map;

public class EmailConnector extends AbstractPasswordLibrary {

	public final static String PARAM_SUBJECT = "subject";
	public final static String PARAM_FROM_EMAIL = "from_email";
	public final static String PARAM_FROM_NAME = "from_name";

	public final String hostname = null;
	public final Integer port = null;
	public final String username = null;
	public final Boolean ssl = null;
	public final Boolean start_tls_enabled = null;
	public final Boolean start_tls_required = null;
	public final Integer connection_timeout = null;
	public final Integer timeout = null;

	public void sendEmail(final Email email) throws EmailException {
		email.setHostName(hostname);
		if (ssl != null)
			email.setSSLOnConnect(ssl);
		if (start_tls_enabled != null)
			email.setStartTLSEnabled(start_tls_enabled);
		if (start_tls_required != null)
			email.setStartTLSRequired(start_tls_required);
		if (port != null)
			email.setSmtpPort(port);
		if (username != null)
			email.setAuthentication(username, password);
		if (connection_timeout != null)
			email.setSocketConnectionTimeout(connection_timeout);
		if (timeout != null)
			email.setSocketTimeout(timeout);
		email.send();
	}

	private void generic_params(final Email email, final Map<String, Object> params) throws EmailException {
		Object subject = params.get(PARAM_SUBJECT);
		if (subject != null)
			email.setSubject(subject.toString());
		Object from_email = params.get(PARAM_FROM_EMAIL);
		if (from_email != null) {
			Object from_name = params.get(PARAM_FROM_NAME);
			if (from_name != null)
				email.setFrom(from_email.toString(), from_name.toString());
			else
				email.setFrom(from_email.toString());
		}
	}

	@JsonIgnore
	public SimpleEmail getNewSimpleEmail(final Map<String, Object> params) throws EmailException {
		SimpleEmail email = new SimpleEmail();
		generic_params(email, params);
		return email;
	}

	@JsonIgnore
	public HtmlEmail getNewHtmlEmail(final Map<String, Object> params) throws EmailException {
		HtmlEmail email = new HtmlEmail();
		generic_params(email, params);
		return email;
	}

	@JsonIgnore
	public ImageHtmlEmail getNewImageHtmlEmail(final Map<String, Object> params) throws EmailException {
		ImageHtmlEmail email = new ImageHtmlEmail();
		generic_params(email, params);
		return email;
	}

	@JsonIgnore
	public MultiPartEmail getNewMultipartEmail(final Map<String, Object> params) throws EmailException {
		MultiPartEmail email = new MultiPartEmail();
		generic_params(email, params);
		return email;
	}

}
