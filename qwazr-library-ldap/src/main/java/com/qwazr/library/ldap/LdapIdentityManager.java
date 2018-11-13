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

import com.qwazr.utils.LoggerUtils;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;
import io.undertow.security.idm.PasswordCredential;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.filter.FilterEncoder;
import org.apache.directory.ldap.client.api.LdapConnection;

import java.io.IOException;
import java.security.Principal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LdapIdentityManager implements IdentityManager {

    private static final Logger LOGGER = LoggerUtils.getLogger(LdapIdentityManager.class);

    private LdapConnector connector;

    public LdapIdentityManager(final LdapConnector connector) {
        this.connector = connector;
    }

    @Override
    public Account verify(final Account account) {
        return account;
    }

    @Override
    public LdapAccount verify(final String id, final Credential credential) {

        // This realm only support one type of credential
        if (!(credential instanceof PasswordCredential))
            throw new RuntimeException("Unsupported credential type: " + credential.getClass().getName());

        final PasswordCredential passwordCredential = (PasswordCredential) credential;

        try (LdapConnection connection = connector.getConnection(null, 60000L)) {
            // We request the database

            final String userFilter = "(&(objectClass=inetOrgPerson)(uid=" + FilterEncoder.encodeFilterValue(id) + "))";

            final Entry entry = connector.auth(connection, userFilter, new String(passwordCredential.getPassword()));
            if (entry == null)
                return authenticationFailure("Authentication error: " + userFilter, null);

            final String roleFilter =
                    "(&(objectClass=groupOfNames)(member=" + FilterEncoder.encodeFilterValue(entry.getDn().toString()) +
                            "))";

            final LinkedHashSet<String> roles = new LinkedHashSet<>();
            final List<Entry> roleEntries = connector.search(connection, roleFilter, 0, 1000);
            if (roleEntries != null) {
                roleEntries.forEach(e -> {
                    try {
                        roles.add(e.get("cn").getString());
                    } catch (LdapInvalidAttributeValueException e1) {
                        authenticationFailure(e1.getMessage(), e1);
                    }
                });
            }

            return new LdapAccount(id, roles);
        } catch (IOException | LdapException | CursorException e) {
            return authenticationFailure(e.getMessage(), e);
        }
    }

    private LdapAccount authenticationFailure(final String msg, final Throwable cause) {
        if (cause != null)
            LOGGER.log(Level.WARNING, cause, cause::getMessage);
        else
            LOGGER.warning(msg);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, e, e::getMessage);
        }
        return null;
    }

    @Override
    public Account verify(final Credential credential) {
        return null;
    }

    public static class LdapAccount implements Account, Principal {

        private final String name;
        private final Set<String> roles;

        private LdapAccount(final String name, final Set<String> roles) {
            this.name = name;
            this.roles = roles;
        }

        @Override
        public Principal getPrincipal() {
            return this;
        }

        @Override
        public Set<String> getRoles() {
            return roles;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
