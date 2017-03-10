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

import com.qwazr.library.annotations.Library;
import com.qwazr.library.test.AbstractLibraryTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class PropertiesTest extends AbstractLibraryTest {

	@Library("properties")
	private PropertiesTool properties;

	@Test
	public void test() throws IOException {
		Assert.assertNotNull(properties);
		Assert.assertEquals("value1", properties.get("key1"));
		Assert.assertEquals("value2", properties.get("key2"));
		Assert.assertEquals(null, properties.get("key3"));
		properties.set("key3", "value3");
		Assert.assertEquals("value3", properties.get("key3"));
	}

}
