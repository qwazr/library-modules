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
package com.qwazr.library.files;

import com.qwazr.component.ComponentsManager;
import com.qwazr.library.annotations.Library;
import com.qwazr.library.test.AbstractLibraryTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class FilesTest extends AbstractLibraryTest {

	@Library("files")
	private FilesTool files;

	@Test
	public void testLibrary() throws IOException {
		Assert.assertNotNull(files);
		files.browse("src/test/java", 1, null);
	}

	@Test
	public void testComponent() throws IOException, ClassNotFoundException {
		Assert.assertNotNull(new ComponentsManager().registerServices().getComponents().get(FilesTool.class.getName()));
	}
}
