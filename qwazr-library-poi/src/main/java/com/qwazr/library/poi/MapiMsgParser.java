/*
 * Copyright 2015-2017 Emmanuel Keller / QWAZR
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
import org.apache.poi.hsmf.MAPIMessage;

import javax.ws.rs.core.MultivaluedMap;
import java.io.InputStream;
import java.util.Properties;

public class MapiMsgParser extends ParserAbstract {

	private static final String[] DEFAULT_MIMETYPES = { "application/vnd.ms-outlook" };

	private static final String[] DEFAULT_EXTENSIONS = { "msg" };

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

	final private static ParserField[] FIELDS = { SUBJECT,
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
			LANG_DETECTION };

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

	private final static Properties JAVAMAIL_PROPS = new Properties();

	static {
		JAVAMAIL_PROPS.put("mail.host", "localhost");
		JAVAMAIL_PROPS.put("mail.transport.protocol", "smtp");
	}

	@Override
	public void parseContent(final MultivaluedMap<String, String> parameters, final InputStream inputStream,
			final String extension, final String mimeType, final ParserResultBuilder resultBuilder) throws Exception {

		final MAPIMessage msg = new MAPIMessage(inputStream);
		msg.setReturnNullOnMissingChunk(true);

		final ParserFieldsBuilder metas = resultBuilder.metas();
		metas.set(MIME_TYPE, DEFAULT_MIMETYPES[0]);

		final ParserFieldsBuilder document = resultBuilder.newDocument();

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
			document.add(LANG_DETECTION, languageDetection(document, PLAIN_CONTENT, 10000));
		else
			document.add(LANG_DETECTION, languageDetection(document, HTML_CONTENT, 10000));

		// TODO manage attachments
	}

}
