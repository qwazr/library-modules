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
package com.qwazr.library.markdown;

import com.qwazr.extractor.ExtractorManager;
import com.qwazr.extractor.ParserResult;
import com.qwazr.extractor.ParserTest;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class MarkdownParserTest extends ParserTest {

	public MarkdownParserTest() throws IOException, ClassNotFoundException {
		super(new ExtractorManager());
		manager.registerServices();
	}

	@Test
	public void testMarkdown() throws Exception {
		final ParserResult parserResult =
				doTest(MarkdownParser.class, "file.md", "text/markdown", "content", "extract data to be indexed");
		Assert.assertEquals("Discovering the main concepts", parserResult.getDocumentFieldValue(0, "h1", 0));
	}

}
