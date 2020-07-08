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

import com.qwazr.extractor.ParserField;
import com.qwazr.extractor.ParserResult.FieldsBuilder;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.ooxml.POIXMLProperties;

import static com.qwazr.extractor.ParserInterface.TITLE;

public interface PoiExtractor {

    ParserField CREATOR = ParserField.newString("creator", "The name of the creator");

    ParserField AUTHOR = ParserField.newString("author", "The name of the author");

    ParserField CREATION_DATE = ParserField.newDate("creation_date", null);

    ParserField MODIFICATION_DATE = ParserField.newDate("modification_date", null);

    ParserField DESCRIPTION = ParserField.newString("description", null);

    ParserField KEYWORDS = ParserField.newString("keywords", null);

    ParserField SUBJECT = ParserField.newString("subject", "The subject of the document");

    static void extractMetas(final POIXMLProperties.CoreProperties info, final ParserResult.FieldsBuilder metas) {
        if (info == null)
            return;
        metas.add(TITLE, info.getTitle());
        metas.add(CREATOR, info.getCreator());
        metas.add(CREATION_DATE, info.getCreated());
        metas.add(MODIFICATION_DATE, info.getModified());
        metas.add(SUBJECT, info.getSubject());
        metas.add(DESCRIPTION, info.getDescription());
        metas.add(KEYWORDS, info.getKeywords());
    }

    static void extractMetas(final SummaryInformation info, final ParserResult.FieldsBuilder metas) {
        if (info == null)
            return;
        metas.add(TITLE, info.getTitle());
        metas.add(AUTHOR, info.getAuthor());
        metas.add(SUBJECT, info.getSubject());
        metas.add(CREATION_DATE, info.getCreateDateTime());
        metas.add(MODIFICATION_DATE, info.getLastSaveDateTime());
        metas.add(KEYWORDS, info.getKeywords());
    }

    /* TODO
    static void extract(final POIFSFileSystem fileSystem, final ParserResult.FieldsBuilder result)
            throws OpenXML4JException, XmlException, IOException {

        // Firstly, get an extractor for the Workbook
        try (final POIOLE2TextExtractor oleTextExtractor = ExtractorFactory.createExtractor(fileSystem)) {

            // Then a List of extractors for any embedded Excel, Word, PowerPoint
            // or Visio objects embedded into it.
            final POITextExtractor[] embeddedExtractors =
                    ExtractorFactory.getEmbeddedDocsTextExtractors(oleTextExtractor);

            for (final POITextExtractor textExtractor : embeddedExtractors) {

                if (textExtractor instanceof ExcelExtractor) {
                    XlsxParser.extract((ExcelExtractor) textExtractor, result);
                } else if (textExtractor instanceof WordExtractor) {
                    DocxParser.extract((WordExtractor) textExtractor, result);
                } else if (textExtractor instanceof PowerPointExtractor) {
                    extract((PowerPointExtractor) textExtractor, result);
                } else if (textExtractor instanceof VisioTextExtractor) {
                    extract((VisioTextExtractor) textExtractor, result);
                } else if (textExtractor instanceof SlideShowExtractor) {
                    extract((SlideShowExtractor<?, ?>) textExtractor, result);
                }
            }
        }
    }
     */

}
