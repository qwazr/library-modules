/*
 * Copyright 2015-2020 Emmanuel Keller / QWAZR
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
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
import org.apache.poi.hslf.record.TextHeaderAtom;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hslf.usermodel.HSLFTextRun;

import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class PptParser extends ParserAbstract {

    private static final String[] DEFAULT_MIMETYPES = { "application/vnd.ms-powerpoint" };

    private static final String[] DEFAULT_EXTENSIONS = { "ppt" };

    final private static ParserField BODY = ParserField.newString("body", "The body of the document");

    final private static ParserField NOTES = ParserField.newString("notes", null);

    final private static ParserField OTHER = ParserField.newString("other", null);

    final private static ParserField[] FIELDS = { TITLE, CONTENT, BODY, NOTES, OTHER, LANG_DETECTION };

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

        try (final HSLFSlideShow ppt = new HSLFSlideShow(inputStream)) {

            final ParserFieldsBuilder metas = resultBuilder.metas();
            metas.set(MIME_TYPE, findMimeType(extension, mimeType, this::findMimeTypeUsingDefault));

            final List<HSLFSlide> slides = ppt.getSlides();
            for (HSLFSlide slide : slides) {
                final ParserFieldsBuilder document = resultBuilder.newDocument();
                final List<List<HSLFTextParagraph>> textLevel0 = slide.getTextParagraphs();
                for (List<HSLFTextParagraph> textLevel1 : textLevel0) {
                    for (HSLFTextParagraph textPara : textLevel1) {
                        final ParserField parserField;
                        switch (textPara.getRunType()) {
                        case TextHeaderAtom.TITLE_TYPE:
                        case TextHeaderAtom.CENTER_TITLE_TYPE:
                            parserField = TITLE;
                            break;
                        case TextHeaderAtom.NOTES_TYPE:
                            parserField = NOTES;
                            break;
                        case TextHeaderAtom.BODY_TYPE:
                        case TextHeaderAtom.CENTRE_BODY_TYPE:
                        case TextHeaderAtom.HALF_BODY_TYPE:
                        case TextHeaderAtom.QUARTER_BODY_TYPE:
                            parserField = BODY;
                            break;
                        case TextHeaderAtom.OTHER_TYPE:
                        default:
                            parserField = OTHER;
                            break;
                        }
                        StringBuilder sb = new StringBuilder();
                        for (HSLFTextRun textRun : textPara.getTextRuns()) {
                            sb.append(textRun.getRawText());
                            sb.append(' ');
                        }
                        final String text = sb.toString().trim();
                        document.add(parserField, text);
                        if (parserField != TITLE)
                            document.add(CONTENT, text);
                    }
                }
                document.add(LANG_DETECTION, languageDetection(document, CONTENT, 10000));
            }
        } catch (IOException e) {
            throw convertIOException(e::getMessage, e);
        }

    }
}
