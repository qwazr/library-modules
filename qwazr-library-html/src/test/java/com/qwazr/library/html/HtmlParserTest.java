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
package com.qwazr.library.html;

import com.qwazr.extractor.ExtractorManager;
import com.qwazr.extractor.ParserResult;
import com.qwazr.extractor.ParserTest;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HtmlParserTest extends ParserTest {

	public HtmlParserTest() throws IOException, ClassNotFoundException {
		super(new ExtractorManager());
		manager.registerServices();
	}

	@Test
	public void testHtml() throws Exception {
		final ParserResult result = doTest(HtmlParser.class, "file.html", "text/html", "search engine software");
		Assert.assertEquals("OpenSearchServer | Open Source Search Engine and API",
				result.getDocumentFieldValue(0, "title", 0).toString().trim());
	}

	private void testSelector(String[] names, String[] selectors, String param, String[] selectorResults) {
		final MultivaluedMap map = new MultivaluedHashMap<>();
		map.addAll(param, selectors);
		if (names != null)
			map.addAll(param + "_name", names);

		ParserResult parserResult = service.extract("html", map, null, getStream("file.html"));
		Assert.assertNotNull(parserResult);
		Map<String, List<String>> results =
				(Map<String, List<String>>) parserResult.getDocumentFieldValue(0, "selectors", 0);
		Assert.assertNotNull(results);
		Assert.assertEquals(selectorResults.length, results.size());
		int i = 0;
		for (String selectorResult : selectorResults) {
			String key = names == null ? Integer.toString(i) : names[i];
			List<String> list = results.get(key);
			Assert.assertNotNull(list);
			Assert.assertTrue(list.size() > 0);
			if (selectorResult == null)
				continue;
			String result = results.get(key).get(0);
			Assert.assertNotNull(result);
			Assert.assertEquals(selectorResult, result);
			i++;
		}
	}

	private final static String[] XPATH_NAMES = { "xp1", "xp2" };
	private final static String[] XPATH_SELECTORS =
			{ "//*[@id=\"crawl\"]/ul/li[1]/strong", "//*[@id=\"download\"]/div/div[2]/div/h3" };
	private final static String[] XPATH_RESULTS = { "web crawler", "Documentation" };

	@Test
	public void testHtmlXPath() {
		testSelector(null, XPATH_SELECTORS, "xpath", XPATH_RESULTS);
		testSelector(XPATH_NAMES, XPATH_SELECTORS, "xpath", XPATH_RESULTS);
	}

	private final static String[] CSS_NAMES = { "css1", "css2" };
	private final static String[] CSS_SELECTORS =
			{ "#crawl > ul > li:nth-child(1) > strong", "#download > div > div:nth-child(2) > div > h3" };
	private final static String[] CSS_RESULTS = { "web crawler", "Documentation" };

	@Test
	public void testHtmlCSS() {
		testSelector(null, CSS_SELECTORS, "css", CSS_RESULTS);
		testSelector(CSS_NAMES, CSS_SELECTORS, "css", CSS_RESULTS);
	}

	private final static String[] REGEXP_NAMES = { "reg1", "reg2" };
	private final static String[] REGEXP_SELECTORS = { "\"downloadUrl\" : \"(.*?)\"", "<script>(.*?)</script>" };
	private final static String[] REGEXP_RESULTS = { "http://www.opensearchserver.com/#download", null };

	@Test
	public void testHtmlRegExp() {
		testSelector(null, REGEXP_SELECTORS, "regexp", REGEXP_RESULTS);
		testSelector(REGEXP_NAMES, REGEXP_SELECTORS, "regexp", REGEXP_RESULTS);
	}
}
