/**
 * Copyright 2016-2017 Emmanuel Keller / QWAZR
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
package com.qwazr.library.mybatis;

import com.qwazr.library.annotations.Library;
import com.qwazr.library.test.AbstractLibraryTest;
import com.qwazr.utils.IOUtils;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Assert;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

public class MybatisTest extends AbstractLibraryTest {

	@Library("mybatis_default")
	private MybatisConnector mybatis_default;

	@Library("mybatis_file")
	private MybatisConnector mybatis_file;

	private void checkSession(SqlSessionFactory sessionFactory) throws SQLException {
		Assert.assertNotNull(sessionFactory);
		final SqlSession session = sessionFactory.openSession();
		try (final Connection connection = session.getConnection()) {
			Assert.assertNotNull(connection);
		}
	}

	private void checkSessionFactory(MybatisConnector mybatis) throws SQLException {
		Assert.assertNotNull(mybatis);
		checkSession(mybatis.getSqlSessionFactory());
		IOUtils.CloseableContext context = new IOUtils.CloseableList();
		checkSession(mybatis.getSqlSessionFactory(context));
		IOUtils.close(context);
	}

	@Test
	public void mybatis_default() throws SQLException {
		checkSessionFactory(mybatis_default);
	}

	@Test
	public void mybatis_file() throws SQLException {
		checkSessionFactory(mybatis_file);
	}
}
