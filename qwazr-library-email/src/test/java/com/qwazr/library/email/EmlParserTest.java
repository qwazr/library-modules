/**
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
package com.qwazr.library.email;

import com.qwazr.extractor.ExtractorManager;
import com.qwazr.extractor.ParserTest;
import org.junit.Test;

public class EmlParserTest extends ParserTest {

	public EmlParserTest() {
		super(new ExtractorManager(null));
		manager.register(EmlParser.class);
	}

	@Test
	public void testEml() throws Exception {
		doTest(EmlParser.class, "file.eml", "Maximum actions in one visit");
	}

}
