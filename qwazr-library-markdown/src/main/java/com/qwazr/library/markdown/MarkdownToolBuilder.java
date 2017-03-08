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
package com.qwazr.library.markdown;

import com.qwazr.classloader.ClassLoaderManager;
import com.qwazr.library.AbstractLibrary;
import org.commonmark.renderer.html.AttributeProvider;

import java.util.LinkedHashSet;

public class MarkdownToolBuilder extends AbstractLibrary {

	LinkedHashSet<MarkdownExtensionEnum> extensions;

	String attributeProviderClassName;

	Class<? extends AttributeProvider> attributeProviderClass;

	final ClassLoaderManager classLoaderManager;

	MarkdownToolBuilder(ClassLoaderManager classLoaderManager) {
		this.classLoaderManager = classLoaderManager;
	}

	public MarkdownToolBuilder extension(MarkdownExtensionEnum extension) {
		if (extensions == null)
			extensions = new LinkedHashSet<>();
		extensions.add(extension);
		return this;
	}

	public MarkdownToolBuilder attributeProviderClass(Class<? extends AttributeProvider> attributeProviderClass) {
		this.attributeProviderClass = attributeProviderClass;
		this.attributeProviderClassName = null;
		return this;
	}

	public MarkdownToolBuilder attributeProviderClass(String attributeProviderClassName) {
		this.attributeProviderClass = null;
		this.attributeProviderClassName = attributeProviderClassName;
		return this;
	}

	public MarkdownTool build() {
		return new MarkdownTool(this);
	}
}