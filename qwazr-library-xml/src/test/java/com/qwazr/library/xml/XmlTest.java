/**
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
 **/
package com.qwazr.library.xml;

import com.qwazr.library.annotations.Library;
import com.qwazr.library.test.AbstractLibraryTest;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class XmlTest extends AbstractLibraryTest {

	@Library("xml")
	private XMLTool xml;

	@Library("xpath")
	private XPathTool xpath;

	private final static String TEST_XML_PATH = "src/test/resources/com/qwazr/library/xml/test.xml";

	@Test
	public void xml() throws IOException, ParserConfigurationException, SAXException {
		Assert.assertNotNull(xml);
		final Document document = xml.domParseFile(TEST_XML_PATH);
		Assert.assertNotNull(document);

		try (final StringWriter sw = new StringWriter()) {
			final String res;
			try (final PrintWriter pw = new PrintWriter(sw)) {
				xml.toXML(document, pw);
			}
			res = sw.toString();
			Assert.assertNotNull(res);
			Assert.assertNotEquals(0, res.length());
		}
	}

	@Test
	public void xpath() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
		Assert.assertNotNull(xpath);
		XPathDocument xPathDoc = xpath.readDocument(TEST_XML_PATH);
		Assert.assertNotNull(xPathDoc);

		Node[] nodes = xPathDoc.xpath_nodes("//test");
		Assert.assertNotNull(xPathDoc);
		Assert.assertEquals(2, nodes.length);
	}
}
