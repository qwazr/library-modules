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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qwazr.library.AbstractLibrary;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.LoggerUtils;
import com.qwazr.utils.StringUtils;
import com.qwazr.utils.SubstitutedVariables;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.rrd4j.core.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

public class Rrd4jTool extends AbstractLibrary {

	private static final Logger LOGGER = LoggerUtils.getLogger(Rrd4jTool.class);

	public final String path = null;
	public final Long startTime = null;
	public final Long step = null;
	public final Integer version = null;
	public final RrdArchive[] archives = null;
	public final RrdDataSource[] datasources = null;
	public final String backendFactory = null;

	@JsonIgnore
	private volatile String resolvedPath;

	@Override
	public void load() {
		resolvedPath = path == null ?
				libraryManager.getDataDirectory().getAbsolutePath() :
				SubstitutedVariables.propertyAndEnvironmentSubstitute(path);
	}

	protected RrdDef createDef() {
		final RrdDef rrdDef;
		if (step != null) {
			if (startTime != null) {
				if (version != null)
					rrdDef = new RrdDef(resolvedPath, startTime, step, version);
				else
					rrdDef = new RrdDef(resolvedPath, startTime, step);
			} else
				rrdDef = new RrdDef(resolvedPath, step);
		} else
			rrdDef = new RrdDef(resolvedPath);
		if (archives != null)
			for (RrdArchive archive : archives)
				rrdDef.addArchive(archive.getDef());
		if (datasources != null)
			for (RrdDataSource datasource : datasources)
				rrdDef.addDatasource(datasource.getDef());
		rrdDef.addDatasource();
		return rrdDef;
	}

	/**
	 * @param closeableContext
	 * @return a new RrdDb instance
	 * @throws IOException
	 * @see RrdDb
	 */
	@JsonIgnore
	public RrdDb getDb(final IOUtils.CloseableContext closeableContext, final boolean readOnly) throws IOException {
		Objects.requireNonNull(closeableContext, "Requires a closeable parameter");
		RrdDatabase rrdDatabase;
		try {
			rrdDatabase = new RrdDatabase(resolvedPath, backendFactory, readOnly);
		} catch (FileNotFoundException e) {
			LOGGER.info(() -> "RRD database not found. Create a new one: " + resolvedPath == null ?
					StringUtils.EMPTY :
					new File(resolvedPath).getAbsolutePath());
			rrdDatabase = new RrdDatabase(createDef(), backendFactory);
		}
		closeableContext.add(rrdDatabase);
		return rrdDatabase.rrdDb;
	}

	/**
	 * @param closeableContext
	 * @return a new RrdDb instance
	 * @throws IOException
	 */
	@JsonIgnore
	public RrdDb getDb(IOUtils.CloseableContext closeableContext) throws IOException {
		return getDb(closeableContext, false);
	}

	@JsonIgnore
	public RrdDb getDb(IOUtils.CloseableContext closeableContext, String rrdPath) throws IOException {
		return new RrdDatabase(rrdPath, backendFactory, true).rrdDb;
	}

	/**
	 * @return the path after resolving the environment variables or properties.
	 */
	@JsonIgnore
	public String getResolvedPath() {
		return resolvedPath;
	}

	@JsonIgnore
	public Sample createSample(RrdDb rrdDb, Long time) throws IOException {
		final Sample sample;
		if (time != null)
			sample = rrdDb.createSample(time);
		else
			sample = rrdDb.createSample();
		return sample;
	}

	/**
	 * Parses at-style time specification and returns the corresponding timestamp. For example:<p>
	 * <pre>
	 * long t = Util.getTimestamp("now-1d");
	 * </pre>
	 *
	 * @param atStyleTimeSpec at-style time specification. For the complete explanation of the syntax
	 *                        allowed see RRDTool's <code>rrdfetch</code> man page.<p>
	 * @return timestamp in seconds since epoch.
	 */
	public long getTimestamp(String atStyleTimeSpec) {
		return Util.getTimestamp(atStyleTimeSpec);
	}

}