/*
 * Copyright 2015-2017 Emmanuel Keller
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

import com.qwazr.extractor.ExtractorManager;
import com.qwazr.extractor.ParserTest;
import org.junit.BeforeClass;
import org.junit.Test;

public class PoiParsersTest extends ParserTest {

	static final String DEFAULT_TEST_STRING = "osstextextractor";

	static ExtractorManager manager;

	@BeforeClass
	public static void init() {
		manager = new ExtractorManager();
		manager.registerServices();
	}

	public PoiParsersTest() {
		super(manager);
	}

	@Test
	public void testDoc() throws Exception {
		doTest(DocParser.class, "file.doc", "application/msword", "content", DEFAULT_TEST_STRING);
	}

	@Test
	public void testDocx() throws Exception {
		doTest(DocxParser.class, "file.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
				"content", DEFAULT_TEST_STRING);
	}

	@Test
	public void testDocx2() throws Exception {
		doTest(DocxParser.class, "file.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
				"content", DEFAULT_TEST_STRING);
	}

	@Test
	public void testPpt() throws Exception {
		doTest(PptParser.class, "file.ppt", "application/vnd.ms-powerpoint", "content", DEFAULT_TEST_STRING);
	}

	@Test
	public void testPptx() throws Exception {
		doTest(PptxParser.class, "file.pptx",
				"application/vnd.openxmlformats-officedocument.presentationml.presentation", "content",
				DEFAULT_TEST_STRING);
	}

	@Test
	public void testXls() throws Exception {
		doTest(XlsParser.class, "file.xls", "application/vnd.ms-excel", "content", DEFAULT_TEST_STRING);
	}

	@Test
	public void testXlsx() throws Exception {
		doTest(XlsxParser.class, "file.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
				"content", DEFAULT_TEST_STRING);
	}

}
