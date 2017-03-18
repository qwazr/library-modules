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
package com.qwazr.library.audio;

import com.qwazr.extractor.ExtractorManager;
import com.qwazr.extractor.ParserTest;
import org.junit.Test;

import java.io.IOException;

public class AudioParserTest extends ParserTest {

	static final String DEFAULT_TEST_STRING = "osstextextractor";

	static final String AUDIO_TEST_STRING = "opensearchserver";

	public AudioParserTest() throws IOException, ClassNotFoundException {
		super(new ExtractorManager(null));
		manager.registerByJsonResources();
	}

	@Test
	public void testAudioFlag() throws Exception {
		doTest(AudioParser.class, "file.flac", AUDIO_TEST_STRING, "format", "flac");
	}

	@Test
	public void testAudioM4a() throws Exception {
		doTest(AudioParser.class, "file.m4a", DEFAULT_TEST_STRING, "format", "m4a");
	}

	@Test
	public void testAudioMp3() throws Exception {
		doTest(AudioParser.class, "file.mp3", DEFAULT_TEST_STRING, "format", "mp3");
	}

	@Test
	public void testAudioOgg() throws Exception {
		doTest(AudioParser.class, "file.ogg", AUDIO_TEST_STRING, "format", "ogg");
	}

	@Test
	public void testAudioWav() throws Exception {
		doTest(AudioParser.class, "file.wav", null, "format", "wav");
	}

	@Test
	public void testAudioWma() throws Exception {
		doTest(AudioParser.class, "file.wma", AUDIO_TEST_STRING, "format", "wma");
	}

}
