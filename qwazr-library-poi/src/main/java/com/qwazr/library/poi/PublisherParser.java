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
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hpbf.extractor.PublisherTextExtractor;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;

public class PublisherParser extends ParserAbstract implements PoiExtractor {

    private static final String[] DEFAULT_MIMETYPES = {"application/x-mspublisher"};

    private static final String[] DEFAULT_EXTENSIONS = {"pub"};

    final private static ParserField[] FIELDS =
            {TITLE, AUTHOR, CREATION_DATE, MODIFICATION_DATE, KEYWORDS, SUBJECT, CONTENT, LANG_DETECTION};

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
    public void parseContent(final MultivaluedMap<String, String> parameters, final InputStream inputStream,
                             final String extension, final String mimeType, final ParserResultBuilder resultBuilder) {

        try (final PublisherTextExtractor extractor = new PublisherTextExtractor(inputStream)) {

            final ParserFieldsBuilder metas = resultBuilder.metas();
            metas.set(MIME_TYPE, findMimeType(extension, mimeType, this::findMimeTypeUsingDefault));
            PoiExtractor.extractMetas(extractor.getSummaryInformation(), metas);
            final String text = extractor.getText();
            if (!StringUtils.isEmpty(text)) {
                final ParserFieldsBuilder result = resultBuilder.newDocument();
                result.add(CONTENT, text);
                result.add(LANG_DETECTION, languageDetection(result, CONTENT, 10000));
            }
        }
        catch (IOException e) {
            throw convertIOException(e::getMessage, e);
        }
    }
}
