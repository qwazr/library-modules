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
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hsmf.MAPIMessage;
import org.apache.poi.hsmf.exceptions.ChunkNotFoundException;

public class MapiMsgParser implements ParserFactory, ParserInterface {

    private final static String NAME = "mapi";

    private static final MediaType DEFAULT_MIMETYPE = MediaType.valueOf("application/vnd.ms-outlook");

    private static final Collection<MediaType> DEFAULT_MIMETYPES = List.of(DEFAULT_MIMETYPE);

    private static final Collection<String> DEFAULT_EXTENSIONS = List.of("msg");

    final private static ParserField SUBJECT = ParserField.newString("subject", "The subject of the email");

    final private static ParserField FROM = ParserField.newString("from", "The from email");

    final private static ParserField RECIPIENT_TO = ParserField.newString("recipient_to", "The recipient to");

    final private static ParserField RECIPIENT_CC = ParserField.newString("recipient_cc", "The recipient cc");

    final private static ParserField RECIPIENT_BCC = ParserField.newString("recipient_bcc", "The recipient bcc");

    final private static ParserField MESSAGE_DATE = ParserField.newDate("message_date", "The message date");

    final private static ParserField CONVERSATION_TOPIC =
            ParserField.newString("conversation_topic", "The conversation topic");

    final private static ParserField ATTACHMENT_NAME = ParserField.newString("attachment_name", "The attachment name");

    final private static ParserField ATTACHMENT_TYPE =
            ParserField.newString("attachment_type", "The attachment mime type");

    final private static ParserField ATTACHMENT_CONTENT =
            ParserField.newString("attachment_content", "The attachment content");

    final private static ParserField PLAIN_CONTENT =
            ParserField.newString("plain_content", "The plain text body content");

    final private static ParserField HTML_CONTENT = ParserField.newString("html_content", "The html text body content");

    final private static Collection<ParserField> FIELDS = List.of(
            SUBJECT,
            FROM,
            RECIPIENT_TO,
            RECIPIENT_CC,
            RECIPIENT_BCC,
            MESSAGE_DATE,
            CONVERSATION_TOPIC,
            ATTACHMENT_NAME,
            ATTACHMENT_TYPE,
            ATTACHMENT_CONTENT,
            PLAIN_CONTENT,
            HTML_CONTENT,
            LANG_DETECTION
    );

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

        try (final MAPIMessage msg = new MAPIMessage(inputStream)) {
            msg.setReturnNullOnMissingChunk(true);

            final ParserResult.FieldsBuilder metas = resultBuilder.metas();
            if (mimeType != null)
                metas.set(MIME_TYPE, mimeType.toString());

            final ParserResult.FieldsBuilder document = resultBuilder.newDocument();

            document.add(FROM, msg.getDisplayFrom());
            document.add(RECIPIENT_TO, msg.getDisplayTo());
            document.add(RECIPIENT_CC, msg.getDisplayCC());
            document.add(RECIPIENT_BCC, msg.getDisplayBCC());
            document.add(SUBJECT, msg.getSubject());
            document.add(HTML_CONTENT, msg.getHtmlBody());
            document.add(PLAIN_CONTENT, msg.getTextBody());
            document.add(MESSAGE_DATE, msg.getMessageDate());
            document.add(CONVERSATION_TOPIC, msg.getConversationTopic());

            if (StringUtils.isEmpty(msg.getHtmlBody()))
                document.add(LANG_DETECTION, ParserUtils.languageDetection(document, PLAIN_CONTENT, 10000));
            else
                document.add(LANG_DETECTION, ParserUtils.languageDetection(document, HTML_CONTENT, 10000));

            // TODO manage attachments
        } catch (ChunkNotFoundException e) {
            throw new InternalServerErrorException(e);
        }
        return resultBuilder.build();
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final Path filePath) throws IOException {
        return ParserUtils.toBufferedStream(filePath, in -> extract(parameters, in, DEFAULT_MIMETYPE));
    }

}
