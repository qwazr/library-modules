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
package com.qwazr.library.wpd;

import com.qwazr.extractor.ExtractorManager;
import com.qwazr.extractor.ParserTest;
import com.qwazr.utils.LoggerUtils;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.SystemUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.logging.Logger;

public class WpdParserTest extends ParserTest {

    static final private Logger LOGGER = LoggerUtils.getLogger(WpdParserTest.class);

    static final String DEFAULT_TEST_STRING = "HANDRAILS AND RAILINGS";

    static ExtractorManager manager;

    @BeforeClass
    public static void init() {
        manager = new ExtractorManager();
        manager.registerServices();
    }

    public WpdParserTest() {
        super(manager);
    }

    @Test
    public void test() throws Exception {
        if (SystemUtils.IS_OS_WINDOWS) {
            LOGGER.warning("This module does not work on Windows OS. Deps on C++ library (libwpd)");
            return;
        }
        doTest(WpdParser.class, "file.wpd",
                MediaType.valueOf("application/wordperfect"),
                "content", DEFAULT_TEST_STRING);

    }
}
