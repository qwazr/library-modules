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

import com.qwazr.library.annotations.Library;
import com.qwazr.library.test.AbstractLibraryTest;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class EmailConnectorTest extends AbstractLibraryTest {

	@Library("email")
	private EmailConnector email;

	private Map<String, Object> getNewParams() {
		final Map<String, Object> params = new HashMap<>();
		params.put("subject", RandomStringUtils.randomAlphanumeric(10));
		params.put("from_email",
				RandomStringUtils.randomAlphanumeric(10) + "@" + RandomStringUtils.randomAlphanumeric(10) + ".test");
		params.put("from_name", RandomStringUtils.randomAlphanumeric(10));
		return params;
	}

	private void checkEmail(final Map<String, Object> params, final Email email) {
		Assert.assertNotNull(email);
		Assert.assertEquals(params.get("subject"), email.getSubject());
		Assert.assertEquals(params.get("from_email"), email.getFromAddress().getAddress());
		Assert.assertEquals(params.get("from_name"), email.getFromAddress().getPersonal());
	}

	@Test
	public void simpleEmail() throws IOException, SQLException, EmailException {
		final Map<String, Object> params = getNewParams();
		checkEmail(params, email.getNewSimpleEmail(params));
	}

	@Test
	public void htmlEmail() throws IOException, SQLException, EmailException {
		final Map<String, Object> params = getNewParams();
		checkEmail(params, email.getNewHtmlEmail(params));
	}

	@Test
	public void imageEmail() throws IOException, SQLException, EmailException {
		final Map<String, Object> params = getNewParams();
		checkEmail(params, email.getNewImageHtmlEmail(params));
	}

	@Test
	public void multipartEmail() throws IOException, SQLException, EmailException {
		final Map<String, Object> params = getNewParams();
		checkEmail(params, email.getNewMultipartEmail(params));
	}
}
