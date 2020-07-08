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
package com.qwazr.library.wpd;

import com.qwazr.extractor.ParserFactory;
import com.qwazr.extractor.ParserField;
import com.qwazr.extractor.ParserInterface;
import com.qwazr.extractor.ParserResult;
import com.qwazr.extractor.ParserUtils;
import com.qwazr.library.html.HtmlParser;
import com.qwazr.utils.AutoCloseWrapper;
import com.qwazr.utils.HtmlUtils;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.LoggerUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class WpdParser implements ParserFactory, ParserInterface {

    private final static String NAME = "wdp";

    private final static Logger LOGGER = LoggerUtils.getLogger(WpdParser.class);

    private final static String CMD_NAME = "wpd2html";

    private static final Collection<MediaType> DEFAULT_MIMETYPES = List.of(
            MediaType.valueOf("application/wordperfect"),
            MediaType.valueOf("application/wordperfect6.0"),
            MediaType.valueOf("application/wordperfect6.1")
    );

    private static final Collection<String> DEFAULT_EXTENSIONS = List.of(
            "wpd", "w60", "w61", "wp", "wp5", "wp6");

    final private static Collection<ParserField> FIELDS = List.of(
            TITLE, CONTENT, LANG_DETECTION);

    @Override
    public Collection<ParserField> getFields() {
        return FIELDS;
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
    public Collection<String> getSupportedFileExtensions() {
        return DEFAULT_EXTENSIONS;
    }

    @Override
    public Collection<MediaType> getSupportedMimeTypes() {
        return DEFAULT_MIMETYPES;
    }

    public ParserResult parseContent(final Path path,
                                     final MediaType mimeType) throws IOException {
        final ParserResult.Builder resultBuilder = ParserResult.of(NAME);
        try (final AutoCloseWrapper<Path> a = AutoCloseWrapper.of(Files.createTempFile("wpdparser", ".html"), LOGGER,
                Files::deleteIfExists)) {
            final Path htmlFile = a.get();
            final ProcessBuilder builder = new ProcessBuilder().directory(path.getParent().toFile())
                    .command(CMD_NAME, path.getFileName().toString())
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(htmlFile.toFile());
            final Process process = builder.start();
            final int resultCode = process.waitFor();
            if (resultCode != 0)
                throw new IOException("The command " + CMD_NAME + " failed with error code: " + resultCode);

            final ParserResult.FieldsBuilder metas = resultBuilder.metas();
            if (mimeType != null)
                metas.set(MIME_TYPE, mimeType.toString());

            final ParserResult.FieldsBuilder parserDocument = resultBuilder.newDocument();

            final DOMParser htmlParser = HtmlParser.getThreadLocalDomParser();
            try (final BufferedReader reader = Files.newBufferedReader(htmlFile, StandardCharsets.UTF_8)) {
                htmlParser.parse(new InputSource(reader));
                HtmlUtils.domTextExtractor(htmlParser.getDocument(), text -> parserDocument.add(CONTENT, text));
                parserDocument.add(LANG_DETECTION, ParserUtils.languageDetection(parserDocument, CONTENT, 10000));
            }
        } catch (InterruptedException | SAXException e) {
            throw new InternalServerErrorException(e);
        }
        return resultBuilder.build();
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final InputStream inputStream,
                                final MediaType mediaType) throws IOException {
        try (final AutoCloseWrapper<Path> a = AutoCloseWrapper.of(
                Files.createTempFile("wpdparser", ".wpd"), LOGGER,
                Files::deleteIfExists)) {
            final Path tmpFile = a.get();
            IOUtils.copy(inputStream, tmpFile);
            return parseContent(tmpFile, mediaType);
        }
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final Path filePath) throws IOException {
        return parseContent(filePath, null);
    }

}
