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

import com.qwazr.utils.IOUtils;
import freemarker.cache.TemplateLoader;

import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

class FileTemplateLoader implements TemplateLoader {

	private final File parentDirectory;

	FileTemplateLoader(final File parentDirectory) {
		this.parentDirectory = parentDirectory;
	}

	@Override
	public Object findTemplateSource(final String path) throws IOException {
		final File file = new File(parentDirectory, path);
		return file.exists() && file.isFile() ? file : null;
	}

	@Override
	public long getLastModified(final Object templateSource) {
		return ((File) templateSource).lastModified();
	}

	@Override
	public Reader getReader(final Object templateSource, final String encoding) throws IOException {
		return new FileReader((File) templateSource);
	}

	@Override
	public void closeTemplateSource(final Object templateSource) throws IOException {
		if (templateSource instanceof Closeable)
			IOUtils.closeQuietly((Closeable) templateSource);
	}

}