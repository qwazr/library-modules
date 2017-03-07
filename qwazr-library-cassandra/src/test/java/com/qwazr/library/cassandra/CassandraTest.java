/**
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
package com.qwazr.library.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.qwazr.library.annotations.Library;
import com.qwazr.library.test.AbstractLibraryTest;
import com.qwazr.utils.concurrent.ThreadUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CassandraTest extends AbstractLibraryTest {

	private final static Logger logger = Logger.getLogger(CassandraTest.class.getName());

	@Library("cassandra")
	private CassandraConnector cassandra;

	private final static String CREATE_SCHEMA = "CREATE KEYSPACE qwazr_connector_test WITH REPLICATION "
			+ "= { 'class' : 'SimpleStrategy', 'replication_factor' : 3 }";

	private final static String CREATE_TABLE = "CREATE TABLE qwazr_connector_test.test"
			+ "(item_id timeuuid, cat_id int, status text, PRIMARY KEY (item_id))";

	private final static String CREATE_INDEX = "CREATE INDEX ON qwazr_connector_test.test(cat_id)";

	@Test
	public void test_02_create() throws IOException {
		try {
			Assert.assertTrue(cassandra.execute(CREATE_SCHEMA).wasApplied());
			Assert.assertTrue(cassandra.execute(CREATE_TABLE).wasApplied());
			Assert.assertTrue(cassandra.execute(CREATE_INDEX).wasApplied());
		} catch (NoHostAvailableException e) {
			logger.warning("Bypass (no cassandra host is running)");
		}
	}

	private static long finalTime = 0;

	@Test
	public void test_10_transaction() throws Exception {
		try {
			cassandra.execute("SELECT count(*) FROM qwazr_connector_test.test").all();
			finalTime = System.currentTimeMillis() + 10000;
			List<ThreadUtils.ParallelRunnable> threadList = new ArrayList<>();
			for (int i = 0; i < 50; i++) {
				threadList.add(new InsertThread());
				threadList.add(new SelectUpdateThread());
			}
			ThreadUtils.parallel(threadList);
		} catch (NoHostAvailableException e) {
			logger.warning("Bypass (no cassandra host is running)");
		}
	}

	private final static String DROP_TABLE = "DROP TABLE qwazr_connector_test.test";
	private final static String DROP_SCHEMA = "DROP SCHEMA qwazr_connector_test";

	@Test
	public void test_98_drop() throws IOException {
		try {
			Assert.assertTrue(cassandra.execute(DROP_TABLE).wasApplied());
			Assert.assertTrue(cassandra.execute(DROP_SCHEMA).wasApplied());
		} catch (NoHostAvailableException e) {
			logger.warning("Bypass (no cassandra host is running)");
		}
	}

	private String INSERT = "INSERT INTO qwazr_connector_test.test " + "(item_id, cat_id) VALUES (now(), ?)";

	private class InsertThread implements ThreadUtils.ParallelRunnable {

		@Override
		public void run() throws Exception {
			long id = Thread.currentThread().getId();
			logger.info("Starts - id: " + id);
			int count = 0;
			while (System.currentTimeMillis() < finalTime) {
				Assert.assertTrue(cassandra.execute(INSERT, RandomUtils.nextInt(0, 10)).wasApplied());
				count++;
			}
			logger.info("Ends - id: " + id + " - count: " + count);
		}
	}

	private String SELECT = "SELECT * FROM qwazr_connector_test.test" + " WHERE cat_id=?";

	private String UPDATE = "UPDATE qwazr_connector_test.test" + " SET status='ok' WHERE item_id=?";

	private class SelectUpdateThread implements ThreadUtils.ParallelRunnable {

		@Override
		public void run() throws Exception {
			long id = Thread.currentThread().getId();
			logger.info("Starts - id: " + id);
			int count = 0;
			while (System.currentTimeMillis() < finalTime) {
				ResultSet result = cassandra.execute(SELECT, RandomUtils.nextInt(0, 10));
				Iterator<Row> it = result.iterator();
				while (it.hasNext()) {
					Row row = it.next();
					cassandra.execute(UPDATE, row.getUUID("item_id")).wasApplied();
					count++;
				}
			}
			logger.info("Ends - id: " + id + " - count: " + count);
		}
	}
}
