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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qwazr.library.AbstractPasswordLibrary;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.LoggerUtils;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.directory.api.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.ValidatingPoolableLdapConnectionFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LdapConnector extends AbstractPasswordLibrary implements Closeable {

	private static final Logger LOGGER = LoggerUtils.getLogger(LdapConnector.class);

	private static final String DEFAULT_PASSWORD_ATTRIBUTE = "userPassword";

	public final String host = null;
	public final Integer port = null;
	public final String username = null;
	public final String base_dn = null;
	public final Boolean use_pool = null;

	@JsonIgnore
	private volatile LdapConnectionPool connectionPool = null;

	@JsonIgnore
	private volatile LdapConnectionConfig config = null;

	@Override
	public void load() {
		config = new LdapConnectionConfig();
		if (host != null)
			config.setLdapHost(host);
		if (port != null)
			config.setLdapPort(port);
		if (username != null)
			config.setName(username);
		if (password != null)
			config.setCredentials(password);
		if (use_pool != null && use_pool) {
			ValidatingPoolableLdapConnectionFactory factory = new ValidatingPoolableLdapConnectionFactory(config);
			connectionPool = new LdapConnectionPool(factory);
			connectionPool.setTestOnBorrow(true);
		} else
			connectionPool = null;
	}

	@JsonIgnore
	public LdapConnection getConnection(final IOUtils.CloseableContext context, final Long timeOut)
			throws LdapException {
		LdapConnection connection;
		if (connectionPool != null)
			connection = connectionPool.getConnection();
		else
			connection = new LdapNetworkConnection(config);
		context.add(connection);
		if (timeOut != null)
			connection.setTimeOut(timeOut);
		return connection;
	}

	public Entry auth(LdapConnection connection, String user_filter, String password)
			throws LdapException, CursorException {
		Entry entry = getEntry(connection, user_filter);
		if (entry == null)
			throw new LdapException("User not found");
		Dn userDN = entry.getDn();
		connection.unBind();
		connection.bind(userDN, password);
		return entry;
	}

	public List<Entry> search(LdapConnection connection, String filter, int start, int rows)
			throws LdapException, CursorException {
		connection.bind();

		SearchRequest request = new SearchRequestImpl();
		request.setBase(new Dn(base_dn));
		request.setFilter(filter);
		request.setScope(SearchScope.SUBTREE);
		request.setSizeLimit(start + rows);

		SearchCursor cursor = connection.search(request);
		while (start > 0 && cursor.next())
			;
		final List<Entry> entries = new ArrayList<>();
		while (rows > 0 && cursor.next())
			entries.add(cursor.getEntry());
		return entries;
	}

	public int count(LdapConnection connection, String filter, int max) throws LdapException, CursorException {
		connection.bind();
		SearchRequest request = new SearchRequestImpl();
		request.setBase(new Dn(base_dn));
		request.setFilter(filter);
		request.setScope(SearchScope.SUBTREE);
		request.setSizeLimit(max);
		SearchCursor cursor = connection.search(request);
		int count = 0;
		while (cursor.next())
			count++;
		return count;
	}

	@JsonIgnore
	public Entry getEntry(LdapConnection connection, String filter) throws LdapException, CursorException {
		connection.bind();
		EntryCursor cursor = connection.search(base_dn, filter, SearchScope.SUBTREE);
		try {
			if (!cursor.next())
				return null;
			Entry entry = cursor.get();
			if (entry == null)
				throw new LdapException("No entry found");
			return entry;
		} finally {
			if (!cursor.isClosed())
				IOUtils.close(cursor);
		}
	}

	public void add(LdapConnection connection, String dn, Object... elements) throws LdapException {
		connection.bind();
		connection.add(new DefaultEntry(dn, elements));
	}

	public void createUser(LdapConnection connection, String dn, String passwordAttribute, String clearPassword,
			ScriptObjectMirror attrs) throws LdapException {
		connection.bind();
		Entry entry = new DefaultEntry(dn + ", " + base_dn);
		if (clearPassword != null)
			entry.add(passwordAttribute, getShaPassword(clearPassword));
		if (attrs != null) {
			for (Map.Entry<String, Object> attr : attrs.entrySet()) {
				String key = attr.getKey();
				Object value = attr.getValue();
				if (value instanceof String) {
					entry.add(key, (String) value);
				} else if (value instanceof ScriptObjectMirror) {
					ScriptObjectMirror som = (ScriptObjectMirror) value;
					if (som.isArray()) {
						for (Object obj : som.values())
							entry.add(key, obj.toString());
					} else
						throw new LdapException("Unsupported hash: " + som);
				} else
					throw new LdapException("Unsupported type: " + value.getClass());
			}
		}
		connection.add(entry);
	}

	public void createUser(LdapConnection connection, String dn, String clearPassword, ScriptObjectMirror attrs)
			throws LdapException {
		createUser(connection, dn, DEFAULT_PASSWORD_ATTRIBUTE, clearPassword, attrs);
	}

	public void updatePassword(LdapConnection connection, String dn, String passwordAttribute, String clearPassword)
			throws LdapException {
		connection.bind();
		Modification changePassword = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE,
				passwordAttribute, getShaPassword(clearPassword));
		connection.modify(dn + ", " + base_dn, changePassword);
	}

	public void updatePassword(LdapConnection connection, String dn, String clearPassword) throws LdapException {
		updatePassword(connection, dn, DEFAULT_PASSWORD_ATTRIBUTE, clearPassword);
	}

	public void updateString(LdapConnection connection, String dn, String attr, String... values) throws LdapException {
		connection.bind();
		Modification modif = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, attr, values);
		connection.modify(dn + ", " + base_dn, modif);
	}

	public byte[] getShaPassword(String clearPassword) {
		return PasswordUtil.createStoragePassword(clearPassword.getBytes(), LdapSecurityConstants.HASH_METHOD_SHA);
	}

	@Override
	public void close() {
		if (connectionPool != null && !connectionPool.isClosed()) {
			try {
				connectionPool.close();
				connectionPool = null;
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, e, e::getMessage);
			}
		}
	}

}
