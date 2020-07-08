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
package com.qwazr.library.pdfbox;

import com.qwazr.extractor.ParserFactory;
import com.qwazr.extractor.ParserField;
import com.qwazr.extractor.ParserInterface;
import com.qwazr.extractor.ParserResult;
import com.qwazr.extractor.ParserUtils;
import com.qwazr.utils.LoggerUtils;
import com.qwazr.utils.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

public class PdfBoxParser implements ParserFactory, ParserInterface {

    private final static String NAME = "pdfbox";

    private final static Logger LOGGER = LoggerUtils.getLogger(PdfBoxParser.class);

    private static final MediaType DEFAULT_MIMETYPE = MediaType.valueOf("application/pdf");

    private static final Collection<MediaType> DEFAULT_MIMETYPES = List.of(DEFAULT_MIMETYPE);

    private static final Collection<String> DEFAULT_EXTENSIONS = List.of("pdf");

    final private static ParserField AUTHOR = ParserField.newString("author", "The name of the author");

    final private static ParserField SUBJECT = ParserField.newString("subject", "The subject of the document");

    final private static ParserField PRODUCER = ParserField.newString("producer", "The producer of the document");

    final private static ParserField KEYWORDS = ParserField.newString("keywords", "The keywords of the document");

    final private static ParserField CREATION_DATE = ParserField.newDate("creation_date", null);

    final private static ParserField MODIFICATION_DATE = ParserField.newDate("modification_date", null);

    final private static ParserField LANGUAGE = ParserField.newString("language", null);

    final private static ParserField ROTATION = ParserField.newInteger("rotation", null);

    final private static ParserField NUMBER_OF_PAGES = ParserField.newInteger("number_of_pages", null);

    final private static ParserField CHARACTER_COUNT = ParserField.newInteger("character_count", null);

    final private static Collection<ParserField> FIELDS = List.of(
            TITLE,
            AUTHOR,
            SUBJECT,
            CONTENT,
            PRODUCER,
            KEYWORDS,
            CREATION_DATE,
            MODIFICATION_DATE,
            LANGUAGE,
            ROTATION,
            NUMBER_OF_PAGES,
            LANG_DETECTION
    );

    final private static ParserField PASSWORD = ParserField.newString("password", StringUtils.EMPTY);

    final private static Collection<ParserField> PARAMETERS = List.of(PASSWORD);

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public ParserInterface createParser() {
        return this;
    }

    private void extractMetaData(final PDDocument pdf, final ParserResult.FieldsBuilder metas) {
        metas.set(MIME_TYPE, DEFAULT_MIMETYPE.toString());
        final PDDocumentInformation info = pdf.getDocumentInformation();
        if (info != null) {
            metas.add(TITLE, info.getTitle());
            metas.add(SUBJECT, info.getSubject());
            metas.add(AUTHOR, info.getAuthor());
            metas.add(PRODUCER, info.getProducer());
            metas.add(KEYWORDS, info.getKeywords());
            metas.add(CREATION_DATE, info.getCreationDate());
            metas.add(MODIFICATION_DATE, info.getModificationDate());
        }
        int pages = pdf.getNumberOfPages();
        metas.add(NUMBER_OF_PAGES, pages);
        PDDocumentCatalog catalog = pdf.getDocumentCatalog();
        if (catalog != null)
            metas.add(LANGUAGE, catalog.getLanguage());
    }

    /**
     * Extract text content using PDFBox
     *
     * @param pdf
     * @param resultBuilder
     * @throws IOException
     */
    private void parseContent(final PDDocument pdf, final ParserResult.Builder resultBuilder) throws IOException {
        try {
            extractMetaData(pdf, resultBuilder.metas());
            final Stripper stripper = new Stripper(resultBuilder);
            stripper.getText(pdf);
        } finally {
            if (pdf != null) {
                try {
                    pdf.close();
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Cannot close the PDDocument", e);
                }
            }
        }
    }

    private String getPassword(final MultivaluedMap<String, String> parameters) {
        final String password = ParserUtils.getParameterValue(parameters, PASSWORD, 0);
        return password == null ? StringUtils.EMPTY : password;
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final InputStream inputStream,
                                final MediaType mediaType) throws IOException {
        try {
            final ParserResult.Builder resultBuilder = ParserResult.of(NAME);
            parseContent(PDDocument.load(inputStream, getPassword(parameters)), resultBuilder);
            return resultBuilder.build();
        } catch (InvalidPasswordException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final Path filePath) throws IOException {
        try {
            final ParserResult.Builder resultBuilder = ParserResult.of(NAME);
            parseContent(PDDocument.load(filePath.toFile(), getPassword(parameters)), resultBuilder);
            return resultBuilder.build();
        } catch (InvalidPasswordException e) {
            throw new InternalServerErrorException("Error with " + filePath.toAbsolutePath() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public Collection<ParserField> getParameters() {
        return PARAMETERS;
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

    private static class Stripper extends PDFTextStripper {

        private final ParserResult.Builder resultBuilder;

        private Stripper(ParserResult.Builder resultBuilder) throws IOException {
            this.resultBuilder = resultBuilder;
        }

        @Override
        protected void endPage(PDPage page) throws IOException {
            super.endPage(page);
            final ParserResult.FieldsBuilder document = resultBuilder.newDocument();
            final String text = output.toString();
            document.add(CHARACTER_COUNT, text.length());
            document.add(CONTENT, text);
            document.add(LANG_DETECTION, ParserUtils.languageDetection(document, CONTENT, 10000));
            document.add(ROTATION, page.getRotation());
            output = new StringWriter();
        }
    }
}
