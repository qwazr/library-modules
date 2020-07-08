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
package com.qwazr.library.poi;

import com.qwazr.extractor.ParserFactory;
import com.qwazr.extractor.ParserField;
import com.qwazr.extractor.ParserInterface;
import com.qwazr.extractor.ParserResult;
import com.qwazr.extractor.ParserUtils;
import com.qwazr.utils.IOUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hwpf.OldWordFileFormatException;
import org.apache.poi.hwpf.extractor.Word6Extractor;
import org.apache.poi.hwpf.extractor.WordExtractor;

public class DocParser implements ParserFactory, ParserInterface, PoiExtractor {

    private static final String NAME = "doc";

    private static final MediaType DEFAULT_MIMETYPE = MediaType.valueOf("application/msword");

    private static final Collection<MediaType> DEFAULT_MIMETYPES = List.of(DEFAULT_MIMETYPE);

    private static final Collection<String> DEFAULT_EXTENSIONS = List.of("doc", "dot");

    final private static Collection<ParserField> FIELDS = List.of(
            TITLE, AUTHOR, CREATION_DATE, MODIFICATION_DATE, SUBJECT, KEYWORDS, CONTENT, LANG_DETECTION);

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

    private ParserResult currentWordExtraction(final InputStream inputStream)
            throws IOException {
        final ParserResult.Builder resultBuilder = ParserResult.of(NAME);

        try (final WordExtractor word = new WordExtractor(inputStream)) {

            final ParserResult.FieldsBuilder metas = resultBuilder.metas();
            metas.set(MIME_TYPE, DEFAULT_MIMETYPE.toString());
            PoiExtractor.extractMetas(word.getSummaryInformation(), metas);

            final ParserResult.FieldsBuilder document = resultBuilder.newDocument();
            final String[] paragraphes = word.getParagraphText();
            if (paragraphes != null)
                for (String paragraph : paragraphes)
                    document.add(CONTENT, paragraph);
            document.add(LANG_DETECTION, ParserUtils.languageDetection(document, CONTENT, 10000));
        }
        return resultBuilder.build();
    }

    private ParserResult oldWordExtraction(final InputStream inputStream)
            throws IOException {
        final ParserResult.Builder resultBuilder = ParserResult.of(NAME);
        Word6Extractor word6 = null;
        try {
            word6 = new Word6Extractor(inputStream);

            final ParserResult.FieldsBuilder metas = resultBuilder.metas();
            metas.set(MIME_TYPE, DEFAULT_MIMETYPE.toString());

            SummaryInformation si = word6.getSummaryInformation();
            if (si != null) {
                metas.add(TITLE, si.getTitle());
                metas.add(AUTHOR, si.getAuthor());
                metas.add(SUBJECT, si.getSubject());
            }

            final ParserResult.FieldsBuilder document = resultBuilder.newDocument();
            @SuppressWarnings("deprecation") String[] paragraphes = word6.getParagraphText();
            if (paragraphes != null)
                for (String paragraph : paragraphes)
                    document.add(CONTENT, paragraph);
            document.add(LANG_DETECTION, ParserUtils.languageDetection(document, CONTENT, 10000));
            return resultBuilder.build();
        } finally {
            IOUtils.closeQuietly(word6);
        }
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final InputStream inputStream,
                                final MediaType mimeType) throws IOException {
        try {
            return currentWordExtraction(inputStream);
        } catch (OldWordFileFormatException e) {
            return oldWordExtraction(inputStream);
        }
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final Path filePath) throws IOException {
        return ParserUtils.toBufferedStream(filePath, in -> extract(parameters, in, DEFAULT_MIMETYPE));
    }

}
