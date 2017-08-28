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
package com.qwazr.library.archiver;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.qwazr.library.AbstractLibrary;
import com.qwazr.utils.CharsetUtils;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.LoggerUtils;
import com.qwazr.utils.ObjectMappers;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.entity.ContentType;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ArchiverTool extends AbstractLibrary {

	private static final Logger LOGGER = LoggerUtils.getLogger(ArchiverTool.class);

	private final CompressorStreamFactory factory;

	public enum CodecType {

		deflate(CompressorStreamFactory.DEFLATE),

		gzip(CompressorStreamFactory.GZIP),

		bzip2(CompressorStreamFactory.BZIP2),

		z(CompressorStreamFactory.Z);

		private final String codecName;

		CodecType(String codecName) {
			this.codecName = codecName;
		}
	}

	public final CodecType codec = null;

	public ArchiverTool() {
		factory = new CompressorStreamFactory();
	}

	private InputStream getCompressorNewInputStream(final InputStream input) throws IOException, CompressorException {
		if (codec == null)
			return factory.createCompressorInputStream(input);
		else
			return factory.createCompressorInputStream(codec.codecName, input);
	}

	/**
	 * Return a reader for the given file
	 *
	 * @param source  the source file
	 * @param context an optional autoclosing context
	 * @return a new reader
	 * @throws IOException
	 * @throws CompressorException
	 */
	@JsonIgnore
	public InputStreamReader getCompressorReader(final File source, final IOUtils.CloseableContext context)
			throws IOException, CompressorException {
		InputStream input = getCompressorNewInputStream(new BufferedInputStream(new FileInputStream(source)));
		InputStreamReader reader = new InputStreamReader(input);
		if (context != null)
			context.add(reader);
		return reader;
	}

	public void decompress(final File source, final File destFile) throws IOException, CompressorException {
		if (destFile.exists() && destFile.length() > 0)
			throw new IOException("The file already exists: " + destFile.getPath());
		try (final InputStream input = getCompressorNewInputStream(
				new BufferedInputStream(new FileInputStream(source)))) {
			IOUtils.copy(input, destFile);
		} catch (IOException e) {
			throw new IOException("Unable to decompress the file: " + source.getPath(), e);
		}
	}

	/**
	 * Decompress the file as a String
	 *
	 * @param sourceFile the file to uncompress
	 * @return a string with the uncompressed content
	 * @throws IOException         related to I/O errors
	 * @throws CompressorException if any compression error occurs
	 */
	public String decompressString(final File sourceFile) throws IOException, CompressorException {
		try (final InputStream input = getCompressorNewInputStream(
				new BufferedInputStream(new FileInputStream(sourceFile)))) {
			return IOUtils.toString(input, StandardCharsets.UTF_8);
		}
	}

	/**
	 * Decompress a JSON structure
	 *
	 * @param sourceFile
	 * @return the decompressed object
	 * @throws IOException         related to I/O errors
	 * @throws CompressorException if any compression error occurs
	 */
	public <T> T decompressJsonClass(final File sourceFile, final Class<T> valueType)
			throws IOException, CompressorException {
		try (final InputStream input = getCompressorNewInputStream(
				new BufferedInputStream(new FileInputStream(sourceFile)))) {
			return ObjectMappers.JSON.readValue(input, valueType);
		}
	}

	/**
	 * Decompress a JSON structure
	 *
	 * @param sourceFile
	 * @param typeReference
	 * @param <T>
	 * @return
	 * @throws IOException
	 * @throws CompressorException
	 */
	public <T> T decompressJsonType(final File sourceFile, TypeReference<T> typeReference)
			throws IOException, CompressorException {
		try (final InputStream input = getCompressorNewInputStream(
				new BufferedInputStream(new FileInputStream(sourceFile)))) {
			return ObjectMappers.JSON.readValue(input, typeReference);
		}
	}

	/**
	 * Decompress a JSON structure
	 *
	 * @param sourceFile
	 * @return
	 * @throws IOException
	 * @throws CompressorException
	 */
	public JsonNode decompressJson(final File sourceFile) throws IOException, CompressorException {
		try (final InputStream input = getCompressorNewInputStream(
				new BufferedInputStream(new FileInputStream(sourceFile)))) {
			return ObjectMappers.JSON.readTree(input);
		}
	}

	public void decompress_dir(final File sourceDir, String sourceExtension, final File destDir,
			final String destExtension) throws IOException, CompressorException {
		if (!sourceDir.exists())
			throw new FileNotFoundException("The source directory does not exist: " + sourceDir.getPath());
		if (!destDir.exists())
			throw new FileNotFoundException("The destination directory does not exist: " + destDir.getPath());
		final File[] sourceFiles = sourceDir.listFiles();
		if (sourceFiles == null)
			return;
		for (File sourceFile : sourceFiles) {
			if (!sourceFile.isFile())
				continue;
			final String ext = FilenameUtils.getExtension(sourceFile.getName());
			if (!sourceExtension.equals(ext))
				continue;
			String newName = FilenameUtils.getBaseName(sourceFile.getName());
			if (destExtension != null)
				newName += '.' + destExtension;
			final File destFile = new File(destDir, newName);
			if (destFile.exists())
				continue;
			decompress(sourceFile, destFile);
		}
	}

	public void decompress_dir(final String sourcePath, final String sourceExtension, final String destPath,
			final String destExtension) throws IOException, CompressorException {
		decompress_dir(new File(sourcePath), sourceExtension, new File(destPath), destExtension);
	}

	public void decompress_dir(final String sourcePath, final String sourceExtension, final String destPath)
			throws IOException, CompressorException {
		decompress_dir(sourcePath, sourceExtension, destPath, null);
	}

	public void extract(final File sourceFile, final File destDir) throws IOException, ArchiveException {
		try (final InputStream is = new BufferedInputStream(new FileInputStream(sourceFile))) {
			try (final ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream(is)) {
				ArchiveEntry entry;
				while ((entry = in.getNextEntry()) != null) {
					if (!in.canReadEntryData(entry))
						continue;
					if (entry.isDirectory()) {
						File newDir = new File(destDir, entry.getName());
						if (newDir.exists() && newDir.isDirectory())
							continue;
						if (!newDir.mkdir())
							throw new IOException("Can't create directory : " + newDir.getPath());
						continue;
					}
					if (entry instanceof ZipArchiveEntry)
						if (((ZipArchiveEntry) entry).isUnixSymlink())
							continue;
					final File destFile = new File(destDir, entry.getName());
					final File parentDir = destFile.getParentFile();
					if (!parentDir.exists())
						if (!parentDir.mkdirs())
							throw new IOException("Can't create directory : " + parentDir.getPath());
					final long entryLastModified = entry.getLastModifiedDate().getTime();
					if (destFile.exists() && destFile.isFile() && destFile.lastModified() == entryLastModified &&
							entry.getSize() == destFile.length())
						continue;
					IOUtils.copy(in, destFile);
					destFile.setLastModified(entryLastModified);
				}
			} catch (IOException e) {
				throw new IOException("Unable to extract the archive: " + sourceFile.getPath(), e);
			}
		} catch (ArchiveException e) {
			throw new ArchiveException("Unable to extract the archive: " + sourceFile.getPath(), e);
		}
	}

	public void extract_dir(final File sourceDir, final String sourceExtension, final File destDir,
			final Boolean logErrorAndContinue) throws IOException, ArchiveException {
		if (!sourceDir.exists())
			throw new FileNotFoundException("The source directory does not exist: " + sourceDir.getPath());
		if (!destDir.exists())
			throw new FileNotFoundException("The destination directory does not exist: " + destDir.getPath());
		final File[] sourceFiles = sourceDir.listFiles();
		if (sourceFiles == null)
			return;
		for (File sourceFile : sourceFiles) {
			if (!sourceFile.isFile())
				continue;
			final String ext = FilenameUtils.getExtension(sourceFile.getName());
			if (!sourceExtension.equals(ext))
				continue;
			try {
				extract(sourceFile, destDir);
			} catch (IOException | ArchiveException e) {
				if (logErrorAndContinue != null && logErrorAndContinue)
					LOGGER.log(Level.SEVERE, e, e::getMessage);
				else
					throw e;
			}
		}
	}

	public void extract_dir(String sourcePath, String sourceExtension, String destPath, Boolean logErrorAndContinue)
			throws IOException, ArchiveException {
		extract_dir(new File(sourcePath), sourceExtension, new File(destPath), logErrorAndContinue);
	}

	private CompressorOutputStream getCompressor(OutputStream input) throws CompressorException {
		return factory.createCompressorOutputStream(codec.codecName, input);
	}

	/**
	 * Compress a stream an write the compressed content in a file
	 *
	 * @param input    the stream to compress
	 * @param destFile the compressed file
	 * @throws CompressorException if any compression error occurs
	 * @throws IOException         if any I/O error occurs
	 */
	public void compress(InputStream input, File destFile) throws IOException, CompressorException {
		OutputStream output = getCompressor(new BufferedOutputStream(new FileOutputStream(destFile)));
		try {
			IOUtils.copy(input, output);
		} finally {
			IOUtils.closeQuietly(output);
		}
	}

	/**
	 * Compress an array of byte and write it to a file
	 *
	 * @param bytes    the bytes to compress
	 * @param destFile the compressed file
	 * @throws CompressorException if any compression error occurs
	 * @throws IOException         related to I/O errors
	 */
	public void compress(byte[] bytes, File destFile) throws CompressorException, IOException {
		InputStream input = new ByteArrayInputStream(bytes);
		try {
			compress(input, destFile);
		} finally {
			IOUtils.closeQuietly(input);
		}
	}

	/**
	 * Compress an UTF-8 string and write it to a file
	 *
	 * @param content  the text to compress
	 * @param destFile the compressed file
	 * @throws CompressorException if any compression error occurs
	 * @throws IOException         related to I/O errors
	 */
	public void compress(String content, File destFile) throws CompressorException, IOException {
		compress(CharsetUtils.encodeUtf8(content), destFile);
	}

	/**
	 * Compress the content of a file to a new file
	 *
	 * @param sourceFile the file to compress
	 * @param destFile   the compressed file
	 * @throws CompressorException if any compression error occurs
	 * @throws IOException         related to I/O errors
	 */
	public void compress(File sourceFile, File destFile) throws CompressorException, IOException {
		InputStream input = new BufferedInputStream(new FileInputStream(sourceFile));
		try {
			compress(input, destFile);
		} finally {
			IOUtils.closeQuietly(input);
		}
	}

	public void createZipArchive(Map<String, Object> sourcePaths, File zipFile) throws IOException {
		FileOutputStream fos = new FileOutputStream(zipFile);
		try {
			ZipOutputStream zos = new ZipOutputStream(fos);
			try {
				for (Map.Entry<String, Object> entry : sourcePaths.entrySet())
					addToZipFile(entry.getKey(), entry.getValue().toString(), zos);
			} finally {
				IOUtils.closeQuietly(zos);
			}
		} finally {
			IOUtils.closeQuietly(fos);
		}
	}

	public void createZipArchive(Map<String, Object> sourcePaths, String zipFilePath) throws IOException {
		createZipArchive(sourcePaths, new File(zipFilePath));
	}

	public void addToZipFile(String entryName, String filePath, ZipOutputStream zos) throws IOException {

		File srcFile = new File(filePath);
		if (!srcFile.exists())
			throw new FileNotFoundException("The file does not exists: " + srcFile.getPath());
		FileInputStream fis = new FileInputStream(srcFile);
		try {
			ZipEntry zipEntry = new ZipEntry(entryName);
			zos.putNextEntry(zipEntry);
			IOUtils.copy(fis, zos);
			zos.closeEntry();
		} finally {
			IOUtils.closeQuietly(fis);
		}
	}

	public final static ContentType APPLICATION_ZIP = ContentType.create("application/zip");

	@JsonIgnore
	public static ContentType getApplicationZipContentType() {
		return APPLICATION_ZIP;
	}
}
