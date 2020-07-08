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
package com.qwazr.library.email;

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
import java.util.Properties;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.util.MimeMessageParser;

public class EmlParser implements ParserFactory, ParserInterface {

    private static final String NAME = "eml";

    private static final MediaType DEFAULT_MIMETYPE = MediaType.valueOf("message/rfc822");

    private static final Collection<MediaType> DEFAULT_MIMETYPES = List.of(DEFAULT_MIMETYPE);

    private static final Collection<String> DEFAULT_EXTENSIONS = List.of("eml");

    private final static ParserField SUBJECT = ParserField.newString("subject", "The subject of the email");

    private final static ParserField FROM = ParserField.newString("from", "The from email");

    private final static ParserField RECIPIENT_TO = ParserField.newString("recipient_to", "The recipient to");

    private final static ParserField RECIPIENT_CC = ParserField.newString("recipient_cc", "The recipient cc");

    private final static ParserField RECIPIENT_BCC = ParserField.newString("recipient_bcc", "The recipient bcc");

    private final static ParserField SENT_DATE = ParserField.newDate("sent_date", "The sent date");

    private final static ParserField RECEIVED_DATE = ParserField.newDate("received_date", "The received date");

    private final static ParserField ATTACHMENT_NAME = ParserField.newString("attachment_name", "The attachment name");

    private final static ParserField ATTACHMENT_TYPE =
            ParserField.newString("attachment_type", "The attachment mime type");

    private final static ParserField ATTACHMENT_CONTENT =
            ParserField.newString("attachment_content", "The attachment content");

    private final static ParserField PLAIN_CONTENT =
            ParserField.newString("plain_content", "The plain text body content");

    private final static ParserField HTML_CONTENT = ParserField.newString("html_content", "The html text body content");

    private final static List<ParserField> FIELDS = List.of(
            SUBJECT,
            FROM,
            RECIPIENT_TO,
            RECIPIENT_CC,
            RECIPIENT_BCC,
            SENT_DATE,
            RECEIVED_DATE,
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

    private final static Properties JAVAMAIL_PROPS = new Properties();

    static {
        JAVAMAIL_PROPS.put("mail.host", "localhost");
        JAVAMAIL_PROPS.put("mail.transport.protocol", "smtp");
    }

    @Override
    public ParserResult extract(final MultivaluedMap<String, String> parameters,
                                final InputStream inputStream,
                                final MediaType mimeType) throws IOException {
        final ParserResult.Builder resultBuilder = ParserResult.of(NAME);
        try {

            final Session session = Session.getDefaultInstance(JAVAMAIL_PROPS);

            if (mimeType != null)
                resultBuilder.metas().set(MIME_TYPE, mimeType.toString());

            final MimeMessage mimeMessage = new MimeMessage(session, inputStream);
            final MimeMessageParser mimeMessageParser = new MimeMessageParser(mimeMessage).parse();

            ParserResult.FieldsBuilder document = resultBuilder.newDocument();
            final String from = mimeMessageParser.getFrom();
            if (from != null)
                document.add(FROM, from);
            for (Address address : mimeMessageParser.getTo())
                document.add(RECIPIENT_TO, address.toString());
            for (Address address : mimeMessageParser.getCc())
                document.add(RECIPIENT_CC, address.toString());
            for (Address address : mimeMessageParser.getBcc())
                document.add(RECIPIENT_BCC, address.toString());
            document.add(SUBJECT, mimeMessageParser.getSubject());
            document.add(HTML_CONTENT, mimeMessageParser.getHtmlContent());
            document.add(PLAIN_CONTENT, mimeMessageParser.getPlainContent());
            document.add(SENT_DATE, mimeMessage.getSentDate());
            document.add(RECEIVED_DATE, mimeMessage.getReceivedDate());

            for (DataSource dataSource : mimeMessageParser.getAttachmentList()) {
                document.add(ATTACHMENT_NAME, dataSource.getName());
                document.add(ATTACHMENT_TYPE, dataSource.getContentType());
                // TODO Extract content from attachmend
                // if (parserSelector != null) {
                // Parser attachParser = parserSelector.parseStream(
                // getSourceDocument(), dataSource.getName(),
                // dataSource.getContentType(), null,
                // dataSource.getInputStream(), null, null, null);
                // if (attachParser != null) {
                // List<ParserResultItem> parserResults = attachParser
                // .getParserResults();
                // if (parserResults != null)
                // for (ParserResultItem parserResult : parserResults)
                // result.addField(
                // ParserFieldEnum.email_attachment_content,
                // parserResult);
                // }
                // }
            }
            if (StringUtils.isEmpty(mimeMessageParser.getHtmlContent()))
                document.add(LANG_DETECTION, ParserUtils.languageDetection(document, PLAIN_CONTENT, 10000));
            else
                document.add(LANG_DETECTION, ParserUtils.languageDetection(document, HTML_CONTENT, 10000));
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
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
