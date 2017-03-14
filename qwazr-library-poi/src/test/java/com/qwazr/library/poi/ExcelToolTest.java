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
package com.qwazr.library.poi;

import com.qwazr.library.annotations.Library;
import com.qwazr.library.test.AbstractLibraryTest;
import com.qwazr.utils.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;

public class ExcelToolTest extends AbstractLibraryTest {

	@Library("excel")
	private com.qwazr.library.poi.ExcelTool excel;

	@Test
	public void test() throws IOException, SQLException {
		Assert.assertNotNull(excel);
		try (final IOUtils.CloseableContext context = new IOUtils.CloseableList()) {
			final ExcelBuilder excelBuilder = excel.getNewBuilder(true, context);
			Assert.assertNotNull(excelBuilder);
		}
	}

}
