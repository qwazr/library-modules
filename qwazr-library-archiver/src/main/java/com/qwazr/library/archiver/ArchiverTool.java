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
import com.qwazr.utils.concurrent.ConcurrentUtils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
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

	public void decompress(final Path source, final Path destFile) throws IOException, CompressorException {
		if (Files.exists(destFile) && Files.size(destFile) > 0)
			throw new IOException("The file already exists: " + destFile.toAbsolutePath());
		try (final InputStream input = getCompressorNewInputStream(
				new BufferedInputStream(Files.newInputStream(source)))) {
			IOUtils.copy(input, destFile);
		} catch (IOException e) {
			throw new IOException("Unable to decompress the file: " + source.toAbsolutePath(), e);
		}
	}

	/**
	 * Decompress the file as a String
	 *
	 * @param sourceFile the path to the file to uncompress
	 * @return a string with the uncompressed content
	 * @throws IOException         related to I/O errors
	 * @throws CompressorException if any compression error occurs
	 */
	public String decompressString(final Path sourceFile) throws IOException, CompressorException {
		try (final InputStream input = getCompressorNewInputStream(
				new BufferedInputStream(Files.newInputStream(sourceFile)))) {
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

	public void decompress_dir(final Path sourceDir, String sourceExtension, final Path destDir,
			final String destExtension) throws IOException, CompressorException {
		if (!Files.exists(sourceDir))
			throw new FileNotFoundException("The source directory does not exist: " + sourceDir.toAbsolutePath());
		if (!Files.exists(destDir))
			throw new FileNotFoundException("The destination directory does not exist: " + destDir.toAbsolutePath());
		final Path[] sourceFiles = Files.list(sourceDir).filter(p -> Files.isRegularFile(p)).toArray(Path[]::new);
		if (sourceFiles == null)
			return;
		for (Path sourceFile : sourceFiles) {
			final String fileName = sourceFile.getFileName().toString();
			final String ext = FilenameUtils.getExtension(fileName);
			if (!sourceExtension.equals(ext))
				continue;
			String newName = FilenameUtils.getBaseName(fileName);
			if (destExtension != null)
				newName += '.' + destExtension;
			final Path destFile = destDir.resolve(newName);
			if (Files.exists(destFile))
				continue;
			decompress(sourceFile, destFile);
		}
	}

	public void decompress_dir(final String sourcePath, final String sourceExtension, final String destPath,
			final String destExtension) throws IOException, CompressorException {
		decompress_dir(Paths.get(sourcePath), sourceExtension, Paths.get(destPath), destExtension);
	}

	public void decompress_dir(final String sourcePath, final String sourceExtension, final String destPath)
			throws IOException, CompressorException {
		decompress_dir(sourcePath, sourceExtension, destPath, null);
	}

	public void extract(final Path sourceFile, final Path destDir) throws IOException, ArchiveException {
		try (final InputStream is = new BufferedInputStream(Files.newInputStream(sourceFile))) {
			try (final ArchiveInputStream in = new ArchiveStreamFactory().createArchiveInputStream(is)) {
				ArchiveEntry entry;
				while ((entry = in.getNextEntry()) != null) {
					if (!in.canReadEntryData(entry))
						continue;
					if (entry.isDirectory()) {
						final Path newDir = destDir.resolve(entry.getName());
						if (!Files.exists(newDir))
							Files.createDirectory(newDir);
						continue;
					}
					if (entry instanceof ZipArchiveEntry)
						if (((ZipArchiveEntry) entry).isUnixSymlink())
							continue;
					final Path destFile = destDir.resolve(entry.getName());
					final Path parentDir = destFile.getParent();
					if (!Files.exists(parentDir))
						Files.createDirectories(parentDir);
					final long entryLastModified = entry.getLastModifiedDate().getTime();
					if (Files.exists(destFile) && Files.isRegularFile(destFile) &&
							Files.getLastModifiedTime(destFile).toMillis() == entryLastModified &&
							entry.getSize() == Files.size(destFile))
						continue;
					IOUtils.copy(in, destFile);
					Files.setLastModifiedTime(destFile, FileTime.fromMillis(entryLastModified));
				}
			} catch (IOException e) {
				throw new IOException("Unable to extract the archive: " + sourceFile.toAbsolutePath(), e);
			}
		} catch (ArchiveException e) {
			throw new ArchiveException("Unable to extract the archive: " + sourceFile.toAbsolutePath(), e);
		}
	}

	public void extract_dir(final Path sourceDir, final String sourceExtension, final Path destDir,
			final Boolean logErrorAndContinue) throws IOException, ArchiveException {
		if (!Files.exists(sourceDir))
			throw new FileNotFoundException("The source directory does not exist: " + sourceDir.toAbsolutePath());
		if (!Files.exists(destDir))
			throw new FileNotFoundException("The destination directory does not exist: " + destDir.toAbsolutePath());
		final Path[] sourceFiles = Files.list(sourceDir).filter(p -> Files.isRegularFile(p)).toArray(Path[]::new);
		if (sourceFiles == null)
			return;
		for (final Path sourceFile : sourceFiles) {
			final String ext = FilenameUtils.getExtension(sourceFile.getFileName().toString());
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
		extract_dir(Paths.get(sourcePath), sourceExtension, Paths.get(destPath), logErrorAndContinue);
	}

	private CompressorOutputStream getCompressor(OutputStream input) throws CompressorException {
		return factory.createCompressorOutputStream(codec.codecName, input);
	}

	/**
	 * Compress a stream an write the compressed content in a file
	 *
	 * @param input    the stream to compress
	 * @param destFile the path of the compressed file
	 * @throws CompressorException if any compression error occurs
	 * @throws IOException         if any I/O error occurs
	 */
	public void compress(final InputStream input, final Path destFile) throws IOException, CompressorException {
		try (final OutputStream output = getCompressor(new BufferedOutputStream(Files.newOutputStream(destFile)))) {
			IOUtils.copy(input, output);
		}
	}

	/**
	 * Compress an array of byte and write it to a file
	 *
	 * @param bytes    the bytes to compress
	 * @param destFile the path of the compressed file
	 * @throws CompressorException if any compression error occurs
	 * @throws IOException         related to I/O errors
	 */
	public void compress(final byte[] bytes, final Path destFile) throws CompressorException, IOException {
		try (final InputStream input = new ByteArrayInputStream(bytes)) {
			compress(input, destFile);
		}
	}

	/**
	 * Compress an UTF-8 string and write it to a file
	 *
	 * @param content  the text to compress
	 * @param destFile the path of the compressed file
	 * @throws CompressorException if any compression error occurs
	 * @throws IOException         related to I/O errors
	 */
	public void compress(final String content, final Path destFile) throws CompressorException, IOException {
		compress(CharsetUtils.encodeUtf8(content), destFile);
	}

	/**
	 * Compress the content of a file to a new file
	 *
	 * @param sourceFile the path of the file to compress
	 * @param destFile   the path of the compressed file
	 * @throws CompressorException if any compression error occurs
	 * @throws IOException         related to I/O errors
	 */
	public void compress(final Path sourceFile, final Path destFile) throws CompressorException, IOException {
		try (final InputStream input = new BufferedInputStream(Files.newInputStream(sourceFile))) {
			compress(input, destFile);
		}
	}

	public void createZipArchive(final Map<String, Object> sourcePaths, final Path zipFile) throws IOException {
		try (final OutputStream out = Files.newOutputStream(zipFile);
				final BufferedOutputStream bOut = new BufferedOutputStream(out);
				final ZipOutputStream zOut = new ZipOutputStream(bOut)) {
			ConcurrentUtils.forEachEx(sourcePaths, (key, value) -> addToZipFile(key, value.toString(), zOut));
		}
	}

	public void createZipArchive(final Map<String, Object> sourcePaths, final String zipFilePath) throws IOException {
		createZipArchive(sourcePaths, Paths.get(zipFilePath));
	}

	public void addToZipFile(final String entryName, final String filePath, final ZipOutputStream zos)
			throws IOException {
		final Path srcFile = Paths.get(filePath);
		if (!Files.exists(srcFile))
			throw new FileNotFoundException("The file does not exists: " + srcFile.toAbsolutePath());
		try (final InputStream in = Files.newInputStream(srcFile);
				final BufferedInputStream bIn = new BufferedInputStream(in)) {
			ZipEntry zipEntry = new ZipEntry(entryName);
			zos.putNextEntry(zipEntry);
			IOUtils.copy(bIn, zos);
			zos.closeEntry();
		}
	}

}
