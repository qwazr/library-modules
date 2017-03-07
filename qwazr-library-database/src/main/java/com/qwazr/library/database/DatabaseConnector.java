/**
 * Copyright 2015-2016 Emmanuel Keller / QWAZR
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
package com.qwazr.library.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qwazr.library.AbstractPasswordLibrary;
import com.qwazr.utils.IOUtils.CloseableContext;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.SubstitutedVariables;
import com.qwazr.utils.jdbc.Transaction;
import com.qwazr.utils.jdbc.connection.ConnectionManager;
import com.qwazr.utils.jdbc.connection.DataSourceConnection;
import com.qwazr.utils.jdbc.connection.JDBCConnection;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.SQLException;

public class DatabaseConnector extends AbstractPasswordLibrary implements Closeable {

	private static final Logger LOGGER = LoggerFactory.getLogger(com.qwazr.connectors.DatabaseConnector.class);

	public final String driver = null;

	public final String url = null;

	public final String username = null;

	public final ConnectionPool pool = null;

	public static class ConnectionPool {

		public final Integer initial_size = null;

		public final Integer max_total = null;

		public final Integer max_idle = null;

		public final Integer min_idle = null;

		public final Long max_wait_millis = null;

		public final Boolean abandoned_usage_tracking = null;

		public final Boolean log_abandoned = null;

		public final Boolean log_expired_connections = null;
	}

	@JsonIgnore
	private volatile ConnectionManager connectionManager = null;

	@JsonIgnore
	private volatile BasicDataSource basicDataSource = null;

	@Override
	public void load() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
		if (pool == null) {
			JDBCConnection cnx = new JDBCConnection();
			if (!StringUtils.isEmpty(driver))
				cnx.setDriver(libraryManager.getClassLoaderManager().getClassLoader(),
						SubstitutedVariables.propertyAndEnvironmentSubstitute(driver));
			if (!StringUtils.isEmpty(url))
				cnx.setUrl(SubstitutedVariables.propertyAndEnvironmentSubstitute(url));
			if (!StringUtils.isEmpty(username))
				cnx.setUsername(SubstitutedVariables.propertyAndEnvironmentSubstitute(username));
			if (!StringUtils.isEmpty(password))
				cnx.setPassword(SubstitutedVariables.propertyAndEnvironmentSubstitute(password));
			connectionManager = cnx;
			basicDataSource = null;
		} else {
			basicDataSource = new BasicDataSource();
			if (driver != null)
				basicDataSource.setDriverClassName(SubstitutedVariables.propertyAndEnvironmentSubstitute(driver));
			if (url != null)
				basicDataSource.setUrl(SubstitutedVariables.propertyAndEnvironmentSubstitute(url));
			if (username != null)
				basicDataSource.setUsername(SubstitutedVariables.propertyAndEnvironmentSubstitute(username));
			if (password != null)
				basicDataSource.setPassword(SubstitutedVariables.propertyAndEnvironmentSubstitute(password));
			if (pool.initial_size != null)
				basicDataSource.setInitialSize(pool.initial_size);
			if (pool.min_idle != null)
				basicDataSource.setMinIdle(pool.min_idle);
			if (pool.max_idle != null)
				basicDataSource.setMaxIdle(pool.max_idle);
			if (pool.max_total != null)
				basicDataSource.setMaxTotal(pool.max_total);
			if (pool.max_wait_millis != null)
				basicDataSource.setMaxWaitMillis(pool.max_wait_millis);
			if (pool.log_abandoned != null)
				basicDataSource.setLogAbandoned(pool.log_abandoned);
			if (pool.log_expired_connections != null)
				basicDataSource.setLogExpiredConnections(pool.log_expired_connections);
			if (pool.abandoned_usage_tracking != null)
				basicDataSource.setAbandonedUsageTracking(pool.abandoned_usage_tracking);
			connectionManager = new DataSourceConnection(basicDataSource);
		}
	}

	@Override
	public void close() {
		if (basicDataSource != null) {
			try {
				if (!basicDataSource.isClosed())
					basicDataSource.close();
				basicDataSource = null;
			} catch (SQLException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

	@JsonIgnore
	public Transaction getConnection(final CloseableContext context) throws SQLException {
		final Transaction transaction = connectionManager.getNewTransaction();
		return context.add(transaction);
	}

	@JsonIgnore
	public Transaction getConnection(final CloseableContext context, final boolean autoCommit) throws SQLException {
		final Transaction transaction = connectionManager.getNewTransaction(autoCommit);
		return context.add(transaction);
	}

	@JsonIgnore
	public Transaction getConnection(final CloseableContext context, final boolean autoCommit,
			final int transactionIsolation) throws SQLException {
		final Transaction transaction = connectionManager.getNewTransaction(autoCommit, transactionIsolation);
		return context.add(transaction);
	}

	/**
	 * The current number of active connections that have been allocated from
	 * this connection pool.
	 *
	 * @return the current number of active connections
	 */
	public Integer getPoolNumActive() {
		if (basicDataSource == null)
			return null;
		return basicDataSource.getNumActive();
	}

	/**
	 * The current number of idle connections that are waiting to be allocated
	 * from this connection pool.
	 *
	 * @return the current number of idle connections
	 */
	public Integer getPoolNumIdle() {
		if (basicDataSource == null)
			return null;
		return basicDataSource.getNumIdle();
	}

}
