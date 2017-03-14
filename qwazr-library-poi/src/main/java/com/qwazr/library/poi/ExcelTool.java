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

import com.qwazr.library.AbstractLibrary;
import com.qwazr.utils.IOUtils;

public class ExcelTool extends AbstractLibrary {

	public final String default_date_format = null;

	public final String default_number_format = null;

	/**
	 * Create a new Excel document builder
	 *
	 * @param xlsx       true to create a XLSX document, false to create a legacy XLS document
	 * @param closeables an optional autocloseable context
	 * @return a new builder
	 */
	public ExcelBuilder getNewBuilder(final boolean xlsx, final IOUtils.CloseableContext closeables) {
		final ExcelBuilder builder = new ExcelBuilder(xlsx);
		if (closeables != null)
			closeables.add(builder);
		if (default_date_format != null)
			builder.setDefaultDateFormat(default_date_format);
		if (default_number_format != null)
			builder.setDefaultNumberFormat(default_number_format);
		return builder;
	}

}
