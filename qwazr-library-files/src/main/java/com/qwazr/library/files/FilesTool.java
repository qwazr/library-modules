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

import com.qwazr.library.AbstractLibrary;
import com.qwazr.component.annotations.Component;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.io.IOException;
import java.nio.file.Paths;

@Component("File system browser")
public class FilesTool extends AbstractLibrary {

	@Component("Browse a directory")
	public void browse(@Component("The path of the directory to browse") final String path,
			@Component("The maximum depth of browsing") final int max_depth,
			@Component("An object with callback methods: file(path, depth) or directory(path, depth)")
			final ScriptObjectMirror browser) throws IOException {
		new FileBrowser(browser, max_depth, Paths.get(path));
	}
}
