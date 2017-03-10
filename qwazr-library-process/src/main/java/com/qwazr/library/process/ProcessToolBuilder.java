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
package com.qwazr.library.process;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProcessToolBuilder {

	List<String> commands;
	Map<String, String> environmentVariables;
	File workingDirectory;

	public ProcessToolBuilder command(String... cmds) {
		if (commands == null)
			commands = new ArrayList<>();
		Collections.addAll(commands, cmds);
		return this;
	}

	public ProcessToolBuilder environmentVariable(String env, String value) {
		if (environmentVariables == null)
			environmentVariables = new LinkedHashMap<>();
		environmentVariables.put(env, value);
		return this;
	}

	public ProcessToolBuilder workingDirectory(File workingDirectory) {
		this.workingDirectory = workingDirectory;
		return this;
	}

	public ProcessTool build() {
		return new ProcessTool(this);
	}
}