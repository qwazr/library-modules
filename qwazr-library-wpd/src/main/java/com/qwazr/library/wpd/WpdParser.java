/*
 * Copyright 2015-2018 Emmanuel Keller / QWAZR
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

import com.qwazr.extractor.ParserAbstract;
import com.qwazr.extractor.ParserField;
import com.qwazr.extractor.ParserFieldsBuilder;
import com.qwazr.extractor.ParserResultBuilder;
import com.qwazr.library.html.HtmlParser;
import com.qwazr.utils.AutoCloseWrapper;
import com.qwazr.utils.HtmlUtils;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.LoggerUtils;
import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.ws.rs.core.MultivaluedMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class WpdParser extends ParserAbstract {

    private final static Logger LOGGER = LoggerUtils.getLogger(WpdParser.class);

    private final static String CMD_NAME = "wpd2html";

    private static final String[] DEFAULT_MIMETYPES =
            { "application/wordperfect", "application/wordperfect6.0", "application/wordperfect6.1" };

    private static final String[] DEFAULT_EXTENSIONS = { "wpd", "w60", "w61", "wp", "wp5", "wp6" };

    final private static ParserField[] FIELDS = { TITLE, CONTENT, LANG_DETECTION };

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
    public void parseContent(MultivaluedMap<String, String> parameters, Path path, String extension, String mimeType,
            ParserResultBuilder resultBuilder) {
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

            final ParserFieldsBuilder metas = resultBuilder.metas();
            metas.set(MIME_TYPE, DEFAULT_MIMETYPES[0]);

            final ParserFieldsBuilder parserDocument = resultBuilder.newDocument();

            final DOMParser htmlParser = HtmlParser.getThreadLocalDomParser();
            try (final BufferedReader reader = Files.newBufferedReader(htmlFile, StandardCharsets.UTF_8)) {
                htmlParser.parse(new InputSource(reader));
                HtmlUtils.domTextExtractor(htmlParser.getDocument(), text -> parserDocument.add(CONTENT, text));
                parserDocument.add(LANG_DETECTION, languageDetection(parserDocument, CONTENT, 10000));
            }
        } catch (IOException | InterruptedException | SAXException e) {
            throw convertException(() -> "Error with " + path.toAbsolutePath() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void parseContent(MultivaluedMap<String, String> parameters, InputStream inputStream, String extension,
            String mimeType, ParserResultBuilder resultBuilder) {
        try (final AutoCloseWrapper<Path> a = AutoCloseWrapper.of(Files.createTempFile("wpdparser", ".wpd"), LOGGER,
                Files::deleteIfExists)) {
            final Path tmpFile = a.get();
            IOUtils.copy(inputStream, tmpFile);
            parseContent(parameters, tmpFile, DEFAULT_EXTENSIONS[0], DEFAULT_MIMETYPES[0], resultBuilder);
        } catch (IOException e) {
            throw convertIOException(e::getMessage, e);
        }
    }

}
