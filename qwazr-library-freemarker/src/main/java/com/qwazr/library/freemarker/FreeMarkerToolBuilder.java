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
package com.qwazr.library.freemarker;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qwazr.library.LibraryManager;
import freemarker.cache.TemplateLoader;

import java.util.LinkedHashSet;
import java.util.Set;

public class FreeMarkerToolBuilder {

	String outputEncoding;
	String defaultEncoding;
	String defaultContentType;
	Boolean useClassloader;
	String templatePath;
	Boolean localizedLookup;
	Set<FreeMarkerTool.Loader> templateLoaders;

	final LibraryManager libraryManager;

	FreeMarkerToolBuilder(LibraryManager libraryManager) {
		this.libraryManager = libraryManager;
	}

	public FreeMarkerToolBuilder outputEncoding(String outputEncoding) {
		this.outputEncoding = outputEncoding;
		return this;
	}

	public FreeMarkerToolBuilder defaultEncoding(String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
		return this;
	}

	public FreeMarkerToolBuilder defaultContentType(String defaultContentType) {
		this.defaultContentType = defaultContentType;
		return this;
	}

	public FreeMarkerToolBuilder useClassloader(Boolean useClassloader) {
		this.useClassloader = useClassloader;
		return this;
	}

	public FreeMarkerToolBuilder templatePath(String templatePath) {
		this.templatePath = templatePath;
		return this;
	}

	public FreeMarkerToolBuilder localizedLookup(Boolean localizedLookup) {
		this.localizedLookup = localizedLookup;
		return this;
	}

	public FreeMarkerToolBuilder templateLoader(TemplateLoader templateLoader) {
		if (this.templateLoaders == null)
			this.templateLoaders = new LinkedHashSet<>();
		templateLoaders.add(new DirectLoader(templateLoader));
		return this;
	}

	public FreeMarkerToolBuilder templateLoader(FreeMarkerTool.Loader.Type type, String path) {
		if (this.templateLoaders == null)
			this.templateLoaders = new LinkedHashSet<>();
		templateLoaders.add(new FreeMarkerTool.Loader(type, path));
		return this;
	}

	public FreeMarkerTool build() {
		return new FreeMarkerTool(this);
	}

	static class DirectLoader extends FreeMarkerTool.Loader {

		private final TemplateLoader templateLoader;

		DirectLoader(TemplateLoader templateLoader) {
			super(null, null, templateLoader.hashCode());
			this.templateLoader = templateLoader;
		}

		@JsonIgnore
		public TemplateLoader build() {
			return templateLoader;
		}
	}
}
