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
package com.qwazr.library.files;

import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

class FileBrowser {

	private final boolean file_method;
	private final boolean dir_method;
	private final int max_depth;

	FileBrowser(final ScriptObjectMirror browser, final int max_depth, final Path rootPath) throws IOException {
		this.max_depth = max_depth;
		if (browser != null) {
			file_method = browser.hasMember("file");
			dir_method = browser.hasMember("directory");
		} else {
			file_method = false;
			dir_method = false;
		}
		if (Files.exists(rootPath))
			browse(browser, rootPath, 0);
	}

	private boolean browse(final ScriptObjectMirror browser, final Path path, final int depth) throws IOException {
		if (Files.isRegularFile(path))
			return browseFile(browser, path, depth);
		else if (Files.isDirectory(path) && depth < max_depth)
			return browseDir(browser, path, depth + 1);
		return true;
	}

	private boolean browseFile(final ScriptObjectMirror browser, final Path filePath, final int depth) {
		return file_method && !Boolean.FALSE.equals(browser.callMember("file", filePath, depth));
	}

	private boolean browseDir(final ScriptObjectMirror browser, final Path dirPath, final int depth)
			throws IOException {
		if (dir_method)
			if (Boolean.FALSE.equals(browser.callMember("directory", dirPath, depth)))
				return false;
		final Iterator<Path> it = Files.list(dirPath).iterator();
		while (it.hasNext())
			if (!browse(browser, it.next(), depth))
				return false;
		return true;
	}

}
