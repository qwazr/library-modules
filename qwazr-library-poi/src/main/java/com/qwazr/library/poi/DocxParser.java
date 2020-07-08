/*
 * Copyright 2014-2020 Emmanuel Keller / QWAZR
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
package com.qwazr.library.poi;

import com.qwazr.extractor.ParserFactory;
import com.qwazr.extractor.ParserField;
import com.qwazr.extractor.ParserInterface;
import com.qwazr.extractor.ParserResult;
import com.qwazr.extractor.ParserUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public class DocxParser implements ParserFactory, ParserInterface, PoiExtractor {

    private final static String NAME = "docx";

    private static final Map<String, MediaType> TYPEMAP = Map.of(
            "docx", MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            "dotx", MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.template")
    );

    final private static Collection<ParserField> FIELDS = List.of(
            TITLE,
            CREATOR,
            CREATION_DATE,
            MODIFICATION_DATE,
            DESCRIPTION,
            KEYWORDS,
            SUBJECT,
            CONTENT,
            LANG_DETECTION);

    @Override
    public Collection<ParserField> getFields() {
        return FIELDS;
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
    public Collection<String> getSupportedFileExtensions() {
        return TYPEMAP.keySet();
    }

    @Override
    public Collection<MediaType> getSupportedMimeTypes() {
        return TYPEMAP.values();
    }

    static void extract(final XWPFWordExtractor word, final ParserResult.FieldsBuilder result) {
        result.add(CONTENT, word.getText());
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final InputStream inputStream,
                                final MediaType mimeType) throws IOException {

        final ParserResult.Builder resultBuilder = ParserResult.of(NAME);

        try (final XWPFDocument document = new XWPFDocument(inputStream)) {

            try (final XWPFWordExtractor word = new XWPFWordExtractor(document)) {

                final ParserResult.FieldsBuilder metas = resultBuilder.metas();
                if (mimeType != null)
                    metas.set(MIME_TYPE, mimeType.toString());
                PoiExtractor.extractMetas(word.getCoreProperties(), metas);
                final ParserResult.FieldsBuilder parserDocument = resultBuilder.newDocument();
                extract(word, parserDocument);
                parserDocument.add(LANG_DETECTION, ParserUtils.languageDetection(parserDocument, CONTENT, 10000));
            }
        }
        return resultBuilder.build();
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final Path filePath) throws IOException {
        final MediaType mediaType = TYPEMAP.get(ParserUtils.getExtension(filePath));
        return ParserUtils.toBufferedStream(filePath, in -> extract(parameters, in, mediaType));
    }
}
