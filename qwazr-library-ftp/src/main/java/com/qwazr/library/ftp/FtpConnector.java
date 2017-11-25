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
package com.qwazr.library.ftp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.qwazr.library.AbstractPasswordLibrary;
import com.qwazr.utils.IOUtils;
import com.qwazr.utils.LoggerUtils;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FtpConnector extends AbstractPasswordLibrary {

	public final String hostname;
	public final String username;
	public final Boolean ssl;
	public final Boolean passive_mode;
	public final Integer connect_time_out;
	public final Integer data_timeout;
	public final Integer keep_alive_timeout;
	public final Integer control_keep_alive_timeout;

	private static final Logger LOGGER = LoggerUtils.getLogger(FtpConnector.class);

	public FtpConnector() {
		hostname = null;
		username = null;
		ssl = null;
		passive_mode = null;
		connect_time_out = null;
		data_timeout = null;
		keep_alive_timeout = null;
		control_keep_alive_timeout = null;
	}

	@JsonIgnore
	public FTPSession getNewSession(final IOUtils.CloseableContext context) {
		FTPSession ftpSession = new FTPSession();
		if (context != null)
			context.add(ftpSession);
		return ftpSession;
	}

	public class FTPSession implements Closeable {

		private final FTPClient ftp;

		private FTPSession() {
			ftp = ssl != null && ssl ? new FTPSClient() : new FTPClient();
		}

		public FTPClient connect() throws IOException {
			if (ftp.isConnected())
				return ftp;
			if (keep_alive_timeout != null)
				ftp.setControlKeepAliveTimeout(keep_alive_timeout);
			if (control_keep_alive_timeout != null)
				ftp.setControlKeepAliveReplyTimeout(control_keep_alive_timeout);
			if (data_timeout != null)
				ftp.setDataTimeout(data_timeout);
			if (connect_time_out != null)
				ftp.setConnectTimeout(connect_time_out);
			ftp.connect(hostname);
			int reply = ftp.getReplyCode();
			if (!FTPReply.isPositiveCompletion(reply))
				throw new IOException("FTP server returned an error: " + reply);
			if (!ftp.login(username, password))
				throw new IOException("FTP login failed: " + ftp.getReplyCode());
			return ftp;
		}

		private void checkPassiveMode() {
			if (passive_mode != null && passive_mode)
				ftp.enterLocalPassiveMode();
			else
				ftp.enterLocalActiveMode();
		}

		/**
		 * Download the file if any
		 *
		 * @param remote   the name of the file
		 * @param filePath the path of the destination file
		 * @throws IOException
		 */
		public void retrieve(final String remote, final Path filePath, final Boolean binary) throws IOException {
			if (binary != null) {
				if (binary) {
					if (!ftp.setFileType(FTP.BINARY_FILE_TYPE))
						throw new IOException("FTP cannot be set to binary mode");
				} else {
					if (!ftp.setFileType(FTP.ASCII_FILE_TYPE))
						throw new IOException("FTP cannot be set to ASCII mode");
				}
			}
			checkPassiveMode();

			try (final InputStream is = ftp.retrieveFileStream(remote)) {
				if (is == null)
					throw new FileNotFoundException("FTP file not found: " + hostname + "/" + remote);
				IOUtils.copy(is, filePath);
			}
			ftp.completePendingCommand();
		}

		public void retrieve(final FTPFile remote, final Path filePath, final Boolean binary) throws IOException {
			retrieve(remote.getName(), filePath, binary);
		}

		public void retrieve(final FTPFile remote, final String local_path, final Boolean binary) throws IOException {
			retrieve(remote.getName(), Paths.get(local_path), binary);
		}

		public void retrieve(final String remote, final String local_path, final Boolean binary) throws IOException {
			retrieve(remote, Paths.get(local_path), binary);
		}

		public void sync_files(final ScriptObjectMirror browser, final String remote_path, final Path localDirectory,
				final Boolean downloadOnlyIfNotExists, final Boolean binary) throws IOException {

			final boolean file_method = browser != null && browser.hasMember("file");
			final boolean dir_method = browser != null && browser.hasMember("directory");
			if (!ftp.changeWorkingDirectory(remote_path))
				throw new IOException("Remote working directory change failed: " + hostname + "/" + remote_path);
			if (!Files.exists(localDirectory))
				throw new FileNotFoundException("The destination directory does not exist: " + localDirectory);
			if (!Files.isDirectory(localDirectory))
				throw new IOException("The destination path is not a directory: " + localDirectory);
			checkPassiveMode();
			FTPFile[] remoteFiles = ftp.listFiles();
			if (remoteFiles == null)
				return;
			final LinkedHashMap<FTPFile, Path> remoteDirs = new LinkedHashMap<>();
			for (FTPFile remoteFile : remoteFiles) {
				if (remoteFile == null)
					continue;
				final String remoteName = remoteFile.getName();
				if (".".equals(remoteName))
					continue;
				if ("..".endsWith(remoteName))
					continue;
				if (remoteFile.isDirectory()) {
					if (dir_method)
						if (Boolean.FALSE.equals(browser.callMember("directory", remote_path + '/' + remoteName)))
							continue;
					final Path localDir = localDirectory.resolve(remoteName);
					if (!Files.exists(localDir))
						Files.createDirectory(localDir);
					remoteDirs.put(remoteFile, localDir);
					continue;
				}
				if (!remoteFile.isFile())
					continue;
				final Path localFilePath = localDirectory.resolve(remoteName);
				if (file_method)
					if (Boolean.FALSE.equals(
							browser.callMember("file", remote_path + '/' + remoteName, Files.exists(localFilePath))))
						continue;
				if (downloadOnlyIfNotExists != null && downloadOnlyIfNotExists && Files.exists(localFilePath))
					continue;
				LOGGER.info(() -> "FTP download: " + hostname + '/' + remote_path + '/' + remoteName);
				retrieve(remoteFile, localFilePath, binary);
			}
			for (Map.Entry<FTPFile, Path> entry : remoteDirs.entrySet())
				sync_files(browser, remote_path + '/' + entry.getKey().getName(), entry.getValue(),
						downloadOnlyIfNotExists, binary);
		}

		public void sync_files(final ScriptObjectMirror browser, final String remote_path, final String local_path,
				final Boolean downloadOnlyIfNotExists, final Boolean binary) throws IOException {
			sync_files(browser, remote_path, Paths.get(local_path), downloadOnlyIfNotExists, binary);
		}

		public void logout() throws IOException {
			ftp.logout();
		}

		@Override
		public void close() throws IOException {
			if (!ftp.isConnected())
				return;
			try {
				ftp.disconnect();
			} catch (IOException e) {
				LOGGER.log(Level.WARNING, e, e::getMessage);
			}
		}
	}

}
