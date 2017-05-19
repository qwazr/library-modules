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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qwazr.library.AbstractLibrary;
import com.qwazr.library.LibraryManager;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class FreeMarkerTool extends AbstractLibrary implements Closeable {

	public final String output_encoding;
	public final String default_encoding;
	public final String default_content_type;

	public final Boolean use_classloader;
	public final String template_path;

	@JsonIgnore
	private volatile Configuration cfg = null;

	private final static String DEFAULT_CHARSET = "UTF-8";
	private final static String DEFAULT_CONTENT_TYPE = "text/html";

	@JsonCreator
	FreeMarkerTool(@JsonProperty("output_encoding") String outputEncoding,
			@JsonProperty("default_encoding") String defaultEncoding,
			@JsonProperty("default_content_type") String defaultContentType,
			@JsonProperty("use_classloader") Boolean useClassloader,
			@JsonProperty("template_path") String templatePath) {
		output_encoding = outputEncoding;
		default_encoding = defaultEncoding;
		default_content_type = defaultContentType;
		use_classloader = useClassloader;
		template_path = templatePath;
	}

	FreeMarkerTool(FreeMarkerToolBuilder builder) {
		output_encoding = builder.outputEncoding;
		default_encoding = builder.defaultEncoding;
		default_content_type = builder.defaultContentType;
		use_classloader = builder.useClassloader;
		template_path = builder.templatePath;
	}

	@Override
	public void load() {
		cfg = new Configuration(Configuration.VERSION_2_3_23);
		cfg.setTemplateLoader((use_classloader != null && use_classloader) ?
				new ResourceTemplateLoader() :
				new FileTemplateLoader(template_path != null ?
						new File(template_path) :
						libraryManager == null ? null : libraryManager.getDataDirectory()));
		cfg.setOutputEncoding(output_encoding == null ? DEFAULT_CHARSET : output_encoding);
		cfg.setDefaultEncoding(default_encoding == null ? DEFAULT_CHARSET : default_encoding);
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
			response.setContentType(default_content_type == null ? DEFAULT_CONTENT_TYPE : default_content_type);
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
