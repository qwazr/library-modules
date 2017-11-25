/*
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
 */
package com.qwazr.library.archiver;

import com.qwazr.library.annotations.Library;
import com.qwazr.library.test.AbstractLibraryTest;
import com.qwazr.utils.IOUtils;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.compressors.CompressorException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ArchiverTest extends AbstractLibraryTest {

	@Library("archiver")
	private ArchiverTool archiver;

	@Library("gzip_archiver")
	private ArchiverTool gzipArchiver;

	private final static String TEST_STRING = "TEST_COMPRESSION";

	@Test
	public void compressDecompress() throws CompressorException, IOException {
		Assert.assertNotNull(archiver);
		Path zipFile = Files.createTempFile("archiverToolTest", ".zip");
		archiver.compress(TEST_STRING, zipFile);
		Assert.assertEquals(TEST_STRING, archiver.decompressString(zipFile));
		Path clearFile = Files.createTempFile("archiverToolTest", ".txt");
		archiver.decompress(zipFile, clearFile);
		Assert.assertEquals(TEST_STRING, IOUtils.readPathAsString(clearFile, StandardCharsets.UTF_8));
	}

	@Test
	public void extractDir() throws IOException, ArchiveException, CompressorException {
		Path destDir = Files.createTempDirectory("archiverToolTest");

		Assert.assertNotNull(gzipArchiver);
		gzipArchiver.decompress_dir("src/test/resources/com/qwazr/library/archiver", "gz",
				destDir.toAbsolutePath().toString());
		Assert.assertTrue(Files.exists(destDir.resolve("test1.tar")));
		Assert.assertTrue(Files.exists(destDir.resolve("test2.tar")));

		Assert.assertNotNull(archiver);
		archiver.extract_dir(destDir.toAbsolutePath().toString(), "tar", destDir.toAbsolutePath().toString(), false);
		Assert.assertTrue(Files.exists(destDir.resolve("test1")));
		Assert.assertTrue(Files.exists(destDir.resolve("test2")));
	}
}
