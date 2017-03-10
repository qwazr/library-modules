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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;

public class XPathDocument {

	private final XPathTool xPathTool;

	private final Document document;

	XPathDocument(XPathTool xPathTool, File file) throws ParserConfigurationException, SAXException, IOException {
		this.xPathTool = xPathTool;
		document = xPathTool.getNewDocumentBuilder().parse(file);
	}

	private Object xpath(String xpath_expression, Object object, QName xPathResult) throws XPathExpressionException {
		if (object == null)
			object = document;
		final XPathExpression xPathExpression = xPathTool.getXPathExpression(xpath_expression);
		synchronized (xPathExpression) {
			return xPathExpression.evaluate(object, xPathResult);
		}
	}

	public Node xpath_node(String xpath_expression, Object object) throws XPathExpressionException {
		return (Node) xpath(xpath_expression, object, XPathConstants.NODE);
	}

	public Node xpath_node(String xpath_expression) throws XPathExpressionException {
		return xpath_node(xpath_expression, document);
	}

	public Node[] xpath_nodes(String xpath_expression, Object object) throws XPathExpressionException {
		if (object == null)
			object = document;
		NodeList nodeList = (NodeList) xpath(xpath_expression, object, XPathConstants.NODESET);
		if (nodeList == null)
			return null;
		Node[] nodes = new Node[nodeList.getLength()];
		for (int i = 0; i < nodes.length; i++)
			nodes[i] = nodeList.item(i);
		return nodes;
	}

	public Node[] xpath_nodes(String xpath_expression) throws XPathExpressionException {
		return xpath_nodes(xpath_expression, document);
	}

	public String xpath_text(String xpath_expression, Object object) throws XPathExpressionException {
		return (String) xpath(xpath_expression, object, XPathConstants.STRING);
	}

	public String xpath_text(String xpath_expression) throws XPathExpressionException {
		return xpath_text(xpath_expression, document);
	}

	public Boolean xpath_boolean(String xpath_expression, Object object) throws XPathExpressionException {
		return (Boolean) xpath(xpath_expression, object, XPathConstants.BOOLEAN);
	}

	public Boolean xpath_boolean(String xpath_expression) throws XPathExpressionException {
		return xpath_boolean(xpath_expression, document);
	}

	public Number xpath_number(String xpath_expression, Object object) throws XPathExpressionException {
		return (Number) xpath(xpath_expression, object, XPathConstants.NUMBER);
	}

	public Number xpath_number(String xpath_expression) throws XPathExpressionException {
		return xpath_number(xpath_expression, document);
	}

}
