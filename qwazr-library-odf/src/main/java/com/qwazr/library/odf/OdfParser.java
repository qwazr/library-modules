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
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.odftoolkit.odfdom.pkg.OdfElement;
import org.odftoolkit.simple.Document;
import org.odftoolkit.simple.common.TextExtractor;
import org.odftoolkit.simple.meta.Meta;

public class OdfParser implements ParserFactory, ParserInterface {

    private final static String NAME = "odf";

    static final Map<String, MediaType> TYPES_MAP = Map.of(
            "ods", MediaType.valueOf("application/vnd.oasis.opendocument.spreadsheet"),
            "ots", MediaType.valueOf("application/vnd.oasis.opendocument.spreadsheet-template"),
            "odt", MediaType.valueOf("application/vnd.oasis.opendocument.text"),
            "odm", MediaType.valueOf("application/vnd.oasis.opendocument.text-master"),
            "ott", MediaType.valueOf("application/vnd.oasis.opendocument.text-template"),
            "odp", MediaType.valueOf("application/vnd.oasis.opendocument.presentation"),
            "otp", MediaType.valueOf("application/vnd.oasis.opendocument.presentation-template")
    );

    final static ParserField CREATOR = ParserField.newString("creator", "The name of the creator");

    final static ParserField CREATION_DATE = ParserField.newDate("creation_date", "The date of creation");

    final static ParserField MODIFICATION_DATE =
            ParserField.newDate("modification_date", "The date of last modification");

    final static ParserField DESCRIPTION = ParserField.newString("description", null);

    final static ParserField KEYWORDS = ParserField.newString("keywords", null);

    final static ParserField SUBJECT = ParserField.newString("subject", "The subject of the document");

    final static ParserField LANGUAGE = ParserField.newString("language", null);

    final static ParserField PRODUCER = ParserField.newString("producer", "The producer of the document");

    final static List<ParserField> FIELDS = List.of(
            TITLE,
            CREATOR,
            CREATION_DATE,
            MODIFICATION_DATE,
            DESCRIPTION,
            KEYWORDS,
            SUBJECT,
            CONTENT,
            LANGUAGE,
            PRODUCER,
            LANG_DETECTION
    );

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ParserInterface createParser() {
        return this;
    }

    @Override
    public Collection<ParserField> getFields() {
        return FIELDS;
    }

    @Override
    public Collection<String> getSupportedFileExtensions() {
        return TYPES_MAP.keySet();
    }

    @Override
    public Collection<MediaType> getSupportedMimeTypes() {
        return TYPES_MAP.values();
    }

    private ParserResult parseContent(final Document document,
                                      final MediaType mediaType) throws Exception {
        final ParserResult.Builder resultBuilder = ParserResult.of(NAME);
        if (mediaType != null)
            resultBuilder.metas().set(MIME_TYPE, mediaType.toString());
        // Load file
        try {
            if (document == null)
                return resultBuilder.build();
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
                    result.add(LANG_DETECTION, ParserUtils.languageDetection(result, CONTENT, 10000));
                }
            }
        } finally {
            if (document != null)
                document.close();
        }
        return resultBuilder.build();
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final InputStream inputStream,
                                final MediaType mimeType) throws IOException {
        try {
            return parseContent(Document.loadDocument(inputStream), mimeType);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerErrorException(e);
        }
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final Path filePath) throws IOException {
        final MediaType mediaType = TYPES_MAP.get(ParserUtils.getExtension(filePath));
        return ParserUtils.toBufferedStream(filePath, in -> extract(parameters, in, mediaType));
    }
}
