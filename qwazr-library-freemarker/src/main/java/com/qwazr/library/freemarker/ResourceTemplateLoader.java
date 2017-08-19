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
package com.qwazr.library.freemarker;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qwazr.utils.IOUtils;
import freemarker.cache.TemplateLoader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.function.Function;

public class ResourceTemplateLoader implements TemplateLoader {

	private final ClassLoader classLoader;
	private final Function<String, String> pathModifier;

	public ResourceTemplateLoader(ClassLoader classLoader, Function<String, String> pathModifier) {
		this.classLoader = classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
		this.pathModifier = pathModifier;
	}

	@Override
	public Object findTemplateSource(String path) throws IOException {
		if (pathModifier != null && path != null)
			path = pathModifier.apply(path);
		return path == null ? null : classLoader.getResourceAsStream(path);
	}

	@Override
	@JsonIgnore
	public long getLastModified(final Object templateSource) {
		return classLoader.hashCode();
	}

	@Override
	@JsonIgnore
	public Reader getReader(final Object templateSource, final String encoding) throws IOException {
		return new InputStreamReader((InputStream) templateSource);
	}

	@Override
	public void closeTemplateSource(final Object templateSource) throws IOException {
		if (templateSource instanceof Closeable)
			IOUtils.close((Closeable) templateSource);
	}

}