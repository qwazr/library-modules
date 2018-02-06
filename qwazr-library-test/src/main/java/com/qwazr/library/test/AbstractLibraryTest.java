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
package com.qwazr.library.test;

import com.qwazr.database.TableServiceInterface;
import com.qwazr.database.TableSingleton;
import com.qwazr.library.LibraryManager;
import com.qwazr.utils.FileUtils;
import com.qwazr.utils.reflection.InstancesSupplier;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

public abstract class AbstractLibraryTest {

	private static LibraryManager libraryManager;

	private static TableSingleton tableSingleton;

	@BeforeClass
	public static void init() throws IOException {
		if (libraryManager != null)
			return;
		final Path dataDirectory = Files.createTempDirectory("library-test");
		final Collection<File> etcFiles = Arrays.asList(new File("src/test/resources/etc/library.json"));
		final TableSingleton tableSingleton = new TableSingleton(dataDirectory, null);
		final InstancesSupplier instancesSupplier = InstancesSupplier.withConcurrentMap();
		instancesSupplier.registerInstance(TableServiceInterface.class, tableSingleton.getTableManager().getService());
		libraryManager = new LibraryManager(dataDirectory.toFile(), etcFiles, instancesSupplier);
		final File resourcesDirectory = new File("src/test/resources");
		if (resourcesDirectory.exists())
			FileUtils.copyDirectory(resourcesDirectory, dataDirectory.toFile());
	}

	@AfterClass
	public static void cleanup() {
		if (tableSingleton != null) {
			tableSingleton.close();
			tableSingleton = null;
		}
	}

	@Before
	public void inject() {
		libraryManager.inject(this);
	}

}
