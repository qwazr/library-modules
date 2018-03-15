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
package com.qwazr.library.markdown;

import com.qwazr.extractor.ParserAbstract;
import com.qwazr.extractor.ParserField;
import com.qwazr.extractor.ParserFieldsBuilder;
import com.qwazr.extractor.ParserResultBuilder;
import com.qwazr.utils.StringUtils;
import org.commonmark.node.Heading;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.text.TextContentRenderer;

import javax.ws.rs.core.MultivaluedMap;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MarkdownParser extends ParserAbstract {

	final static String[] DEFAULT_MIMETYPES = { "text/markdown" };

	final static String[] DEFAULT_EXTENSIONS = { "md", "markdown" };

	final static ParserField H1 = ParserField.newString("h1", "h1 headers");

	final static ParserField H2 = ParserField.newString("h2", "h2 headers");

	final static ParserField H3 = ParserField.newString("h3", "h3 headers");

	final static ParserField H4 = ParserField.newString("h4", "h4 headers");

	final static ParserField H5 = ParserField.newString("h5", "h5 headers");

	final static ParserField H6 = ParserField.newString("h6", "h6 headers");

	final static ParserField URL = ParserField.newString("url", "Detected URLs");

	final static ParserField URL_TITLE = ParserField.newString("title", "URL title");

	final protected static ParserField[] FIELDS = { H1, H2, H3, H4, H5, H6, CONTENT, URL, URL_TITLE, LANG_DETECTION };

	@Override
	public ParserField[] getFields() {
		return FIELDS;
	}

	@Override
	public String[] getDefaultExtensions() {
		return DEFAULT_EXTENSIONS;
	}

	@Override
	public String[] getDefaultMimeTypes() {
		return DEFAULT_MIMETYPES;
	}

	@Override
	final public void parseContent(final MultivaluedMap<String, String> parameters, final InputStream inputStream,
			final String extension, final String mimeType, final ParserResultBuilder resultBuilder) throws Exception {

		resultBuilder.metas().set(MIME_TYPE, DEFAULT_MIMETYPES[0]);
		final ParserFieldsBuilder result = resultBuilder.newDocument();
		final Parser parser = Parser.builder().build();

		final Node documentNode;

		try (final InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
			documentNode = parser.parseReader(reader);
		}

		// First pass we extract the meta data fields
		TextContentRenderer.builder()
				.nodeRendererFactory(context -> new ExtractorNodeRenderer(result))
				.build()
				.render(documentNode);

		// Second pass we extract the text content
		final String text = TextContentRenderer.builder().build().render(documentNode);
		if (text != null) {
			final String[] lines = StringUtils.splitLines(text);
			for (String line : lines)
				result.add(CONTENT, line);
		}

	}

	private final static Set<Class<? extends Node>> TYPES =
			new HashSet<>(Arrays.asList(Link.class, org.commonmark.node.Image.class, Text.class));

	final public class ExtractorNodeRenderer implements NodeRenderer {

		private final ParserFieldsBuilder result;

		private ExtractorNodeRenderer(final ParserFieldsBuilder result) {
			this.result = result;
		}

		@Override
		public Set<Class<? extends Node>> getNodeTypes() {
			return TYPES;
		}

		@Override
		public void render(Node node) {
			if (node instanceof Link) {
				final Link link = (Link) node;
				result.add(URL, link.getDestination());
				result.add(URL_TITLE, link.getTitle());
			} else if (node instanceof org.commonmark.node.Image) {
				final org.commonmark.node.Image img = (org.commonmark.node.Image) node;
				result.add(URL, img.getDestination());
				result.add(URL_TITLE, img.getTitle());
			} else if (node instanceof Text) {
				final Text text = (Text) node;
				final Node parent = node.getParent();
				if (parent != null && parent instanceof Heading) {
					final Heading heading = (Heading) parent;
					switch (heading.getLevel()) {
					case 1:
						result.add(H1, text.getLiteral());
						break;
					case 2:
						result.add(H2, text.getLiteral());
						break;
					case 3:
						result.add(H3, text.getLiteral());
						break;
					case 4:
						result.add(H4, text.getLiteral());
						break;
					case 5:
						result.add(H5, text.getLiteral());
						break;
					case 6:
						result.add(H6, text.getLiteral());
						break;
					}
				}
			}
		}
	}
}
