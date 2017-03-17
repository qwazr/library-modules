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
 **/
package com.qwazr.library.tess4j;

import com.qwazr.extractor.ExtractorManager;
import com.qwazr.extractor.ParserTest;
import org.junit.Assume;
import org.junit.Test;

import java.io.IOException;

public class OcrParserTest extends ParserTest {

	static final String DEFAULT_TEST_STRING = "osstextextractor";

	public OcrParserTest() throws IOException, ClassNotFoundException {
		super(new ExtractorManager(null));
		manager.registerByJsonResources();
	}

	@Test
	public void testOcr() throws Exception {
		try {
			doTest(OcrParser.class, "file.pdf", DEFAULT_TEST_STRING);
		} catch (UnsatisfiedLinkError e) {
			Assume.assumeNoException("OCR skipped: no TESSDATA_PREFIX", e);
		}
	}

}
