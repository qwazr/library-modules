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
package com.qwazr.library.rrd4j;

import com.qwazr.utils.LoggerUtils;
import org.rrd4j.core.RrdBackendFactory;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

class RrdDatabase implements Closeable {

	private static final Logger LOGGER = LoggerUtils.getLogger(RrdDatabase.class);

	final RrdDb rrdDb;

	RrdDatabase(String path, String backendFactory, boolean readOnly) throws IOException {
		if (backendFactory != null) {
			rrdDb = new RrdDb(path, readOnly, RrdBackendFactory.getFactory(backendFactory));
		} else {
			rrdDb = new RrdDb(path, readOnly);
		}
	}

	RrdDatabase(RrdDef def, String backendFactory) throws IOException {
		if (backendFactory != null)
			rrdDb = new RrdDb(def, RrdBackendFactory.getFactory(backendFactory));
		else
			rrdDb = new RrdDb(def);
	}

	@Override
	public void close() throws IOException {
		if (rrdDb == null || rrdDb.isClosed())
			return;
		try {
			rrdDb.close();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, e, e::getMessage);
		}
	}

}