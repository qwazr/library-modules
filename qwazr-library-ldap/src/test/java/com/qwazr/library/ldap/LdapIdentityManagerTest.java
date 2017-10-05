/*
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
 */
package com.qwazr.library.ldap;

import io.undertow.security.idm.PasswordCredential;
import org.junit.Assert;
import org.junit.Test;

public class LdapIdentityManagerTest {

	final static String HOST = System.getProperty("LdapIdentityManagerTest.Host", "localhost");
	final static Integer PORT = Integer.parseInt(System.getProperty("LdapIdentityManagerTest.Port", "389"));
	final static String ADMIN_USERNAME = System.getProperty("LdapIdentityManagerTest.AdminUsername");
	final static String ADMIN_PASSWORD = System.getProperty("LdapIdentityManagerTest.AdminPassword");
	final static String BASE_DN = System.getProperty("LdapIdentityManagerTest.BaseDn");
	final static String USERNAME = System.getProperty("LdapIdentityManagerTest.Username");
	final static String PASSWORD = System.getProperty("LdapIdentityManagerTest.Password");
	final static String ROLE = System.getProperty("LdapIdentityManagerTest.Role");

	@Test
	public void test() {
		if (BASE_DN == null)
			return;
		try (final LdapConnector ldapConnector = new LdapConnector(HOST, PORT, ADMIN_USERNAME, ADMIN_PASSWORD, BASE_DN,
				false)) {
			ldapConnector.load();
			final LdapIdentityManager identityManager = new LdapIdentityManager(ldapConnector);
			final LdapIdentityManager.LdapAccount account =
					identityManager.verify(USERNAME, new PasswordCredential(PASSWORD.toCharArray()));
			Assert.assertNotNull(account);
			Assert.assertNotNull(account.getPrincipal());
			Assert.assertEquals(USERNAME, account.getPrincipal().getName());
			Assert.assertEquals(USERNAME, account.getName());
			Assert.assertTrue(account.getRoles().contains(ROLE));
			Assert.assertTrue(account.getRoles().contains(ROLE));
		}
	}
}
