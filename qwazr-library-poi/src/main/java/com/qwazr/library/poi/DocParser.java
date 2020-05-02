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
import com.qwazr.extractor.ParserFieldsBuilder;
import com.qwazr.extractor.ParserResultBuilder;
import com.qwazr.utils.IOUtils;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hwpf.OldWordFileFormatException;
import org.apache.poi.hwpf.extractor.Word6Extractor;
import org.apache.poi.hwpf.extractor.WordExtractor;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;

public class DocParser extends ParserAbstract {

    private static final String[] DEFAULT_MIMETYPES = { "application/msword" };

    private static final String[] DEFAULT_EXTENSIONS = { "doc", "dot" };

    final private static ParserField AUTHOR = ParserField.newString("author", "The name of the author");

    final private static ParserField CREATION_DATE = ParserField.newDate("creation_date", null);

    final private static ParserField MODIFICATION_DATE = ParserField.newDate("modification_date", null);

    final private static ParserField SUBJECT = ParserField.newString("subject", "The subject of the document");

    final private static ParserField KEYWORDS = ParserField.newString("keywords", null);

    final private static ParserField[] FIELDS =
            { TITLE, AUTHOR, CREATION_DATE, MODIFICATION_DATE, SUBJECT, KEYWORDS, CONTENT, LANG_DETECTION };

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

    private void currentWordExtraction(final InputStream inputStream, final ParserResultBuilder resultBuilder)
            throws IOException {

        try (final WordExtractor word = new WordExtractor(inputStream)) {

            final SummaryInformation info = word.getSummaryInformation();
            if (info != null) {
                final ParserFieldsBuilder metas = resultBuilder.metas();
                metas.set(MIME_TYPE, DEFAULT_MIMETYPES[0]);
                metas.add(TITLE, info.getTitle());
                metas.add(AUTHOR, info.getAuthor());
                metas.add(SUBJECT, info.getSubject());
                metas.add(CREATION_DATE, info.getCreateDateTime());
                metas.add(MODIFICATION_DATE, info.getLastSaveDateTime());
                metas.add(KEYWORDS, info.getKeywords());
            }

            final ParserFieldsBuilder document = resultBuilder.newDocument();
            final String[] paragraphes = word.getParagraphText();
            if (paragraphes != null)
                for (String paragraph : paragraphes)
                    document.add(CONTENT, paragraph);
            document.add(LANG_DETECTION, languageDetection(document, CONTENT, 10000));
        }
    }

    private void oldWordExtraction(final InputStream inputStream, final ParserResultBuilder resultBuilder)
            throws IOException {
        Word6Extractor word6 = null;
        try {
            word6 = new Word6Extractor(inputStream);

            final ParserFieldsBuilder metas = resultBuilder.metas();
            metas.set(MIME_TYPE, DEFAULT_MIMETYPES[0]);

            SummaryInformation si = word6.getSummaryInformation();
            if (si != null) {
                metas.add(TITLE, si.getTitle());
                metas.add(AUTHOR, si.getAuthor());
                metas.add(SUBJECT, si.getSubject());
            }

            final ParserFieldsBuilder document = resultBuilder.newDocument();
            @SuppressWarnings("deprecation") String[] paragraphes = word6.getParagraphText();
            if (paragraphes != null)
                for (String paragraph : paragraphes)
                    document.add(CONTENT, paragraph);
            document.add(LANG_DETECTION, languageDetection(document, CONTENT, 10000));
        } finally {
            IOUtils.closeQuietly(word6);
        }
    }

    @Override
    public void parseContent(final MultivaluedMap<String, String> parameters, final InputStream inputStream,
            final String extension, final String mimeType, final ParserResultBuilder resultBuilder) {
        try {
            try {
                currentWordExtraction(inputStream, resultBuilder);
            } catch (OldWordFileFormatException e) {
                oldWordExtraction(inputStream, resultBuilder);
            }
        } catch (IOException e) {
            throw convertIOException(e::getMessage, e);
        }
    }

}
