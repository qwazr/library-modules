/**
 * Copyright 2016-2017 Emmanuel Keller / QWAZR
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
package com.qwazr.library.csv;

import com.qwazr.library.annotations.Library;
import com.qwazr.library.test.AbstractLibraryTest;
import com.qwazr.utils.IOUtils;
import org.apache.commons.csv.CSVParser;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class CsvTest extends AbstractLibraryTest {

	@Library("csv")
	private CSVTool csv;

	private final static String TEST_CSV_PATH = "src/test/resources/com/qwazr/library/csv/test.csv";

	@Test
	public void csv() throws IOException, ParserConfigurationException, SAXException {
		Assert.assertNotNull(csv);

		try (final IOUtils.CloseableContext context = new IOUtils.CloseableList()) {
			try (final FileReader reader = new FileReader(new File(TEST_CSV_PATH))) {
				CSVParser parser = csv.getNewParser(reader, context);
				Assert.assertNotNull(parser);
			}
		}
	}
}
