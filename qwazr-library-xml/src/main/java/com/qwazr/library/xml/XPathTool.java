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
package com.qwazr.library.xml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class XPathTool extends AbstractXmlFactoryTool implements Closeable {

	private final static XPathFactory xPathFactory = XPathFactory.newInstance();

	@JsonIgnore
	private final XPath xPath;

	@JsonIgnore
	private final ConcurrentHashMap<String, XPathExpression> xPathMap;

	public XPathTool() {
		synchronized (xPathFactory) {
			xPath = xPathFactory.newXPath();
		}
		xPathMap = new ConcurrentHashMap<>();
	}

	@Override
	public void close() {
		xPathMap.clear();
	}

	public XPathDocument readDocument(File file) throws ParserConfigurationException, SAXException, IOException {
		return new XPathDocument(this, file);
	}

	public XPathDocument readDocument(String path) throws ParserConfigurationException, SAXException, IOException {
		return new XPathDocument(this, new File(path));
	}

	public Collection<String> extractText(Node node) {
		if (node == null)
			return null;
		ArrayList<String> list = new ArrayList<>();
		extractText(node, list);
		return list;
	}

	public void extractText(Node node, Collection<String> collector) {
		if (node == null)
			return;
		if (node.getNodeType() == Node.TEXT_NODE) {
			collector.add(node.getTextContent());
			return;
		}
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node childNode = list.item(i);
			extractText(childNode, collector);
		}
	}

	@JsonIgnore
	XPathExpression getXPathExpression(String xpathExpression) {
		return xPathMap.computeIfAbsent(xpathExpression, exp -> {
			synchronized (xPath) {
				try {
					return xPath.compile(exp);
				} catch (XPathExpressionException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
			}
		});
	}

	public void clearXpathCache() {
		xPathMap.clear();
	}

}
