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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class XlsParser implements ParserFactory, ParserInterface, PoiExtractor {

    private final static String NAME = "xls";

    private static final MediaType DEFAULT_MIMETYPE = MediaType.valueOf(
            "application/vnd.ms-excel");

    private static final Collection<MediaType> DEFAULT_MIMETYPES = List.of(DEFAULT_MIMETYPE);

    private static final Collection<String> DEFAULT_EXTENSIONS = List.of("xls");

    final private static Collection<ParserField> FIELDS = List.of(
            TITLE, AUTHOR, KEYWORDS, SUBJECT, CREATION_DATE, MODIFICATION_DATE, CONTENT, LANG_DETECTION);

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
        return DEFAULT_EXTENSIONS;
    }

    @Override
    public Collection<MediaType> getSupportedMimeTypes() {
        return DEFAULT_MIMETYPES;
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final InputStream inputStream,
                                final MediaType mimeType) throws IOException {

        final ParserResult.Builder resultBuilder = ParserResult.of(NAME);
        try (final HSSFWorkbook workbook = new HSSFWorkbook(inputStream)) {

            try (final ExcelExtractor excel = new ExcelExtractor(workbook)) {

                final ParserResult.FieldsBuilder metas = resultBuilder.metas();
                if (mimeType != null)
                    metas.set(MIME_TYPE, mimeType.toString());
                PoiExtractor.extractMetas(excel.getSummaryInformation(), metas);

                final ParserResult.FieldsBuilder result = resultBuilder.newDocument();
                result.add(CONTENT, excel.getText());
                result.add(LANG_DETECTION, ParserUtils.languageDetection(result, CONTENT, 10000));
            }
        }
        return resultBuilder.build();
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final Path filePath) throws IOException {
        return ParserUtils.toBufferedStream(filePath, in -> extract(parameters, in, DEFAULT_MIMETYPE));
    }
}
