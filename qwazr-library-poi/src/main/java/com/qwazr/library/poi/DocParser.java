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

import com.qwazr.extractor.ParserAbstract;
import com.qwazr.extractor.ParserField;
import com.qwazr.extractor.ParserResult.FieldsBuilder;
import com.qwazr.extractor.ParserResult.Builder;
import com.qwazr.utils.IOUtils;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hwpf.OldWordFileFormatException;
import org.apache.poi.hwpf.extractor.Word6Extractor;
import org.apache.poi.hwpf.extractor.WordExtractor;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;

public class DocParser implements ParserFactory, ParserInterface implements PoiExtractor {

    private static final Collection<String> DEFAULT_MIMETYPES = {"application/msword"};

    private static final Collection<String> DEFAULT_EXTENSIONS = {"doc", "dot"};

    final private static Collection<ParserField> FIELDS =
            {TITLE, AUTHOR, CREATION_DATE, MODIFICATION_DATE, SUBJECT, KEYWORDS, CONTENT, LANG_DETECTION};

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

    private void currentWordExtraction(final InputStream inputStream, final ParserResult.Builder resultBuilder)
            throws IOException {

        try (final WordExtractor word = new WordExtractor(inputStream)) {

            final ParserResult.FieldsBuilder metas = resultBuilder.metas();
            metas.set(MIME_TYPE, DEFAULT_MIMETYPES[0]);
            PoiExtractor.extractMetas(word.getSummaryInformation(), metas);

            final ParserResult.FieldsBuilder document = resultBuilder.newDocument();
            final String[] paragraphes = word.getParagraphText();
            if (paragraphes != null)
                for (String paragraph : paragraphes)
                    document.add(CONTENT, paragraph);
            document.add(LANG_DETECTION, languageDetection(document, CONTENT, 10000));
        }
    }

    private void oldWordExtraction(final InputStream inputStream, final ParserResult.Builder resultBuilder)
            throws IOException {
        Word6Extractor word6 = null;
        try {
            word6 = new Word6Extractor(inputStream);

            final ParserResult.FieldsBuilder metas = resultBuilder.metas();
            metas.set(MIME_TYPE, DEFAULT_MIMETYPES[0]);

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
            document.add(LANG_DETECTION, languageDetection(document, CONTENT, 10000));
        }
        finally {
            IOUtils.closeQuietly(word6);
        }
    }

    @Override
    public void parseContent(final MultivaluedMap<String, String> parameters, final InputStream inputStream,
                             final String extension, final String mimeType, final ParserResult.Builder resultBuilder) {
        try {
            try {
                currentWordExtraction(inputStream, resultBuilder);
            }
            catch (OldWordFileFormatException e) {
                oldWordExtraction(inputStream, resultBuilder);
            }
        }
        catch (IOException e) {
            throw convertIOException(e::getMessage, e);
        }
    }

}
