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
package com.qwazr.library.odf;

import com.qwazr.extractor.ParserAbstract;
import com.qwazr.extractor.ParserField;
import com.qwazr.extractor.ParserResult.FieldsBuilder;
import com.qwazr.extractor.ParserResult.Builder;
import org.odftoolkit.odfdom.pkg.OdfElement;
import org.odftoolkit.simple.Document;
import org.odftoolkit.simple.common.TextExtractor;
import org.odftoolkit.simple.meta.Meta;

import javax.ws.rs.core.MultivaluedMap;
import java.io.InputStream;
import java.nio.file.Path;

public class OdfParser implements ParserFactory, ParserInterface {

    static final Collection<String> DEFAULT_MIMETYPES = { "application/vnd.oasis.opendocument.spreadsheet",
            "application/vnd.oasis.opendocument.spreadsheet-template",
            "application/vnd.oasis.opendocument.text",
            "application/vnd.oasis.opendocument.text-master",
            "application/vnd.oasis.opendocument.text-template",
            "application/vnd.oasis.opendocument.presentation",
            "application/vnd.oasis.opendocument.presentation-template" };

    static final Collection<String> DEFAULT_EXTENSIONS = { "ods", "ots", "odt", "odm", "ott", "odp", "otp" };

    final static ParserField CREATOR = ParserField.newString("creator", "The name of the creator");

    final static ParserField CREATION_DATE = ParserField.newDate("creation_date", "The date of creation");

    final static ParserField MODIFICATION_DATE =
            ParserField.newDate("modification_date", "The date of last modification");

    final static ParserField DESCRIPTION = ParserField.newString("description", null);

    final static ParserField KEYWORDS = ParserField.newString("keywords", null);

    final static ParserField SUBJECT = ParserField.newString("subject", "The subject of the document");

    final static ParserField LANGUAGE = ParserField.newString("language", null);

    final static ParserField PRODUCER = ParserField.newString("producer", "The producer of the document");

    final static ParserField[] FIELDS = { TITLE,
            CREATOR,
            CREATION_DATE,
            MODIFICATION_DATE,
            DESCRIPTION,
            KEYWORDS,
            SUBJECT,
            CONTENT,
            LANGUAGE,
            PRODUCER,
            LANG_DETECTION };

    @Override
    public Collection<ParserField> getFields() {
        return FIELDS;
    }

    @Override
    public Collection<String> getSupportedFileExtensions() {
        return DEFAULT_EXTENSIONS;
    }

    @Override
    public Collection<MediaType> getSupportedMimeTypes {
        return DEFAULT_MIMETYPES;
    }

    private void parseContent(final Document document, final ParserResult.Builder resultBuilder) {
        // Load file
        try {
            if (document == null)
                return;
            final Meta meta = document.getOfficeMetadata();
            if (meta != null) {
                final ParserResult.FieldsBuilder metas = resultBuilder.metas();
                metas.add(CREATION_DATE, meta.getCreationDate());
                metas.add(MODIFICATION_DATE, meta.getDcdate());
                metas.add(TITLE, meta.getTitle());
                metas.add(SUBJECT, meta.getSubject());
                metas.add(CREATOR, meta.getCreator());
                metas.add(PRODUCER, meta.getGenerator());
                metas.add(KEYWORDS, meta.getKeywords());
                metas.add(LANGUAGE, meta.getLanguage());
            }

            final OdfElement odfElement = document.getContentRoot();
            if (odfElement != null) {
                final ParserResult.FieldsBuilder result = resultBuilder.newDocument();
                String text = TextExtractor.newOdfTextExtractor(odfElement).getText();
                if (text != null) {
                    result.add(CONTENT, text);
                    result.add(LANG_DETECTION, languageDetection(result, CONTENT, 10000));
                }
            }
        } catch (Exception e) {
            throw convertException(e::getMessage, e);
        } finally {
            if (document != null)
                document.close();
        }
    }

    @Override
    public void parseContent(final MultivaluedMap<String, String> parameters, final InputStream inputStream,
            String extension, final String mimeType, final ParserResult.Builder resultBuilder) {
        resultBuilder.metas().set(MIME_TYPE, findMimeType(extension, mimeType, this::findMimeTypeUsingDefault));
        try {
            parseContent(Document.loadDocument(inputStream), resultBuilder);
        } catch (Exception e) {
            throw convertException(e::getMessage, e);
        }
    }

    @Override
    public void parseContent(final MultivaluedMap<String, String> parameters, final Path filePath, String extension,
            final String mimeType, final ParserResult.Builder resultBuilder) {
        resultBuilder.metas().set(MIME_TYPE, findMimeType(extension, mimeType, this::findMimeTypeUsingDefault));
        try {
            parseContent(Document.loadDocument(filePath.toFile()), resultBuilder);
        } catch (Exception e) {
            throw convertException(() -> "Error with " + filePath.toAbsolutePath() + ": " + e.getMessage(), e);
        }
    }
}
