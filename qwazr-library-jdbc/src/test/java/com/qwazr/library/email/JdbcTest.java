package com.qwazr.library.email; /**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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

import com.qwazr.library.annotations.Library;
import com.qwazr.library.test.AbstractLibraryTest;
import com.qwazr.utils.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class JdbcTest extends AbstractLibraryTest {

	@Library("derbyNoPool")
	private JdbcConnector derbyNoPool;

	@Library("derbyWithPool")
	private JdbcConnector derbyWithPool;

	@Test
	public void noPool() throws IOException, SQLException {
		Assert.assertNotNull(derbyNoPool);
		try (IOUtils.CloseableContext context = new IOUtils.CloseableList()) {
			Assert.assertNotNull(derbyNoPool.getConnection(context));
			Assert.assertNotNull(derbyNoPool.getConnection(context, true));
			Assert.assertNotNull(derbyNoPool.getConnection(context, true, Connection.TRANSACTION_READ_COMMITTED));
			Assert.assertNull(derbyNoPool.getPoolNumActive());
			Assert.assertNull(derbyNoPool.getPoolNumIdle());
		}
	}

	@Test
	public void withPool() throws IOException, SQLException {
		Assert.assertNotNull(derbyWithPool);
		try (IOUtils.CloseableContext context = new IOUtils.CloseableList()) {
			Assert.assertNotNull(derbyWithPool.getConnection(context));
			Assert.assertNotNull(derbyWithPool.getConnection(context, true));
			Assert.assertNotNull(derbyWithPool.getConnection(context, true, Connection.TRANSACTION_READ_COMMITTED));
			Assert.assertEquals(new Integer(3), derbyWithPool.getPoolNumActive());
			Assert.assertEquals(new Integer(7), derbyWithPool.getPoolNumIdle());
		}
	}

}
