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
package com.qwazr.library.jdbc;

import com.qwazr.library.annotations.Library;
import com.qwazr.library.test.AbstractLibraryTest;
import com.qwazr.utils.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class JdbcTest extends AbstractLibraryTest {

	@Test
	public void testPdf() throws Exception {
		doTest(PdfBox.class, "file.pdf", DEFAULT_TEST_STRING);
	}

	@Test
	public void testPwdPdf() throws Exception {
		doTest(PdfBox.class, "file-pass.pdf", DEFAULT_TEST_STRING, "password", "1234");
	}

}
