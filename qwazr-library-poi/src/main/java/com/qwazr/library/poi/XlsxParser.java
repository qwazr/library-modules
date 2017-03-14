/**
 * Copyright 2014-2016 Emmanuel Keller / QWAZR
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
import org.apache.poi.POIXMLProperties.CoreProperties;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.ws.rs.core.MultivaluedMap;
import java.io.InputStream;

public class XlsxParser extends ParserAbstract {

	private static final String[] DEFAULT_MIMETYPES =
			{ "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" };

	private static final String[] DEFAULT_EXTENSIONS = { "xlsx" };

	final private static ParserField TITLE = ParserField.newString("title", "The title of the document");

	final private static ParserField CREATOR = ParserField.newString("creator", "The name of the creator");

	final private static ParserField CREATION_DATE = ParserField.newDate("creation_date", null);

	final private static ParserField MODIFICATION_DATE = ParserField.newDate("modification_date", null);

	final private static ParserField DESCRIPTION = ParserField.newString("description", null);

	final private static ParserField KEYWORDS = ParserField.newString("keywords", null);

	final private static ParserField SUBJECT = ParserField.newString("subject", "The subject of the document");

	final private static ParserField CONTENT = ParserField.newString("content", "The content of the document");

	final private static ParserField LANG_DETECTION =
			ParserField.newString("lang_detection", "Detection of the language");

	final private static ParserField[] FIELDS = { TITLE,
			CREATOR,
			CREATION_DATE,
			MODIFICATION_DATE,
			DESCRIPTION,
			KEYWORDS,
			SUBJECT,
			CONTENT,
			LANG_DETECTION };

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
			String extension, final String mimeType, final ParserResultBuilder resultBuilder) throws Exception {

		try (final XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {

			try (final XSSFExcelExtractor excelExtractor = new XSSFExcelExtractor(workbook)) {

				final CoreProperties info = excelExtractor.getCoreProperties();
				if (info != null) {
					final ParserFieldsBuilder metas = resultBuilder.metas();
					metas.add(TITLE, info.getTitle());
					metas.add(CREATOR, info.getCreator());
					metas.add(CREATION_DATE, info.getCreated());
					metas.add(MODIFICATION_DATE, info.getModified());
					metas.add(SUBJECT, info.getSubject());
					metas.add(DESCRIPTION, info.getDescription());
					metas.add(KEYWORDS, info.getKeywords());
				}

				final ParserFieldsBuilder result = resultBuilder.newDocument();
				excelExtractor.setIncludeCellComments(true);
				excelExtractor.setIncludeHeadersFooters(true);
				excelExtractor.setIncludeSheetNames(true);
				result.add(CONTENT, excelExtractor.getText());
				result.add(LANG_DETECTION, languageDetection(result, CONTENT, 10000));

			}
		}

	}

}
