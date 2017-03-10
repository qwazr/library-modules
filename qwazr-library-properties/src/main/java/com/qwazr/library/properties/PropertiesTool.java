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
package com.qwazr.library.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qwazr.library.AbstractLibrary;

import java.io.*;
import java.util.Objects;
import java.util.Properties;

public class PropertiesTool extends AbstractLibrary {

	final public String path;

	@JsonIgnore
	final public Properties properties = new Properties();

	@JsonIgnore
	private volatile boolean isXML;

	@JsonIgnore
	private volatile File propertiesFile;

	@JsonIgnore
	private volatile long lastModified = 0;

	@JsonIgnore
	private volatile String comments;

	public PropertiesTool() {
		this(null);
	}

	public PropertiesTool(String path) {
		this.path = path;
	}

	@Override
	public void load() {
		propertiesFile = path != null ? new File(path) : null;
		isXML = propertiesFile != null && propertiesFile.getName().endsWith(".xml");
	}

	private Properties checkProperties() throws IOException {
		if (propertiesFile == null)
			return properties;
		synchronized (properties) {
			final long lastMod = propertiesFile.lastModified();
			if (propertiesFile.exists() && lastMod == lastModified)
				return properties;
			if (isXML)
				loadFromXML();
			else
				loadFromText();
			lastModified = lastMod;
			return properties;
		}
	}

	/**
	 * Get the current value for the given key
	 *
	 * @param key
	 * @return the value of the key
	 * @throws IOException
	 */
	public String get(String key) throws IOException {
		return get(key, null);
	}

	/**
	 * Get the current value for the given key
	 *
	 * @param key
	 * @param defaultValue
	 * @return
	 * @throws IOException
	 */
	public String get(String key, String defaultValue) throws IOException {
		return checkProperties().getProperty(key, defaultValue);
	}

	/**
	 * Set the value for the given key
	 *
	 * @param key
	 * @param value
	 * @throws IOException
	 */
	public void set(String key, String value) throws IOException {
		checkProperties().setProperty(key, value);
	}

	public Properties getProperties() {
		synchronized (properties) {
			return properties;
		}
	}

	public void save(String comments) throws IOException {
		Objects.requireNonNull(propertiesFile, "The property file cannot be saved The path is missing.");
		if (isXML)
			storeToXML();
		else
			storeToText();
	}

	/**
	 * Load the properties from a file in TEXT format.
	 *
	 * @throws IOException if any I/O error occurs
	 */
	private void loadFromText() throws IOException {
		loadFromText(properties, propertiesFile);
	}

	/**
	 * Load the properties
	 *
	 * @param properties
	 * @param file
	 * @throws IOException
	 */
	private static void loadFromText(Properties properties, File file) throws IOException {
		try (final FileInputStream fis = new FileInputStream(file)) {
			properties.load(fis);
		}
	}

	/**
	 * Load the properties from a TEXT file
	 *
	 * @param path the path to the TEXT file
	 * @return a new Properties object
	 */
	public Properties loadFromText(String path) throws IOException {
		Properties properties = new Properties();
		loadFromText(properties, new File(path));
		return properties;
	}

	/**
	 * Load the properties from a file in XML format.
	 *
	 * @throws IOException if any I/O error occurs
	 */
	private void loadFromXML() throws IOException {
		loadFromXML(properties, propertiesFile);
	}

	private static void loadFromXML(Properties properties, File file) throws IOException {
		try (final FileInputStream fis = new FileInputStream(file)) {
			properties.loadFromXML(fis);
		}
	}

	/**
	 * Load Properties from an XML file
	 *
	 * @param path the path to the XML file
	 * @return a new Properties object
	 */
	public Properties loadFromXML(String path) throws IOException {
		Properties properties = new Properties();
		loadFromXML(properties, new File(path));
		return properties;
	}

	/**
	 * Store the properties to a file in TEXT format.
	 *
	 * @throws IOException if any I/O error occurs
	 */
	private void storeToText() throws IOException {
		try (final FileWriter fw = new FileWriter(propertiesFile)) {
			properties.store(fw, comments);
		}
	}

	/**
	 * Stores the properties to a file in XML format.
	 *
	 * @throws IOException if any I/O error occurs
	 */
	private void storeToXML() throws IOException {
		try (final FileOutputStream fos = new FileOutputStream(propertiesFile)) {
			properties.storeToXML(fos, comments, "UTF-8");
		}
	}
}