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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qwazr.library.AbstractLibrary;
import com.qwazr.library.LibraryManager;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FreeMarkerTool extends AbstractLibrary implements Closeable {

	@JsonProperty("output_encoding")
	public final String outputEncoding;

	@JsonProperty("default_encoding")
	public final String defaultEncoding;

	@JsonProperty("default_content_type")
	public final String defaultContentType;

	@JsonProperty("use_classloader")
	public final Boolean useClassloader;

	@JsonProperty("template_path")
	public final String templatePath;

	@JsonProperty("localized_lookup")
	public final Boolean localizedLookup;

	@JsonProperty("template_loaders")
	public final List<Loader> templateLoaders;

	public static class Loader {

		public enum Type {resource, file}

		public final Type type;
		public final String path;

		@JsonIgnore
		private final int hashCode;

		Loader(@JsonProperty("type") Type type, @JsonProperty("path") String path) {
			this.type = type;
			this.path = path;
			hashCode = new HashCodeBuilder().append(type.ordinal()).append(path).build();
		}

		@JsonIgnore
		public TemplateLoader build() {
			final Type t = type == null ? Type.resource : type;
			switch (t) {
			case file:
				return new FileTemplateLoader(new File(Objects.requireNonNull(path, "The path is missing")));
			default:
			case resource:
				return new ResourceTemplateLoader(null, path);
			}
		}

		@JsonIgnore
		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null || !(o instanceof Loader))
				return false;
			final Loader l = (Loader) o;
			return Objects.equals(l.path, path) && type == l.type;
		}
	}

	@JsonIgnore
	private volatile Configuration cfg = null;

	private final static String DEFAULT_CHARSET = "UTF-8";
	private final static String DEFAULT_CONTENT_TYPE = "text/html";

	@JsonCreator
	FreeMarkerTool(@JsonProperty("output_encoding") String outputEncoding,
			@JsonProperty("default_encoding") String defaultEncoding,
			@JsonProperty("default_content_type") String defaultContentType,
			@JsonProperty("use_classloader") Boolean useClassloader, @JsonProperty("template_path") String templatePath,
			@JsonProperty("localized_lookup") Boolean localizedLookup,
			@JsonProperty("template_loaders") Collection<Loader> templateLoaders) {
		this.outputEncoding = outputEncoding;
		this.defaultEncoding = defaultEncoding;
		this.defaultContentType = defaultContentType;
		this.useClassloader = useClassloader;
		this.templatePath = templatePath;
		this.localizedLookup = localizedLookup;
		this.templateLoaders = templateLoaders == null ? null : new ArrayList<>(templateLoaders);
	}

	FreeMarkerTool(FreeMarkerToolBuilder builder) {
		this(builder.outputEncoding, builder.defaultEncoding, builder.defaultContentType, builder.useClassloader,
				builder.templatePath, builder.localizedLookup, builder.templateLoaders);
	}

	@Override
	public void load() {
		cfg = new Configuration(Configuration.VERSION_2_3_26);
		final MultiTemplateLoader.Builder builder = MultiTemplateLoader.of();
		if (useClassloader != null && useClassloader)
			builder.loader(new Loader(Loader.Type.resource, null).build());
		else
			builder.loader(new FileTemplateLoader(templatePath != null ?
					new File(templatePath) :
					libraryManager == null ? null : libraryManager.getDataDirectory()));
		if (templateLoaders != null)
			templateLoaders.forEach(loader -> builder.loader(loader.build()));
		cfg.setTemplateLoader(builder.build());
		cfg.setOutputEncoding(outputEncoding == null ? DEFAULT_CHARSET : outputEncoding);
		cfg.setDefaultEncoding(defaultEncoding == null ? DEFAULT_CHARSET : defaultEncoding);
		cfg.setLocalizedLookup(false);
		cfg.setTagSyntax(Configuration.AUTO_DETECT_TAG_SYNTAX);
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
	}

	@Override
	public void close() {
		if (cfg != null) {
			cfg.clearTemplateCache();
			cfg = null;
		}
	}

	public void template(String templatePath, Map<String, Object> dataModel, HttpServletResponse response)
			throws TemplateException, IOException {
		if (response.getContentType() == null)
			response.setContentType(defaultContentType == null ? DEFAULT_CONTENT_TYPE : defaultContentType);
		response.setCharacterEncoding(DEFAULT_CHARSET);
		Template template = cfg.getTemplate(templatePath);
		template.process(dataModel, response.getWriter());
	}

	public void template(String templatePath, HttpServletRequest request, HttpServletResponse response)
			throws IOException, TemplateException {
		Map<String, Object> variables = new HashMap<>();
		variables.put("request", request);
		variables.put("session", request.getSession());
		Enumeration<String> attrNames = request.getAttributeNames();
		if (attrNames != null) {
			while (attrNames.hasMoreElements()) {
				String attrName = attrNames.nextElement();
				variables.put(attrName, request.getAttribute(attrName));
			}
		}
		template(templatePath, variables, response);
	}

	public String template(final String templatePath, final Map<String, Object> dataModel)
			throws TemplateException, IOException {
		final Template template = cfg.getTemplate(templatePath);
		try (final StringWriter stringWriter = new StringWriter()) {
			template.process(dataModel, stringWriter);
			return stringWriter.toString();
		}
	}

	@JsonIgnore
	public static FreeMarkerToolBuilder of() {
		return of(null);
	}

	@JsonIgnore
	public static FreeMarkerToolBuilder of(LibraryManager libraryManager) {
		return new FreeMarkerToolBuilder(libraryManager);
	}
}
