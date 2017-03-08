/**
 * Copyright 2016 Emmanuel Keller / QWAZR
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
package com.qwazr.library.freemarker;

import com.qwazr.library.annotations.Library;
import com.qwazr.library.test.AbstractLibraryTest;
import freemarker.template.TemplateException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FreemarkerTest extends AbstractLibraryTest {

	private final static String TEMPLATE_RESOURCE = "com/qwazr/library/freemarker/template.ftl";
	private final static String TEST_TEXT = "Hello world!";

	@Library("freemarker_classloader")
	private FreeMarkerTool freemarker_classloader;

	@Library("freemarker_files")
	private FreeMarkerTool freemarker_files;

	private Map<String, Object> getVariables() {
		Map<String, Object> variables = new HashMap<>();
		variables.put("test", TEST_TEXT);
		return variables;
	}

	@Test
	public void classloaderTemplate() throws IOException, TemplateException {
		Assert.assertNotNull(freemarker_classloader);
		String test = freemarker_classloader.template(TEMPLATE_RESOURCE, getVariables());
		Assert.assertNotNull(test);
		Assert.assertTrue(test.contains(TEST_TEXT));
	}

	@Test
	public void fileTemplate() throws IOException, TemplateException {
		Assert.assertNotNull(freemarker_files);
		String test = freemarker_files.template(TEMPLATE_RESOURCE, getVariables());
		Assert.assertNotNull(test);
		Assert.assertTrue(test.contains(TEST_TEXT));
	}

	@Test
	public void builder() {
		final FreeMarkerToolBuilder builder = FreeMarkerTool.of();
		Assert.assertNotNull(builder);
		final FreeMarkerTool tool =
				builder.defaultContentType("TEXT/HTML").defaultEncoding("UTF-8").outputEncoding("UTF-8").build();
		Assert.assertNotNull(tool);
	}
}
