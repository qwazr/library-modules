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

import com.qwazr.extractor.ParserAbstract;
import com.qwazr.extractor.ParserField;
import com.qwazr.extractor.ParserFieldsBuilder;
import com.qwazr.extractor.ParserResultBuilder;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;

public class DocxParser extends ParserAbstract implements PoiExtractor {

    private static final String[] DEFAULT_MIMETYPES = {
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.template"};

    private static final String[] DEFAULT_EXTENSIONS = {"docx", "dotx"};

    final private static ParserField[] FIELDS = {TITLE,
            CREATOR,
            CREATION_DATE,
            MODIFICATION_DATE,
            DESCRIPTION,
            KEYWORDS,
            SUBJECT,
            CONTENT,
            LANG_DETECTION};

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

    static void extract(final XWPFWordExtractor word, final ParserFieldsBuilder result) {
        result.add(CONTENT, word.getText());
    }

    @Override
    public void parseContent(final MultivaluedMap<String, String> parameters, final InputStream inputStream,
                             final String extension, final String mimeType, final ParserResultBuilder resultBuilder) {

        try (final XWPFDocument document = new XWPFDocument(inputStream)) {

            try (final XWPFWordExtractor word = new XWPFWordExtractor(document)) {

                final ParserFieldsBuilder metas = resultBuilder.metas();
                metas.set(MIME_TYPE, findMimeType(extension, mimeType, this::findMimeTypeUsingDefault));
                PoiExtractor.extractMetas(word.getCoreProperties(), metas);
                final ParserFieldsBuilder parserDocument = resultBuilder.newDocument();
                extract(word, parserDocument);
                parserDocument.add(LANG_DETECTION, languageDetection(parserDocument, CONTENT, 10000));
            }
        }
        catch (IOException e) {
            throw convertIOException(e::getMessage, e);
        }
    }
}
