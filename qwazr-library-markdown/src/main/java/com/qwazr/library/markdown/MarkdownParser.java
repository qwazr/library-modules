/*
 * Copyright 2015-2020 Emmanuel Keller / QWAZR
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

import com.qwazr.extractor.ParserFactory;
import com.qwazr.extractor.ParserField;
import com.qwazr.extractor.ParserInterface;
import com.qwazr.extractor.ParserResult;
import com.qwazr.extractor.ParserUtils;
import com.qwazr.utils.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.commonmark.node.Heading;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.text.TextContentRenderer;

public class MarkdownParser implements ParserInterface, ParserFactory {

    final private static String NAME = "markdown";

    final private static MediaType DEFAULT_MEDIATYPE = MediaType.valueOf("text/markdown");

    final static Collection<MediaType> DEFAULT_MIMETYPES = List.of(DEFAULT_MEDIATYPE);

    final static Collection<String> DEFAULT_EXTENSIONS = List.of("md", "markdown");

    final static ParserField H1 = ParserField.newString("h1", "h1 headers");

    final static ParserField H2 = ParserField.newString("h2", "h2 headers");

    final static ParserField H3 = ParserField.newString("h3", "h3 headers");

    final static ParserField H4 = ParserField.newString("h4", "h4 headers");

    final static ParserField H5 = ParserField.newString("h5", "h5 headers");

    final static ParserField H6 = ParserField.newString("h6", "h6 headers");

    final static ParserField URL = ParserField.newString("url", "Detected URLs");

    final static ParserField URL_TITLE = ParserField.newString("title", "URL title");

    final static Collection<ParserField> FIELDS = List.of(H1, H2, H3, H4, H5, H6, CONTENT, URL, URL_TITLE, LANG_DETECTION);

    @Override
    public Collection<ParserField> getFields() {
        return FIELDS;
    }

    @Override
    public Collection<String> getSupportedFileExtensions() {
        return DEFAULT_EXTENSIONS;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ParserInterface createParser() {
        return this;
    }

    @Override
    public Collection<MediaType> getSupportedMimeTypes() {
        return DEFAULT_MIMETYPES;
    }

    @Override
    final public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                      final InputStream inputStream,
                                      final MediaType mimeType) throws IOException {

        ParserResult.Builder builder = ParserResult.of(NAME);
        builder.metas().set(MIME_TYPE, DEFAULT_MEDIATYPE.toString());
        final ParserResult.FieldsBuilder result = builder.newDocument();
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
        return builder.build();
    }

    @Override
    public ParserResult extract(MultivaluedMap<String, String> parameters, Path filePath) throws IOException {
        return ParserUtils.toBufferedStream(filePath, in -> extract(parameters, in, DEFAULT_MEDIATYPE));
    }

    private final static Set<Class<? extends Node>> TYPES =
            new HashSet<>(Arrays.asList(Link.class, org.commonmark.node.Image.class, Text.class));

    final public static class ExtractorNodeRenderer implements NodeRenderer {

        private final ParserResult.FieldsBuilder result;

        private ExtractorNodeRenderer(final ParserResult.FieldsBuilder result) {
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
                if (parent instanceof Heading) {
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
