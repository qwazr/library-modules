/**
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
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import javax.ws.rs.core.MultivaluedMap;
import java.io.InputStream;

public class XlsParser extends ParserAbstract {

	private static final String[] DEFAULT_MIMETYPES = { "application/vnd.ms-excel" };

	private static final String[] DEFAULT_EXTENSIONS = { "xls" };

	final private static ParserField TITLE = ParserField.newString("title", "The title of the document");

	final private static ParserField AUTHOR = ParserField.newString("author", "The name of the author");

	final private static ParserField KEYWORDS = ParserField.newString("keywords", null);

	final private static ParserField SUBJECT = ParserField.newString("subject", "The subject of the document");

	final private static ParserField CREATION_DATE = ParserField.newDate("creation_date", null);

	final private static ParserField MODIFICATION_DATE = ParserField.newDate("modification_date", null);

	final private static ParserField CONTENT = ParserField.newString("content", "The content of the document");

	final private static ParserField LANG_DETECTION =
			ParserField.newString("lang_detection", "Detection of the language");

	final private static ParserField[] FIELDS =
			{ TITLE, AUTHOR, KEYWORDS, SUBJECT, CREATION_DATE, MODIFICATION_DATE, CONTENT, LANG_DETECTION };

	@Override
	public ParserField[] getParameters() {
		return null;
	}

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
			final String extension, final String mimeType, final ParserResultBuilder resultBuilder) throws Exception {

		final HSSFWorkbook workbook = new HSSFWorkbook(inputStream);

		try (final ExcelExtractor excel = new ExcelExtractor(workbook)) {

			final SummaryInformation info = excel.getSummaryInformation();
			if (info != null) {
				final ParserFieldsBuilder metas = resultBuilder.metas();
				metas.add(TITLE, info.getTitle());
				metas.add(AUTHOR, info.getAuthor());
				metas.add(SUBJECT, info.getSubject());
				metas.add(CREATION_DATE, info.getCreateDateTime());
				metas.add(MODIFICATION_DATE, info.getLastSaveDateTime());
				metas.add(KEYWORDS, info.getKeywords());
			}

			final ParserFieldsBuilder result = resultBuilder.newDocument();
			result.add(CONTENT, excel.getText());
			result.add(LANG_DETECTION, languageDetection(result, CONTENT, 10000));
		}
	}
}
