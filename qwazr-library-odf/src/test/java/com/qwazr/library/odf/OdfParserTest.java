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
package com.qwazr.library.odf;

import com.qwazr.extractor.ExtractorManager;
import com.qwazr.extractor.ParserTest;
import javax.ws.rs.core.MediaType;
import org.junit.Test;

public class OdfParserTest extends ParserTest {

    static final String DEFAULT_TEST_STRING = "osstextextractor";

    public OdfParserTest() {
        super(new ExtractorManager());
        manager.registerServices();
    }

    @Test
    public void testOdt() throws Exception {
        doTest(OdfParser.class, "file.odt",
                MediaType.valueOf("application/vnd.oasis.opendocument.text"),
                "content", DEFAULT_TEST_STRING);
    }

    @Test
    public void testOds() throws Exception {
        doTest(OdfParser.class, "file.ods",
                MediaType.valueOf("application/vnd.oasis.opendocument.spreadsheet"),
                "content", DEFAULT_TEST_STRING);
    }

    @Test
    public void testOdp() throws Exception {
        doTest(OdfParser.class, "file.odp",
                MediaType.valueOf("application/vnd.oasis.opendocument.presentation"),
                "content", DEFAULT_TEST_STRING);
    }

}
