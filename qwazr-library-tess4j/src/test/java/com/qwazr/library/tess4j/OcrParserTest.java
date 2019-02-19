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
package com.qwazr.library.tess4j;

import com.qwazr.extractor.ExtractorManager;
import com.qwazr.extractor.ParserTest;
import net.sourceforge.tess4j.Tesseract1;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Paths;

public class OcrParserTest extends ParserTest {

    static final String DEFAULT_TEST_STRING = "osstextextractor";

    static {
        System.setProperty("TESSDATA_PREFIX", Paths.get("src", "test").toString());
    }

    @BeforeClass
    public static void setup() {
        Assume.assumeTrue(checkTesseract());
    }

    private static boolean checkTesseract() {
        try {
            new Tesseract1();
            return true;
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
            return false;
        }
    }

    public OcrParserTest() {
        super(new ExtractorManager());
        manager.registerServices();
    }

    @Test
    public void testOcr() throws Exception {
        doTest(OcrParser.class, "file.pdf", "application/pdf", "content", DEFAULT_TEST_STRING);
    }

}
