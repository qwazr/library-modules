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

import com.qwazr.library.annotations.Library;
import com.qwazr.library.test.AbstractLibraryTest;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.junit.Assert;
import org.junit.Test;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;

public class ProcessToolTest extends AbstractLibraryTest {

	@Library("process")
	private ProcessTool process;

	@Test
	public void test() throws ScriptException, IOException, InterruptedException {
		Assert.assertNotNull(process);
		Object object = new ScriptEngineManager().getEngineByName("nashorn").eval("JSON");
		Process p = process.execute((ScriptObjectMirror) object);
		Assert.assertNotNull(p);
		Assert.assertEquals(0, p.waitFor());
	}

}
