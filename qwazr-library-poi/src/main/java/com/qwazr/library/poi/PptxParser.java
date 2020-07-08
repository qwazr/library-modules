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
import com.qwazr.utils.LoggerUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ooxml.extractor.POIXMLTextExtractor;
import org.apache.poi.sl.extractor.SlideShowExtractor;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

public class PptxParser implements ParserFactory, ParserInterface implements PoiExtractor {

    private static final Logger LOGGER = LoggerUtils.getLogger(PptxParser.class);

    private static final Collection<String> DEFAULT_MIMETYPES =
            {"application/vnd.openxmlformats-officedocument.presentationml.presentation"};

    private static final Collection<String> DEFAULT_EXTENSIONS = {"pptx"};

    final private static Collection<ParserField> FIELDS = {TITLE,
            CONTENT,
            CREATOR,
            DESCRIPTION,
            KEYWORDS,
            SUBJECT,
            CREATION_DATE,
            MODIFICATION_DATE,
            LANG_DETECTION};

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

    static void extract(final SlideShow<?, ?> slideShow, final ParserResult.FieldsBuilder result) throws IOException {
        try (final SlideShowExtractor<?, ?> extractor = new SlideShowExtractor<>(slideShow)) {
            extractor.setCommentsByDefault(true);
            extractor.setNotesByDefault(true);
            extractor.setSlidesByDefault(true);
            final String text = extractor.getText();
            if (!StringUtils.isEmpty(text))
                result.add(CONTENT, text);
        }
    }

    @Override
    public void parseContent(final MultivaluedMap<String, String> parameters, final InputStream inputStream,
                             final String extension, final String mimeType, final ParserResult.Builder resultBuilder) {

        try (final XMLSlideShow slideshow = new XMLSlideShow(inputStream)) {

            try (final POIXMLTextExtractor textExtractor = slideshow.getMetadataTextExtractor()) {
                final ParserResult.FieldsBuilder metas = resultBuilder.metas();
                metas.set(MIME_TYPE, findMimeType(extension, mimeType, this::findMimeTypeUsingDefault));
                PoiExtractor.extractMetas(textExtractor.getCoreProperties(), metas);
            }

            final ParserResult.FieldsBuilder result = resultBuilder.newDocument();
            extract(slideshow, result);
            result.add(LANG_DETECTION, languageDetection(result, CONTENT, 10000));

        }
        catch (IOException e) {
            throw convertIOException(e::getMessage, e);
        }

    }

}
